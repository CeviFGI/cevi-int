package tools.cevi.event;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.EventFixture;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-002 Manage Events, A3 to A5 and A8: removing an event after confirmation (BR-007), only from
 * the system's own form (A7, BR-009), a removed event that is opened again (A5), and a removal the
 * store refuses (A8).
 */
@QuarkusTest
public class EventDeleteTest{
    private static final String FAILING_TITLE = "CLEANUP A8 cannot be removed";

    @Inject
    EntityManager entityManager;

    @AfterEach
    void dropFailureTrigger() {
        QuarkusTransaction.requiringNew().run(() ->
                entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc002_a8_delete").executeUpdate());
    }

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

    /** BR-001, Pre-1: a visitor who is not signed in is sent to the sign-in page instead. */
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

    /** BR-001, Pre-1 (NFR-006): confirming a removal without a session leads to the sign-in page; the event stays. */
    @Test
    public void confirm_delete_without_administrator_role_is_refused() {
        long id = EventFixture.createEvent("CLEANUP confirm_delete_without_administrator_role");

        Csrf.given()
                .formParam("id", id)
                .redirects().follow(false)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));

        assertThat(Event.findById((int) id) != null, equalTo(true));
    }

    /** BR-001, Pre-2 (NFR-006): a signed-in user without the administrator role cannot remove an event. */
    @Test
    @TestSecurity(user = "visitor", roles = {"user"})
    public void confirm_delete_as_non_administrator_is_forbidden() {
        long id = EventFixture.createEvent("CLEANUP confirm_delete_as_non_administrator");

        Csrf.given()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        assertThat(Event.findById((int) id) != null, equalTo(true));
    }

    /** A3 step 1, BR-007: the administrator is asked to confirm the removal first. */
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

    /** A4, BR-007: opening the confirmation page and leaving it without confirming removes nothing. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_form_left_without_confirming_keeps_the_event() {
        long id = EventFixture.createEvent("CLEANUP delete_form_left_without_confirming");

        given().queryParam("id", id).when().get(deleteEndpoint).then().statusCode(HttpStatus.SC_OK);
        given().when().get("/anlaesse").then().statusCode(HttpStatus.SC_OK);

        assertThat(Event.findById((int) id) != null, equalTo(true));
    }

    /** A5: the confirmation page of an event that no longer exists says so. */
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
                .body(containsString("Diese Seite gibt es nicht"));

        assertThat(Event.count(), equalTo(eventCount));
    }

    /** A3 steps 2 to 4: the confirmed removal removes the event and returns to the event list. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void confirm_delete() {
        String title = "CLEANUP confirm_delete";

        long id = EventFixture.createEvent(title);
        long eventCount = Event.count();

        Csrf.given()
                .formParam("id", id)
                .redirects()
                .follow(false)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", containsString("/anlaesse"));

        assertThat(Event.count(), equalTo(eventCount-1));
    }

    /** A5: confirming the removal of an event that no longer exists says so and removes nothing. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void confirm_delete_non_existing() {
        long eventCount = Event.count();

        Csrf.given()
                .formParam("id", 10000000)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));

        assertThat(Event.count(), equalTo(eventCount));
    }

    /**
     * A GET must not delete: a prefetching browser, a link preview or a plain link from another
     * site would otherwise be enough to remove an event (BR-009, NFR-021).
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void get_with_confirmed_parameter_does_not_delete() {
        String title = "CLEANUP get_with_confirmed_parameter_does_not_delete";

        long id = EventFixture.createEvent(title);
        long eventCount = Event.count();

        given()
                .queryParam("id", id)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Anlass löschen"));

        assertThat(Event.count(), equalTo(eventCount));
    }

    /** A7, BR-009: without a CSRF token the removal is refused before it reaches the endpoint (NFR-011). */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void confirm_delete_without_csrf_token_is_rejected() {
        String title = "CLEANUP confirm_delete_without_csrf_token_is_rejected";

        long id = EventFixture.createEvent(title);
        long eventCount = Event.count();

        Csrf.givenWithoutToken()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(Event.count(), equalTo(eventCount));
    }

    /**
     * A7, BR-009: a form token handed out to another browser does not match this browser's token
     * cookie, so the removal is refused and the event stays.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void confirm_delete_with_token_of_another_browser_is_rejected() {
        long id = EventFixture.createEvent("CLEANUP confirm_delete_with_token_of_another_browser");
        long eventCount = Event.count();

        Csrf.givenWithTokenOfAnotherBrowser()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        Csrf.givenWithoutCookie()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(Event.count(), equalTo(eventCount));
    }

    /**
     * A8: the store refuses the removal. The event is preserved, and the confirmation page comes
     * back with a notice that the event could not be removed.
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void removal_that_cannot_be_completed_keeps_the_event() {
        long id = EventFixture.createEvent(FAILING_TITLE);
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "CREATE TRIGGER uc002_a8_delete BEFORE DELETE ON events WHEN OLD.title = '"
                        + FAILING_TITLE + "' BEGIN SELECT RAISE(ABORT, 'UC-002 A8 test'); END").executeUpdate());

        String html = Csrf.given()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .extract().asString();

        assertThat(html, containsString("Anlass löschen"));
        assertThat(html, containsString("Der Anlass konnte nicht gelöscht werden"));
        assertThat(html, containsString(FAILING_TITLE));
        dropFailureTrigger();
        assertThat(Event.findById((int) id) != null, equalTo(true));
    }
}
