package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;

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
                .body(not(containsString("Neuen Anlass eintragen")));
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
                .body(containsString("Neuen Anlass eintragen"));
    }

    @Test
    public void form_save_no_auth() {
        given().contentType(ContentType.URLENC).formParam("title", "test title")
                .formParam("date", "12-19-2022")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"))
                .cookie("quarkus-redirect-location", containsString(eventEndpoint.toString()));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void form_save_auth() {
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

        List<Event> events = Event.listAll();
        assertThat(events, is(not(empty())));
        assertThat(events.get(events.size()-1).title, equalTo("test title"));
    }
}
