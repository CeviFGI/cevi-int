package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * The headers are the second line of defence behind the HTML sanitiser: even if a payload were to
 * survive the reduction, a policy without {@code unsafe-inline} in {@code script-src} keeps it from
 * running (NFR-024).
 */
@QuarkusTest
class SecurityHeadersTest {

    @Test
    void public_page_carries_the_security_headers() {
        given().when().get("/anlaesse").then()
                .statusCode(HttpStatus.SC_OK)
                .header("X-Frame-Options", equalTo("DENY"))
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .header("Referrer-Policy", equalTo("strict-origin-when-cross-origin"))
                .header("Strict-Transport-Security", containsString("max-age=31536000"))
                .header("Content-Security-Policy", containsString("frame-ancestors 'none'"));
    }

    @Test
    void content_security_policy_does_not_allow_inline_scripts() {
        given().when().get("/anlaesse").then()
                .statusCode(HttpStatus.SC_OK)
                .header("Content-Security-Policy", containsString("script-src 'self'"))
                .header("Content-Security-Policy", not(containsString("script-src 'self' 'unsafe-inline'")));
    }

    /**
     * The policy only holds if the pages actually stop carrying inline scripts — the editor
     * initialisation lives in its own file for exactly that reason.
     */
    @Test
    void form_pages_carry_no_inline_script() {
        given().when().get("/kontakt").then()
                .statusCode(HttpStatus.SC_OK)
                .body(not(containsString("<script>")));
    }
}
