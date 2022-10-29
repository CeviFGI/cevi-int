package tools.cevi.infra.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class NotFoundExceptionMapperTest {
    @Test
    public void non_existing_page() {
        given().when().get("/non_existing_page").then().statusCode(404);
    }
}
