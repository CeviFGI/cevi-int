package tools.cevi.auth;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-006 Sign In as Administrator over HTTP: the main success scenario (steps 1 to 6), a sign-in
 * that is not recognised (A1), an established session (A2), signing out with and without a session
 * (A3, A4) only from the system's own form (BR-009), and the general administration entry (BR-010).
 */
@QuarkusTest
public class AuthResourceTest {
    @TestHTTPEndpoint(AuthResource.class)
    @TestHTTPResource("login")
    URL loginEndpoint;

    @TestHTTPEndpoint(AuthResource.class)
    @TestHTTPResource("logout")
    URL logoutEndpoint;

    @TestHTTPEndpoint(AuthResource.class)
    @TestHTTPResource("loggedOut")
    URL loggedOutEndpoint;

    @TestHTTPEndpoint(AuthResource.class)
    @TestHTTPResource
    URL authEndpoint;

    /**
     * Main success scenario, steps 1 and 2: the sign-in page asks for user name and password.
     */
    @Test
    public void login_form() {
        given()
                .when()
                .get(loginEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("<label for=\"j_username\">"))
                .body(containsString("<label for=\"j_password\">"))
                .body(containsString("autocomplete=\"current-password\""));
    }

    /**
     * A2: with an established session the sign-in page leads to the start page.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void redirect_to_home_if_logged_in() {
        given()
                .cookie("quarkus-credential")
                .redirects()
                .follow(false)
                .when()
                .get(loginEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/"));
    }

    /**
     * Main success scenario, steps 3 to 6: valid credentials establish the session and lead to the
     * start page.
     */
    @Test
    public void login_success() {
        given().contentType(ContentType.URLENC)
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", is("http://localhost:8081/"))
                .cookie("quarkus-credential");
    }

    /**
     * A1: credentials that are not recognised lead to the error page and establish no session.
     */
    @Test
    public void login_failed() {
        String sessionCookie = given().contentType(ContentType.URLENC)
                .formParam("j_username", "admin")
                .formParam("j_password", "bla")
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("auth/error"))
                .extract().cookie("quarkus-credential");
        assertThat(sessionCookie, nullValue());
    }

    /**
     * A3: signing out with an established session leads to the confirmation.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void logout_when_logged_in() {
        Csrf.given()
                .cookie("quarkus-credential")
                .redirects()
                .follow(false)
                .when()
                .post(logoutEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", containsString("auth/loggedOut"));
    }

    /**
     * A4: signing out without an active session leads to the start page.
     */
    @Test
    public void logout_when_not_logged_in() {
        Csrf.given()
                .redirects()
                .follow(false)
                .when()
                .post(logoutEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/"));
    }

    /**
     * UC-006 A4: a session that has ended leaves its cookie behind; signing out with it is a sign-out
     * without an active session and leads to the start page, not to the confirmation.
     */
    @Test
    public void logout_with_stale_session_cookie() {
        Csrf.given()
                .cookie("quarkus-credential", "expired-or-forged")
                .redirects()
                .follow(false)
                .when()
                .post(logoutEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/"));
    }

    /**
     * A3 step 2: the confirmation tells the administrator they are signed out.
     */
    @Test
    public void logged_out() {
        given().when().get(loggedOutEndpoint).then().statusCode(HttpStatus.SC_OK).body(containsString("Abgemeldet"));
    }

    /**
     * BR-010, A5: without a session the general administration entry leads to the sign-in page.
     */
    @Test
    public void auth_redirect_to_login() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .when()
                .get(authEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/auth/login"));
    }

    /**
     * BR-010, A2: with a session the general administration entry leads to the start page.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void auth_redirect_to_home_if_logged_in() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .cookie("quarkus-credential")
                .when()
                .get(authEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/"));
    }

    /**
     * BR-009: signing out is refused unless it comes from the system's own form in this browser —
     * without a token, with the token of another browser, and by following a link. The session
     * survives every refused attempt.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void logout_only_from_the_systems_own_form() {
        Csrf.givenWithoutToken()
                .redirects().follow(false)
                .when()
                .post(logoutEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        Csrf.givenWithTokenOfAnotherBrowser()
                .redirects().follow(false)
                .when()
                .post(logoutEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        given()
                .redirects().follow(false)
                .when()
                .get(logoutEndpoint)
                .then()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * BR-009: an error page for an address that matches no endpoint issues no CSRF token, so it
     * offers no sign-out form for a signed-in administrator and still renders instead of failing.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void error_page_without_token_renders_for_a_signed_in_administrator() {
        given()
                .when()
                .get("/")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("action=\"/auth/logout\""));

        given()
                .when()
                .get("/gibt-es-nicht")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(not(containsString("/auth/logout")));
    }
}
