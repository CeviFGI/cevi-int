package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class EventDeleteTest{
    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

    @Test
    public void login_when_delete_anonymous() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .redirects()
                .follow(false)
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_form() {
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Anlass löschen"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_form_non_existing() {
        long eventCount = Event.count();

        given()
                .queryParam("id", 10000000)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Nicht gefunden"));

        assertThat(Event.count(), equalTo(eventCount));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void confirm_delete() {
        String title = "CLEANUP confirm_delete";

        long id = EventFixture.createEvent(title);
        long eventCount = Event.count();

        given()
                .queryParam("id", id)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Hier findest du eine Liste"));

        assertThat(Event.count(), equalTo(eventCount-1));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void confirm_delete_non_existing() {
        long eventCount = Event.count();

        given()
                .queryParam("id", 10000000)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Nicht gefunden"));

        assertThat(Event.count(), equalTo(eventCount));
    }
}
