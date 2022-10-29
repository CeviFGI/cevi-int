package tools.cevi.infra.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class IndexPageTest {
    @Test
    public void page_working() {
        given().when().get("/").then().statusCode(200);
    }
}
