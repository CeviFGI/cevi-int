package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class NotFoundExceptionMapperTest {
    @Test
    public void non_existing_page() {
        given().when().get("/non_existing_page").then().statusCode(HttpStatus.SC_NOT_FOUND).body(containsString("Nicht gefunden"));
    }
}
