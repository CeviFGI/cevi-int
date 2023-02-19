package tools.cevi.event;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class EventResourceTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

    @Test
    public void list_no_auth() {
        List<Event> events = Event.listAll();
        assertThat(events, is(not(empty())));
        given()
                .when()
                .get(eventEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(events.get(0).description))
                .body(not(containsString("Neuen Anlass eintragen")))
                .body(not(containsString("Bearbeiten")));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void list_with_auth() {
        List<Event> events = Event.listAll();
        assertThat(events, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(eventEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(events.get(0).description))
                .body(containsString("Neuen Anlass eintragen"))
                .body(containsString("Bearbeiten"));
    }

    @Test
    public void add_form_no_auth() {
        given()
                .redirects()
                .follow(false)
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_form_with_auth() {
        given()
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Neuen Anlass anlegen"));
    }

    @Test
    public void add_submit_no_auth() {
        given().contentType(ContentType.URLENC).formParam("title", "test title")
                .formParam("date", "12-19-2022")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_submit_with_auth() {
        long eventCount = Event.count();
        String title = "CLEANUP add_submit_with_auth";

        given()
                .contentType(ContentType.URLENC)
                .formParam("title", title)
                .formParam("date", "17.02.2023")
                .formParam("displayDate", "2023-01-01")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(200);

        assertThat(Event.count(), equalTo(eventCount+1));
        List<Event> events = Event.listAll();
        assertThat(events.get(events.size()-1).title, equalTo(title));
        cleanupEvent(title);
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void order_upcomingevents_by_displayDate() {
        String title1 = "CLEANUP order_upcomingevents_by_displayDate 1";
        String title2 = "CLEANUP order_upcomingevents_by_displayDate 2";
        createEvent(title1, LocalDate.now().plusDays(5));
        createEvent(title2, LocalDate.now().plusDays(3));

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

        cleanupEvent(title1, title2);
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void dont_show_past_events_as_upcoming() {
        String title = "CLEANUP dont_show_past_events_as_upcoming";
        createEvent(title, LocalDate.now().minusDays(5));
        var pastEvent = Event.upcomingEvents().stream().filter(e -> e.title.equals(title)).toList();
        assertThat(pastEvent, empty());
    }

    private long createEvent(String title, LocalDate displayDate) {
        QuarkusTransaction.begin();
        Event event = new Event();
        event.title = title;
        event.date = "some date";
        event.displayDate = displayDate;
        event.location = "some location";
        event.description = "some description";
        event.persist();
        QuarkusTransaction.commit();
        return event.id;
    }

    private long createEvent(String title) {
        return createEvent(title, LocalDate.now());
    }

    private void cleanupEvent(String... title) {
        QuarkusTransaction.begin();
        Arrays.stream(title).forEach(Event::deleteByTitle);
        QuarkusTransaction.commit();
    }

    @Test
    public void edit_form_no_auth() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .redirects()
                .follow(false)
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_form_with_auth() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Anlass bearbeiten"));
    }

    @Test
    public void edit_submit_no_auth() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given().contentType(ContentType.URLENC)
                .formParam("id", firstId)
                .formParam("title", "test title")
                .formParam("date", "12-19-2022")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_submit_with_auth() {
        String title = "CLEANUP edit_submit_with_auth";
        long id = createEvent(title);
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("title", title)
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(200);

        assertThat(Event.count(), equalTo(eventCount));

        QuarkusTransaction.begin();
        assertThat(((Event) Event.findById(id)).location, equalTo(uuid.toString()));
        QuarkusTransaction.rollback();
        cleanupEvent(title);
    }

    @Test
    public void delete_no_auth() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .redirects()
                .follow(false)
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_with_auth() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Anlass löschen"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_confirmed_with_auth() {
        String title = "CLEANUP delete_confirmed_with_auth";

        long id = createEvent(title);
        long eventCount = Event.count();

        given()
                .queryParam("id", id)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Anlässe"));

        assertThat(Event.count(), equalTo(eventCount-1));
        cleanupEvent(title);
    }
}
