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
                .body(containsString(events.get(0).description))
                .body(not(containsString("Neuen Anlass eintragen")))
                .body(not(containsString("Bearbeiten")));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_link_when_logged_in() {
        List<Event> events = Event.listAll();
        assertThat(events, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(events.get(0).description))
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
