package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rich-text editor is by far the largest asset the application serves, and only the two
 * administrator form pages have a field to edit. NFR-036 therefore requires that no page reachable
 * without an administrator session references it — a regression here is invisible in the browser
 * and only shows up as a slower public site.
 */
@QuarkusTest
class EditorAssetsTest {

    private static final String EDITOR_SCRIPT = "/webjars/jodit/es2021/jodit.min.js";
    private static final String EDITOR_STYLESHEET = "/webjars/jodit/es2021/jodit.min.css";
    private static final String EDITOR_INIT = "/js/editor-init.js";

    private static final String[] PUBLIC_PAGES = { "/", "/anlaesse", "/volontariat", "/fgi",
            "/kontakt", "/datenschutzinformation", "/auth/login" };

    /** A script element, with its {@code src} when it has one. */
    private static final Pattern SCRIPT_TAG = Pattern.compile("(?i)<script\\b(?:[^>]*\\ssrc=\"([^\"]*)\")?[^>]*>");

    /** NFR-041: at most 8 KB of self-hosted script per public page, uncompressed. */
    private static final int SCRIPT_BUDGET = 8 * 1024;

    @Test
    void public_pages_do_not_reference_the_editor() {
        for (String path : PUBLIC_PAGES) {
            given().redirects().follow(true).when().get(path).then()
                    .statusCode(HttpStatus.SC_OK)
                    .body(not(containsString("jodit")))
                    .body(not(containsString(EDITOR_INIT)));
        }
    }

    /**
     * The rule C-021 and NFR-041 state since revision 7: script may enhance a public page, never
     * carry it — so a public page carries no inline script (it would need an exception in
     * {@code script-src 'self'}, NFR-024), only files from its own {@code /js/} folder, never a
     * library from a WebJar, and at most 8 KB of them in total. The event page, which also loads
     * the share script, is held to the same ceiling by {@code EventShareTest}; that every page
     * works without any of it is what the server-rendered tests of each use case show.
     */
    @Test
    void a_public_page_carries_only_small_self_hosted_scripts() {
        List<String> pages = new ArrayList<>(List.of(PUBLIC_PAGES));
        pages.add("/suche?q=anlass");
        for (String path : pages) {
            String body = given().redirects().follow(true).when().get(path).then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract().body().asString();

            int total = 0;
            Matcher script = SCRIPT_TAG.matcher(body);
            while (script.find()) {
                String src = script.group(1);
                assertTrue(src != null && src.startsWith("/js/"),
                        path + " delivers a script that is inline or not self-hosted (C-021): " + script.group());
                total += given().when().get(src).then().statusCode(HttpStatus.SC_OK)
                        .extract().body().asByteArray().length;
            }
            assertTrue(total <= SCRIPT_BUDGET,
                    path + " executes " + total + " bytes of script, more than NFR-041 allows (" + SCRIPT_BUDGET + ")");
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin" })
    void event_form_loads_the_editor() {
        given().when().get("/anlaesse/add").then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(EDITOR_STYLESHEET))
                .body(containsString(EDITOR_SCRIPT))
                .body(containsString(EDITOR_INIT));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin" })
    void voluntary_form_loads_the_editor() {
        given().when().get("/volontariat/add").then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(EDITOR_STYLESHEET))
                .body(containsString(EDITOR_SCRIPT))
                .body(containsString(EDITOR_INIT));
    }

    /**
     * The editor is served from the WebJar on the classpath, under a URL that carries no version so
     * that a dependency bump needs no template change.
     */
    @Test
    void editor_script_is_served_from_the_webjar() {
        given().when().get(EDITOR_SCRIPT).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Jodit"));
    }
}
