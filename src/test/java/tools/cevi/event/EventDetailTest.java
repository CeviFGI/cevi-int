package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class EventDetailTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("detail")
    URL detailEndpoint;

    @Test
    public void show_detail() {
        String title = "CLEANUP show_detail";
        long id = EventFixture.createEvent(title);
        String slug = ((Event)Event.findById(id)).slug;

        given()
                .queryParam("slug", slug)
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Anlassdetail"));
    }

    @Test
    public void show_detail_non_existing() {
        given()
                .queryParam("slug", "non_existing_slug")
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(404)
                .body(containsString("Nicht gefunden"));
    }
}
