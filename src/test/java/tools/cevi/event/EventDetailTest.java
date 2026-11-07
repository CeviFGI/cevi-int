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

/**
 * UC-001 Browse Upcoming Events: the event's own page (main success scenario steps 5-6, A2,
 * BR-003, BR-004).
 */
@QuarkusTest
public class EventDetailTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("detail")
    URL detailEndpoint;

    /** Main success scenario step 5, BR-003: the event is reachable under its short name. */
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

    /** The detail page is where the whole formatted description lives (UC-001 BR-004). */
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

    /**
     * A2: an unknown short name leads to a page that says the page does not exist and offers the
     * way back to the event list.
     */
    @Test
    public void show_detail_non_existing() {
        given()
                .queryParam("slug", "non_existing_slug")
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"))
                .body(containsString("href=\"/anlaesse\""));
    }

    /**
     * Main success scenario steps 5-6: the page presents what a visitor needs to decide — date
     * text and location — and the address under which it can be passed on.
     */
    @Test
    public void detail_presents_date_location_and_its_address() {
        String title = "CLEANUP detail_presents_date_location";
        long id = EventFixture.createEvent(title, java.time.LocalDate.now().plusDays(5),
                "3. - 9. August 2099", "Lissabon", "<p>Ein Sommerlager</p>");
        String slug = ((Event) Event.findById(id)).slug;

        given()
                .queryParam("slug", slug)
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("3. - 9. August 2099"))
                .body(containsString("Lissabon"))
                .body(containsString("/anlaesse/detail?slug=" + slug + "\""));
    }

    /**
     * Main success scenario step 5, A4: the maintenance controls belong to the list only; the
     * event's own page shows a signed-in administrator none, and states no remaining time
     * (BR-005 names the list).
     */
    @Test
    @io.quarkus.test.security.TestSecurity(user = "admin", roles = {"admin"})
    public void detail_offers_no_maintenance_controls_and_no_countdown() {
        String title = "CLEANUP detail_without_admin_links";
        long id = EventFixture.createEvent(title, java.time.LocalDate.now().plusDays(5));
        String slug = ((Event) Event.findById(id)).slug;

        given()
                .queryParam("slug", slug)
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(title))
                .body(org.hamcrest.Matchers.not(containsString("/anlaesse/edit?id=")))
                .body(org.hamcrest.Matchers.not(containsString("/anlaesse/delete?id=")))
                .body(org.hamcrest.Matchers.not(containsString("Noch 5 Tage")));
    }

    /** BR-007: after the description the page offers the way to the working group. */
    @Test
    public void detail_leads_to_the_working_group_after_the_description() {
        long id = EventFixture.createEvent("CLEANUP detail_leads_to_working_group");
        String slug = ((Event) Event.findById(id)).slug;

        String page = given()
                .queryParam("slug", slug)
                .when()
                .get(detailEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        int description = page.indexOf("some description");
        // the header and the footer link the contact page on every page; this is the offer itself
        int contact = page.indexOf("href=\"/kontakt\">Schreib uns</a>", description);
        org.hamcrest.MatcherAssert.assertThat(description, org.hamcrest.Matchers.greaterThan(-1));
        org.hamcrest.MatcherAssert.assertThat(contact, org.hamcrest.Matchers.greaterThan(description));
    }
}
