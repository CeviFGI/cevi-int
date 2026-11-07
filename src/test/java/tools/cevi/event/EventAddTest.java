package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-002 Manage Events, main success scenario: adding an event, with the short name the
 * administrator supplies or the one derived from the title (BR-004, BR-005, BR-006), A9 for an event
 * whose display date has passed, and BR-001 for a visitor who is not signed in.
 */
@QuarkusTest
public class EventAddTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("detail")
    URL detailEndpoint;

    /**
     * A9: an event whose display date has passed is recorded, but the public event list does not
     * show it; it stays reachable under its Public Address.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_past_display_date_is_stored_but_not_listed() {
        String title = "CLEANUP UC-002 add_with_past_display_date";
        long eventCount = Event.count();

        // The fixture submits the display date 2023-01-01.
        long id = EventFixture.createEventWithRest(eventEndpoint, title, "");

        assertThat(Event.count(), equalTo(eventCount + 1));
        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(not(containsString(title)));
        given().queryParam("slug", ((Event) Event.findById(id)).slug)
                .when().get(detailEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(title));
    }

    /**
     * BR-001, Pre-1: a visitor who is not signed in is sent to the sign-in page instead of the form,
     * both when opening it and when submitting it.
     */
    @Test
    public void login_form_when_add_anonymous() {
        // try to show form
        given()
                .redirects()
                .follow(false)
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));

        // try to submit form
        Csrf.given().formParam("title", "test title")
                .formParam("date", "12-19-2022")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    /**
     * Main success scenario, steps 1 and 2: the administrator gets the empty event form.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_form_when_logged_in() {
        given()
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Neuen Anlass anlegen"));
    }

    /**
     * Main success scenario, steps 6 and 9, BR-005: without a short name the event is recorded with
     * one derived from the title.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_empty_slug() {
        long eventCount = Event.count();
        String title = "CLEANUP add_with_empty_slug";

        EventFixture.createEventWithRest(eventEndpoint, title, "");

        assertThat(Event.count(), equalTo(eventCount+1));
        List<Event> events = Event.listAll();
        var event = events.get(events.size()-1);
        assertThat(event.title, equalTo(title));
        assertThat(event.slug, equalTo("cleanup-add-with-empty-slug")); // if no slug is defined one will be generated
    }

    /**
     * Main success scenario, steps 3 and 9: a short name the administrator supplies is stored as entered.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_slug() {
        long eventCount = Event.count();
        String title = "CLEANUP add_with_slug";

        EventFixture.createEventWithRest(eventEndpoint, title, "add_with_slug");

        assertThat(Event.count(), equalTo(eventCount+1));
        List<Event> events = Event.listAll();
        var event = events.get(events.size()-1);
        assertThat(event.title, equalTo(title));
        assertThat(event.slug, equalTo("add_with_slug")); // if no slug is defined one will be generated
    }

    /**
     * BR-004, A2: a short name already used by another event is rejected and nothing is recorded.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_slug_other_event_same_slug() {
        long eventCount = Event.count();
        String title = "CLEANUP add_with_slug";

        EventFixture.createEventWithRest(eventEndpoint, title, "add_with_slug");

        Csrf.given()
                .formParam("title", title)
                .formParam("slug", "add_with_slug")
                .formParam("date", "17.02.2023")
                .formParam("displayDate", "2023-01-01")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(containsString("Es existiert bereits ein anderer Eintrag mit demselben Slug"));

        assertThat(Event.count(), equalTo(eventCount+1));
        List<Event> events = Event.listAll();
        var event = events.get(events.size()-1);
        assertThat(event.title, equalTo(title));
        assertThat(event.slug, equalTo("add_with_slug")); // if no slug is defined one will be generated
    }

    /**
     * BR-003, A2: a title longer than 255 characters is rejected and nothing is recorded.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_too_long_title() {
        long eventCount = Event.count();

        Csrf.given()
                .formParam("title", "abcde".repeat(60))
                .formParam("slug", "add_with_too_long_title")
                .formParam("date", "17.02.2023")
                .formParam("displayDate", "2023-01-01")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(containsString("Größe muss zwischen 0 und 255 sein"));

        assertThat(Event.count(), equalTo(eventCount));
    }

    /**
     * BR-005: a derived short name that is already taken gets a counter appended.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_twice_with_same_title() {
        long eventCount = Event.count();
        String title = "CLEANUP add_twice_with_same_title";

        EventFixture.createEventWithRest(eventEndpoint, title, "");
        EventFixture.createEventWithRest(eventEndpoint, title, "");

        assertThat(Event.count(), equalTo(eventCount+2));
        List<Event> events = Event.listAll();
        var firstEvent = events.get(events.size()-2);
        var secondEvent = events.get(events.size()-1);
        assertThat(firstEvent.title, equalTo(title));
        assertThat(secondEvent.title, equalTo(title));
        assertThat(firstEvent.slug, equalTo("cleanup-add-twice-with-same-title")); // if no slug is defined one will be generated
        assertThat(secondEvent.slug, equalTo("cleanup-add-twice-with-same-title0")); // same title adds a counter
    }

    /**
     * UC-002 BR-005: the counter keeps counting until the derived short name is free, so a third
     * event with the same title gets its own short name too.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_three_times_with_same_title() {
        String title = "CLEANUP add_three_times_with_same_title";

        EventFixture.createEventWithRest(eventEndpoint, title, "");
        EventFixture.createEventWithRest(eventEndpoint, title, "");
        EventFixture.createEventWithRest(eventEndpoint, title, "");

        List<Event> events = Event.listAll();
        List<String> slugs = events.subList(events.size() - 3, events.size()).stream().map(e -> e.slug).toList();
        assertThat(slugs, equalTo(List.of(
                "cleanup-add-three-times-with-same-title",
                "cleanup-add-three-times-with-same-title0",
                "cleanup-add-three-times-with-same-title1")));
    }

    /**
     * UC-002 BR-006: a short name derived from a title of the maximum length is cut to 252
     * characters, and the counter appended to it keeps it within the 255 characters of BR-003.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_twice_with_longest_title() {
        String title = "CLEANUP " + "a".repeat(247);
        String derived = ("cleanup-" + "a".repeat(247)).substring(0, 252);

        EventFixture.createEventWithRest(eventEndpoint, title, "");
        EventFixture.createEventWithRest(eventEndpoint, title, "");

        List<Event> events = Event.listAll();
        assertThat(events.get(events.size() - 2).slug, equalTo(derived));
        assertThat(events.get(events.size() - 1).slug, equalTo(derived + "0"));
    }
}
