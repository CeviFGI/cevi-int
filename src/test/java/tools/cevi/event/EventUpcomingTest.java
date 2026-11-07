package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import io.quarkus.narayana.jta.QuarkusTransaction;
import tools.cevi.voluntary.VoluntaryService;

import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-001 Browse Upcoming Events: the event list shows the upcoming events (main success scenario
 * steps 1-4, A1, A3, A4, BR-001 to BR-006, the postcondition that no data changes); and UC-006
 * BR-003: its maintenance links appear only for a signed-in administrator.
 */
@QuarkusTest
public class EventUpcomingTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    /** UC-001 main success scenario, UC-006 BR-003: a visitor sees the events but no maintenance links. */
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
     * UC-001 BR-004: the list is for comparing, the event's own page is for reading. Rendering every
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

    /** UC-001 BR-005: how long an event stays announced is derived from the display date it already has. */
    @Test
    public void list_states_how_much_time_an_event_has_left() {
        EventFixture.createEvent("CLEANUP countdown_in_list", LocalDate.now().plusDays(3));

        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Noch 3 Tage"));
    }

    /** UC-006 BR-003, main success scenario step 7: a signed-in administrator sees the maintenance links. */
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

    /**
     * UC-001 BR-006: the maintenance control is the quieter ghost button, so the list itself stays
     * what a visitor's eye goes to.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void maintenance_control_is_visually_quieter() {
        given()
                .when()
                .get(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("<a class=\"btn btn--ghost\" href=\"/anlaesse/add\">"))
                .body(not(containsString("<a class=\"btn btn--primary\" href=\"/anlaesse/add\">")));
    }

    /** UC-001 BR-002: the closest event comes first. */
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

    /** UC-001 BR-001: a past event stays stored but is not upcoming. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void no_past_events_as_upcoming() {
        String title = "CLEANUP no_past_events_as_upcoming";
        EventFixture.createEvent(title, LocalDate.now().minusDays(5));
        var pastEvent = Event.upcomingEvents().stream().filter(e -> e.title.equals(title)).toList();
        assertThat(pastEvent, empty());
    }

    /**
     * UC-001 main success scenario steps 3-4, BR-003: each event in the list carries its title, its
     * date text and its location, and leads to its own page under its short name.
     */
    @Test
    public void list_presents_date_text_and_location_and_links_to_the_event_page() {
        String title = "CLEANUP list_presents_date_and_location";
        long id = EventFixture.createEvent(title, LocalDate.now().plusDays(20),
                "12. - 19. Juli 2099", "Lissabon", "<p>Ein Sommerlager</p>");
        String slug = ((Event) Event.findById(id)).slug;

        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(title))
                .body(containsString("12. - 19. Juli 2099"))
                .body(containsString("Lissabon"))
                .body(containsString("href=\"/anlaesse/detail?slug=" + slug + "\""));
    }

    /**
     * UC-001 BR-001: the display date decides — an event whose display date is today is still
     * listed, one whose display date was yesterday is not.
     */
    @Test
    public void list_shows_an_event_until_its_display_date_and_hides_it_afterwards() {
        String today = "CLEANUP display_date_today";
        String yesterday = "CLEANUP display_date_yesterday";
        EventFixture.createEvent(today, LocalDate.now());
        EventFixture.createEvent(yesterday, LocalDate.now().minusDays(1));

        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(today))
                .body(not(containsString(yesterday)));
        assertThat(Event.find("title", yesterday).count(), is(1L));
    }

    /** UC-001 BR-002: on the page, the event with the earlier display date is presented first. */
    @Test
    public void list_page_presents_the_closest_event_first() {
        String later = "CLEANUP page_order_later";
        String sooner = "CLEANUP page_order_sooner";
        EventFixture.createEvent(later, LocalDate.now().plusDays(9));
        EventFixture.createEvent(sooner, LocalDate.now().plusDays(8));

        String page = given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertThat(page.indexOf(sooner), greaterThan(-1));
        assertThat(page.indexOf(sooner), lessThan(page.indexOf(later)));
    }

    /** UC-001 A3: a description shorter than the excerpt is shown in full, with no sign of a cut. */
    @Test
    public void short_description_is_presented_in_full_without_an_ellipsis() {
        String title = "CLEANUP short_description";
        EventFixture.createEvent(title, LocalDate.now().plusDays(10), "<p>Kurz und vollst\u00e4ndig</p>");

        String page = given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertThat(page, containsString("<p class=\"card__excerpt\">Kurz und vollst\u00e4ndig</p>"));
    }

    /**
     * UC-001 A1: without any upcoming event the list says that nothing is announced and offers the
     * way to the working group instead.
     */
    @Test
    public void empty_list_explains_and_offers_the_way_to_the_working_group() {
        Map<Integer, LocalDate> hidden = moveUpcomingEventsIntoThePast();
        try {
            given().when().get(eventEndpoint).then()
                    .statusCode(HttpStatus.SC_OK)
                    .body(containsString("Gerade ist nichts ausgeschrieben"))
                    .body(containsString("href=\"/kontakt\""))
                    .body(not(containsString("class=\"card\"")));
        } finally {
            restoreDisplayDates(hidden);
        }
    }

    /**
     * UC-001 A4, BR-006: for a signed-in administrator each event additionally carries its
     * maintenance controls, in their own group apart from the visitor's "Details" control, which
     * stays the primary one.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void maintenance_controls_form_a_separate_group_below_the_visitor_control() {
        long id = EventFixture.createEvent("CLEANUP maintenance_group", LocalDate.now().plusDays(10));

        String page = given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        int card = page.indexOf("CLEANUP maintenance_group");
        int details = page.indexOf("btn btn--primary card__cta", card);
        int group = page.indexOf("<div class=\"card__admin\">", card);
        int edit = page.indexOf("href=\"/anlaesse/edit?id=" + id + "\"", card);
        int delete = page.indexOf("href=\"/anlaesse/delete?id=" + id + "\"", card);
        assertThat(details, greaterThan(card));
        assertThat(group, greaterThan(details));
        assertThat(edit, greaterThan(group));
        assertThat(delete, greaterThan(group));
        assertThat(page.substring(group, delete), not(containsString("btn--primary")));
        // A4 offers controls only; the display date is not repeated as a note for administrators
        assertThat(page, not(containsString("sichtbar bis")));
    }

    /** UC-001 postcondition: browsing the list and an event page changes no data. */
    @Test
    public void browsing_changes_no_data() {
        long id = EventFixture.createEvent("CLEANUP browsing_changes_no_data", LocalDate.now().plusDays(4));
        String slug = ((Event) Event.findById(id)).slug;
        Map<String, List<String>> before = snapshot();

        given().when().get(eventEndpoint).then().statusCode(HttpStatus.SC_OK);
        given().queryParam("slug", slug).when().get(eventEndpoint + "/detail").then().statusCode(HttpStatus.SC_OK);
        given().queryParam("slug", "unknown-short-name").when().get(eventEndpoint + "/detail").then()
                .statusCode(HttpStatus.SC_NOT_FOUND);

        assertThat(snapshot(), is(before));
    }

    private static Map<String, List<String>> snapshot() {
        Map<String, List<String>> state = new LinkedHashMap<>();
        state.put("events", Event.<Event>listAll().stream()
                .sorted(Comparator.comparingInt(e -> e.id)).map(Event::toString).toList());
        state.put("voluntary", VoluntaryService.<VoluntaryService>listAll().stream()
                .map(VoluntaryService::toString).sorted().toList());
        return state;
    }

    /** Hides every stored upcoming event for A1; {@link #restoreDisplayDates} puts them back. */
    private static Map<Integer, LocalDate> moveUpcomingEventsIntoThePast() {
        Map<Integer, LocalDate> original = new LinkedHashMap<>();
        QuarkusTransaction.begin();
        for (Event event : Event.upcomingEvents()) {
            original.put(event.id, event.displayDate);
            event.displayDate = LocalDate.now().minusDays(1);
        }
        QuarkusTransaction.commit();
        return original;
    }

    private static void restoreDisplayDates(Map<Integer, LocalDate> original) {
        QuarkusTransaction.begin();
        original.forEach((id, displayDate) -> {
            Event event = Event.findById(id);
            event.displayDate = displayDate;
        });
        QuarkusTransaction.commit();
    }
}
