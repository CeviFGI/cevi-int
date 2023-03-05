package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class EventEditTest {
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

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
        given().contentType(ContentType.URLENC)
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

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_form_when_logged_in() {
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
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_empty_slug() {
        String title = "CLEANUP edit_empty_slug";
        String titleedited = title + " edited";
        long id = EventFixture.createEvent(title);
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("title", titleedited)
                .formParam("slug", "")
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(307);

        assertThat(Event.count(), equalTo(eventCount));

        var event = (Event) Event.findById(id);
        assertThat(event.location, equalTo(uuid.toString()));
        assertThat(event.slug, equalTo("cleanup-edit-empty-slug-edited")); // generate a new slug
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_empty_slug_other_event_with_same_title() {
        String title = "CLEANUP edit_empty_slug";
        String titleedited = title + " edited";
        long id = EventFixture.createEvent(title);
        EventFixture.createEvent(titleedited);
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("title", titleedited)
                .formParam("slug", "")
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(307);

        assertThat(Event.count(), equalTo(eventCount));

        var event = (Event) Event.findById(id);
        assertThat(event.location, equalTo(uuid.toString()));
        assertThat(event.slug, equalTo("cleanup-edit-empty-slug-edited0")); // generate a new slug with counter as there is already one for another event
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_change_title_keep_slug() {
        String title = "CLEANUP edit_change_title_keep_slug";
        long id = EventFixture.createEvent(title);
        String slug = ((Event) Event.findById(id)).slug;
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("title", "other")
                .formParam("slug", slug)
                .formParam("date", "some date")
                .formParam("location", uuid)
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(307);

        assertThat(Event.count(), equalTo(eventCount));

        var event = (Event) Event.findById(id);
        assertThat(event.slug, equalTo(slug)); // keep the slug from insertion
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_slug_same_as_other_event() {
        String title = "CLEANUP edit_empty_slug_other_event_with_same_title";
        String titleOther = "CLEANUP edit_empty_slug_other_event_with_same_title Other";
        long id = EventFixture.createEvent(title);
        EventFixture.createEvent(titleOther);

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", id)
                .formParam("title", title)
                .formParam("slug", "cleanup-edit-empty-slug-other-event-with-same-title-other")
                .formParam("date", "some date")
                .formParam("location", "location")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(400)
                .body(containsString("Es existiert bereits ein anderer Eintrag mit demselben Slug"));
    }
}
