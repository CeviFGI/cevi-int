package tools.cevi.infra;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;

import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
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
}
