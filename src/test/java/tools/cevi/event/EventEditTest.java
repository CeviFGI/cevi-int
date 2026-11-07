package tools.cevi.event;

import io.quarkus.narayana.jta.QuarkusTransaction;
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
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-002 Manage Events, A1: changing an existing event, keeping or deriving its short name
 * (BR-004, BR-005), A5 for an event that no longer exists, and BR-001 for a visitor who is not
 * signed in.
 */
@QuarkusTest
public class EventEditTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

    /**
     * BR-001, Pre-1: a visitor who is not signed in is sent to the sign-in page instead of the edit
     * form, both when opening it and when submitting it.
     */
    @Test
    public void login_when_edit_anonymous() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        // try to show form
        given()
                .redirects()
                .follow(false)
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));

        // try to submit
        Csrf.given()
                .formParam("id", firstId)
                .formParam("title", "test title")
                .formParam("slug", firstId)
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
     * A1 step 1: the administrator gets the form for an existing event.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_form_when_logged_in() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Anlass bearbeiten"));
    }

    /** A5: the edit page of an event that no longer exists says so. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_form_non_existing() {
        given()
                .queryParam("id", 10000000)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));
    }

    /**
     * A1, BR-005: a short name emptied while changing is derived anew from the changed title.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_empty_slug() {
        String title = "CLEANUP edit_empty_slug";
        String titleedited = title + " edited";
        long id = EventFixture.createEvent(title);
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        Csrf.given()
                .formParam("id", id)
                .formParam("title", titleedited)
                .formParam("slug", "")
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);

        assertThat(Event.count(), equalTo(eventCount));

        var event = (Event) Event.findById(id);
        assertThat(event.location, equalTo(uuid.toString()));
        assertThat(event.slug, equalTo("cleanup-edit-empty-slug-edited")); // generate a new slug
    }

    /**
     * A1, BR-005: a short name derived anew gets a counter when another event already uses it.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_empty_slug_other_event_with_same_title() {
        String title = "CLEANUP edit_empty_slug";
        String titleedited = title + " edited";
        long id = EventFixture.createEvent(title);
        EventFixture.createEvent(titleedited);
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        Csrf.given()
                .formParam("id", id)
                .formParam("title", titleedited)
                .formParam("slug", "")
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);

        assertThat(Event.count(), equalTo(eventCount));

        var event = (Event) Event.findById(id);
        assertThat(event.location, equalTo(uuid.toString()));
        assertThat(event.slug, equalTo("cleanup-edit-empty-slug-edited0")); // generate a new slug with counter as there is already one for another event
    }

    /**
     * A1: changing the title keeps the short name the event already has.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_change_title_keep_slug() {
        String title = "CLEANUP edit_change_title_keep_slug";
        long id = EventFixture.createEvent(title);
        String slug = ((Event) Event.findById(id)).slug;
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        Csrf.given()
                .formParam("id", id)
                .formParam("title", "other")
                .formParam("slug", slug)
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);

        assertThat(Event.count(), equalTo(eventCount));

        var event = (Event) Event.findById(id);
        assertThat(event.slug, equalTo(slug)); // keep the slug from insertion
    }

    /**
     * A1, A2, BR-004: a short name used by another event is rejected when changing.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_slug_same_as_other_event() {
        String title = "CLEANUP edit_empty_slug_other_event_with_same_title";
        String titleOther = "CLEANUP edit_empty_slug_other_event_with_same_title Other";
        long id = EventFixture.createEvent(title);
        EventFixture.createEvent(titleOther);

        Csrf.given()
                .formParam("id", id)
                .formParam("title", title)
                .formParam("slug", "cleanup-edit-empty-slug-other-event-with-same-title-other")
                .formParam("date", "some date")
                .formParam("location", "location")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(containsString("Es existiert bereits ein anderer Eintrag mit demselben Slug"));
    }

    /**
     * A5: a change submitted for an event that was removed after its form was opened is answered
     * with the page saying the event does not exist, and no event is recorded in its place.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_submit_for_removed_event() {
        long id = EventFixture.createEvent("CLEANUP edit_submit_for_removed_event");
        QuarkusTransaction.requiringNew().run(() -> Event.deleteById((int) id));
        long eventCount = Event.count();

        Csrf.given()
                .formParam("id", id)
                .formParam("title", "CLEANUP edit_submit_for_removed_event changed")
                .formParam("slug", "")
                .formParam("date", "some date")
                .formParam("displayDate", "2030-01-01")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));

        assertThat(Event.count(), equalTo(eventCount));
    }
}
