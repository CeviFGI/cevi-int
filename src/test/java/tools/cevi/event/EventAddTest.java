package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class EventAddTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

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
    public void add_form_when_logged_in() {
        given()
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Neuen Anlass anlegen"));
    }

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

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_slug_other_event_same_slug() {
        long eventCount = Event.count();
        String title = "CLEANUP add_with_slug";

        EventFixture.createEventWithRest(eventEndpoint, title, "add_with_slug");

        given()
                .contentType(ContentType.URLENC)
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

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_with_too_long_title() {
        long eventCount = Event.count();

        given()
                .contentType(ContentType.URLENC)
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
}
