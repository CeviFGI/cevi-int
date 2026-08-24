package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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

    @Test
    void public_pages_do_not_reference_the_editor() {
        for (String path : new String[] { "/", "/anlaesse", "/volontariat", "/fgi", "/kontakt",
                "/datenschutzinformation", "/auth/login" }) {
            given().redirects().follow(true).when().get(path).then()
                    .statusCode(HttpStatus.SC_OK)
                    .body(not(containsString("jodit")))
                    .body(not(containsString(EDITOR_INIT)));
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
