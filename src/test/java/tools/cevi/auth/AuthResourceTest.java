package tools.cevi.auth;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.StringContains.containsString;

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

    @Test
    public void login_form() {
        given().when().get(loginEndpoint).then().statusCode(200).body(containsString("Benutzer:"));
    }

    @Test
    public void login_success() {
        given().contentType(ContentType.URLENC)
                .formParam("j_username", "patrick")
                .formParam("j_password", "patrick")
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(302)
                .header("location", is("http://localhost:8081/"))
                .cookie("quarkus-credential");
    }

    @Test
    public void login_failed() {
        given().contentType(ContentType.URLENC)
                .formParam("j_username", "patrick")
                .formParam("j_password", "bla")
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(302)
                .header("location", containsString("auth/error"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void logout_when_logged_in() {
        given()
                .cookie("quarkus-credential")
                .redirects()
                .follow(false)
                .when()
                .get(logoutEndpoint)
                .then()
                .statusCode(307)
                .header("location", containsString("auth/loggedOut"));
    }

    @Test
    public void logout_when_not_logged_in() {
        given()
                .redirects()
                .follow(false)
                .when()
                .get(logoutEndpoint)
                .then()
                .statusCode(307)
                .header("location", is("http://localhost:8081/"));
    }

    @Test
    public void logged_out() {
        given().when().get(loggedOutEndpoint).then().statusCode(200).body(containsString("Abgemeldet"));
    }
}
