package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
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
                .statusCode(HttpStatus.SC_OK)
                // The event names itself now; the page no longer opens with the generic word
                // "Anlassdetail", which told the visitor nothing.
                .body(containsString(title))
                .body(containsString("Alle Anl"));
    }

    /** The detail page is where the whole formatted description lives (BR-040). */
    @Test
    public void detail_renders_the_formatted_description() {
        String title = "CLEANUP detail_renders_formatting";
        long id = EventFixture.createEvent(title, java.time.LocalDate.now(),
                "<p>Programm mit <strong>Workshops</strong></p><ul><li>Prag</li></ul>");
        String slug = ((Event) Event.findById(id)).slug;

        given()
                .queryParam("slug", slug)
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("<strong>Workshops</strong>"))
                .body(containsString("<li>Prag</li>"));
    }

    @Test
    public void show_detail_non_existing() {
        given()
                .queryParam("slug", "non_existing_slug")
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));
    }
}
