package tools.cevi.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;

/**
 * UC-006 BR-007: the session cookie is confined to encrypted transport, to the page's own requests
 * and to this site.
 * <p>
 * Quarkus has no setting that forces the Secure attribute of the form-login cookie; it sets it when
 * the request arrived over TLS. In production TLS ends at the reverse proxy, so the attribute rests
 * on {@code quarkus.http.proxy.proxy-address-forwarding} in {@code application-prod.properties}.
 * This test switches on the same forwarding and signs in the way the proxy hands a request on.
 */
@QuarkusTest
@TestProfile(SessionCookieAttributesTest.BehindTlsProxy.class)
class SessionCookieAttributesTest {

    /** The proxy settings of the {@code prod} profile, on top of the test profile. */
    public static class BehindTlsProxy implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.proxy.proxy-address-forwarding", "true",
                    "quarkus.http.proxy.enable-forwarded-host", "true",
                    "quarkus.http.proxy.enable-forwarded-prefix", "true");
        }
    }

    /** UC-006 BR-007: Secure, HttpOnly and SameSite=Strict on the cookie handed out at sign-in. */
    @Test
    void session_cookie_is_secure_http_only_and_same_site_strict() {
        given().contentType(ContentType.URLENC)
                .header("X-Forwarded-Proto", "https")
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .redirects()
                .follow(false)
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .cookie("quarkus-credential", detailedCookie()
                        .secured(true)
                        .httpOnly(true)
                        .sameSite("Strict"));
    }
}
