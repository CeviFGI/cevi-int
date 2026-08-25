package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class EventUpcomingTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @Test
    public void show_upcoming() {
        List<Event> events = Event.upcomingEvents();
        assertThat(events, is(not(empty())));
        given()
                .when()
                .get(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(events.get(0).title))
                .body(not(containsString("Neuen Anlass eintragen")))
                .body(not(containsString("Bearbeiten")));
    }

    /**
     * BR-040: the list is for comparing, the event's own page is for reading. Rendering every
     * description in full is what made the list unusable on a phone, so the excerpt has to be
     * there and the tail of the text has to be absent.
     */
    @Test
    public void list_shows_an_excerpt_and_the_event_page_the_whole_text() {
        String title = "CLEANUP list_shows_an_excerpt";
        String tail = "SCHLUSSDESTEXTES";
        String description = "<p>" + "Wort ".repeat(60) + tail + "</p>";
        long id = EventFixture.createEvent(title, LocalDate.now().plusDays(10), description);
        String slug = ((Event) Event.findById(id)).slug;

        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(title))
                .body(containsString("Wort"))
                .body(not(containsString(tail)));

        given().queryParam("slug", slug)
                .when().get(eventEndpoint + "/detail").then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(tail));
    }

    /** BR-041: how long an event stays announced is derived from the display date it already has. */
    @Test
    public void list_states_how_much_time_an_event_has_left() {
        EventFixture.createEvent("CLEANUP countdown_in_list", LocalDate.now().plusDays(3));

        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Noch 3 Tage"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_link_when_logged_in() {
        List<Event> events = Event.upcomingEvents();
        assertThat(events, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(events.get(0).title))
                .body(containsString("Neuen Anlass eintragen"))
                .body(containsString("Bearbeiten"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void order_upcoming_by_displayDate() {
        String title1 = "CLEANUP order_upcoming_by_displayDate 1";
        String title2 = "CLEANUP order_upcoming_by_displayDate 2";
        EventFixture.createEvent(title1, LocalDate.now().plusDays(5));
        EventFixture.createEvent(title2, LocalDate.now().plusDays(3));

        List<Event> events = Event.upcomingEvents();
        int position1 = -1;
        int position2 = -1;
        for (int i=0; i<events.size(); i++) {
            if (events.get(i).title.equals(title1)) {
                position1 = i;
            } else if (events.get(i).title.equals(title2)) {
                position2 = i;
            }
        }

        // both events are found
        assertThat(position1, greaterThan(-1));
        assertThat(position2, greaterThan(-1));
        // the second event comes first because the displayDate is before the first event
        assertThat(position2, lessThan(position1));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void no_past_events_as_upcoming() {
        String title = "CLEANUP no_past_events_as_upcoming";
        EventFixture.createEvent(title, LocalDate.now().minusDays(5));
        var pastEvent = Event.upcomingEvents().stream().filter(e -> e.title.equals(title)).toList();
        assertThat(pastEvent, empty());
    }
}
