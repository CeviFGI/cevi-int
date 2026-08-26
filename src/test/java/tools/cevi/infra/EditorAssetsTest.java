package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
            "/kontakt", "/datenschutzinformation", "/auth/login", "/version" };

    private static final Pattern SCRIPT = Pattern.compile("(?i)<script\\b");

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
     * The stronger form of the same rule, and the one C-021 actually states: a public page carries
     * no script at all, not merely no editor. The navigation drawer and the disclosures are a
     * checkbox and a {@code <details>} for exactly this reason, so the constraint is a property of
     * the markup rather than an intention — and it is what keeps {@code script-src 'self'} free of
     * the exceptions that make a Content-Security-Policy a formality (NFR-024).
     */
    @Test
    void a_public_page_carries_no_script_at_all() {
        for (String path : PUBLIC_PAGES) {
            String body = given().redirects().follow(true).when().get(path).then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract().body().asString();

            assertFalse(SCRIPT.matcher(body).find(),
                    path + " delivers script to a visitor who never asked to edit anything (C-021)");
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
