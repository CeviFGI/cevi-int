package tools.cevi.event;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
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

        given()
                .contentType(ContentType.URLENC)
                .formParam("title", "test title")
                .formParam("date", "12-19-2022")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(200);

        assertThat(Event.count(), equalTo(eventCount+1));
        List<Event> events = Event.listAll();
        assertThat(events.get(events.size()-1).title, equalTo("test title"));
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
        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;
        long eventCount = Event.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", firstId)
                .formParam("title", "title_" + uuid)
                .formParam("date", "12-19-2022")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(200);

        assertThat(Event.count(), equalTo(eventCount));

        QuarkusTransaction.begin();
        assertThat(((Event) Event.findById(firstId)).title, equalTo("title_" + uuid));
        QuarkusTransaction.rollback();
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
        long eventCount = Event.count();

        long firstId = ((Event) Event.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Anlässe"));

        assertThat(Event.count(), equalTo(eventCount-1));
    }
}
