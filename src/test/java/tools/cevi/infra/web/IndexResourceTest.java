package tools.cevi.infra.web;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
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
    public void page_working() {
        given().when().get(indexEndpoint).then().statusCode(200).body(containsString("Herzlich Willkommen"));
    }

    @Test
    public void version_working() {
        given().when().get(versionEndpoint).then().statusCode(200).body(containsString("Version:"));
    }
}
