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
        given().when().get("/non_existing_page").then().statusCode(HttpStatus.SC_NOT_FOUND).body(containsString("Diese Seite gibt es nicht"));
    }

    /**
     * An exception mapper is not a resource method, so {@code @Produces} on it is never read. Left
     * to itself the answer carries no Content-Type and the browser shows the page's own markup as
     * text — which is what the error page looked like to every visitor who mistyped an address.
     */
    @Test
    public void the_error_page_is_served_as_a_page() {
        given().when().get("/non_existing_page").then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(containsString("text/html"));
    }
}
