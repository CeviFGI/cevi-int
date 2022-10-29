package tools.cevi.infra.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class IndexResourceTest {
    @Test
    public void page_working() {
        given().when().get("/").then().statusCode(200);
    }

    @Test
    public void version_working() {
        given().when().get("/version").then().statusCode(200);
    }
}
