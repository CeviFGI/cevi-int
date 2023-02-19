package tools.cevi.infra;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class IndexResourceTest {
    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource
    URL indexEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("version")
    URL versionEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("admin")
    URL adminEndpoint;

    @Test
    public void index_when_not_logged_in() {
        given().when().get(indexEndpoint).then().statusCode(200)
                .body(containsString("Herzlich Willkommen"))
                .body(not(containsString("Ausloggen")));
    }

    @Test
    @TestSecurity(user = "patrick", roles = { "admin"})
    public void index_when_logged_in() {
        given()
                .cookie("quarkus-credential")
                .when()
                .get(indexEndpoint).then()
                .statusCode(200)
                .body(containsString("Herzlich Willkommen patrick"))
                .body(containsString("Ausloggen"));
    }

    @Test
    public void version_working() {
        given().when().get(versionEndpoint).then().statusCode(200).body(containsString("Version:"));
    }

    @Test
    public void admin_redirect_to_login() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .when()
                .get(adminEndpoint)
                .then()
                .statusCode(307)
                .header("location", is("http://localhost:8081/auth/login"));
    }

    @Test
    @TestSecurity(user = "patrick", roles = { "admin"})
    public void admin_redirect_to_home_if_logged_in() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .cookie("quarkus-credential")
                .when()
                .get(adminEndpoint)
                .then()
                .statusCode(307)
                .header("location", is("http://localhost:8081/"));
    }
}
