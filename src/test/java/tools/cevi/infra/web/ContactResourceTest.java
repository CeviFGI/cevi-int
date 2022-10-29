package tools.cevi.infra.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class ContactResourceTest {
    @Test
    public void page_working() {
        given().when().get("/kontakt").then().statusCode(200);
    }
}
