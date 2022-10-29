package tools.cevi.infra.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class VolontariatPageTest {
    @Test
    public void page_working() {
        given().when().get("/volontariat").then().statusCode(200);
    }
}
