package tools.cevi.infra.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class EventResourceTest {
    @Test
    public void page_working() {
        given().when().get("/anlaesse").then().statusCode(200);
    }
}
