package tools.cevi.infra;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class IndexResourceTest {
    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource
    URL indexEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("fgi")
    URL fgiEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("version")
    URL versionEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("admin")
    URL adminEndpoint;

    @Test
    public void fgi_page() {
        given().when().get(fgiEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("FGI - Fachgruppe International"));
    }

    @Test
    @TestSecurity(user = "patrick", roles = { "admin"})
    public void index_forward_to_anlaesse() {
        given()
                .redirects().follow(false)
                .when()
                .get(indexEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/anlaesse"));
    }

    @Test
    public void version_working() {
        given().when().get(versionEndpoint).then().statusCode(HttpStatus.SC_OK).body(containsString("Version:"));
    }

    @Test
    public void admin_redirect_to_login() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .when()
                .get(adminEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
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
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/"));
    }
}
