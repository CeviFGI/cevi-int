package tools.cevi.event;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * UC-002 Manage Events: the rules the event form enforces on what it accepts — BR-001, BR-002,
 * BR-003, BR-008, A2, A6 and A7 for adding (main success scenario) and changing (A1) an event.
 */
@QuarkusTest
class EventFormRulesTest {
    private static final String FAILING_TITLE = "CLEANUP A6 cannot be stored";
    private static final String FAILING_EDIT_TITLE = "CLEANUP A6 change cannot be stored";

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

    @Inject
    EntityManager entityManager;

    @AfterEach
    void dropFailureTriggers() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc002_a6_insert").executeUpdate();
            entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc002_a6_update").executeUpdate();
        });
    }

    /** BR-001: a signed-in user without the administrator role can neither open nor submit the form. */
    @Test
    @TestSecurity(user = "visitor", roles = {"user"})
    void non_administrator_is_refused() {
        long eventCount = Event.count();

        given().redirects().follow(false).when().get(addEndpoint)
                .then().statusCode(HttpStatus.SC_FORBIDDEN);
        submit(Csrf.given(), validEntry("CLEANUP BR-001 visitor"))
                .then().statusCode(HttpStatus.SC_FORBIDDEN);

        assertThat(Event.count(), equalTo(eventCount));
    }

    /** Main success scenario step 2: the empty form has today's date pre-filled as the display date. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void add_form_prefills_today_as_display_date() {
        given().when().get(addEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("name=\"displayDate\" value=\"" + LocalDate.now() + "\""));
    }

    /**
     * BR-002, A2: every mandatory value is checked; a missing one rejects the entry, keeps what was
     * typed and marks the field that needs correction.
     */
    @ParameterizedTest
    @ValueSource(strings = {"title", "date", "displayDate", "location", "description"})
    @TestSecurity(user = "admin", roles = {"admin"})
    void missing_mandatory_value_is_rejected(String field) {
        long eventCount = Event.count();
        Map<String, String> entry = validEntry("CLEANUP BR-002 " + field);
        entry.put(field, "");

        String html = submit(Csrf.given(), entry).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertFieldMarked(html, field);
        assertValuesKept(html, entry, field);
        assertThat(Event.count(), equalTo(eventCount));
    }

    /** BR-003, A2: each value is limited — 255 characters, the description 65535 — and one over is rejected. */
    @ParameterizedTest
    @CsvSource({"title, 255", "slug, 255", "date, 255", "location, 255", "description, 65535"})
    @TestSecurity(user = "admin", roles = {"admin"})
    void value_over_its_limit_is_rejected(String field, int limit) {
        long eventCount = Event.count();
        Map<String, String> entry = validEntry("CLEANUP BR-003 " + field);
        entry.put(field, "a".repeat(limit + 1));

        String html = submit(Csrf.given(), entry).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertFieldMarked(html, field);
        assertThat(Event.count(), equalTo(eventCount));
    }

    /** BR-003: a value of exactly its limit is accepted. */
    @ParameterizedTest
    @CsvSource({"slug, 255", "date, 255", "location, 255", "description, 65535"})
    @TestSecurity(user = "admin", roles = {"admin"})
    void value_at_its_limit_is_accepted(String field, int limit) {
        Map<String, String> entry = validEntry("CLEANUP BR-003 at limit " + field);
        entry.put(field, "b".repeat(limit));

        submit(Csrf.given(), entry).then().statusCode(HttpStatus.SC_SEE_OTHER);

        Event stored = Event.find("title", entry.get("title")).firstResult();
        EventFixture.trackForCleanup(stored.id);
        assertThat(fieldOf(stored, field).length(), is(limit));
    }

    /** BR-008: for a new event an unreadable display date leaves the entry incomplete, so it is rejected. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void new_event_with_unreadable_display_date_is_rejected() {
        long eventCount = Event.count();
        Map<String, String> entry = validEntry("CLEANUP BR-008 add");
        entry.put("displayDate", "31.12.2099");

        String html = submit(Csrf.given(), entry).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertFieldMarked(html, "displayDate");
        assertThat(Event.count(), equalTo(eventCount));
    }

    /** BR-008, A1: on a change an unreadable display date is ignored and the stored one is kept. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void changed_event_with_unreadable_display_date_keeps_the_stored_one() {
        LocalDate stored = LocalDate.now().plusDays(10);
        long id = EventFixture.createEvent("CLEANUP BR-008 edit", stored);
        Map<String, String> entry = validEntry("CLEANUP BR-008 edit changed");
        entry.put("id", Long.toString(id));
        entry.put("displayDate", "not a date");

        submit(Csrf.given(), entry).then().statusCode(HttpStatus.SC_SEE_OTHER);

        Event changed = Event.findById((int) id);
        assertThat(changed.title, equalTo("CLEANUP BR-008 edit changed"));
        assertThat(changed.displayDate, equalTo(stored));
    }

    /** A7, BR-009: an addition without the token of the system's own form is refused, nothing is stored. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void add_without_form_token_is_refused() {
        long eventCount = Event.count();

        submit(Csrf.givenWithoutToken(), validEntry("CLEANUP A7 add"))
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(Event.count(), equalTo(eventCount));
    }

    /**
     * A7, BR-009: a form token handed out to another browser, or one sent without this browser's
     * token cookie, is refused; nothing is stored.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void add_with_token_of_another_browser_is_refused() {
        long eventCount = Event.count();

        submit(Csrf.givenWithTokenOfAnotherBrowser(), validEntry("CLEANUP A7 foreign token"))
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);
        submit(Csrf.givenWithoutCookie(), validEntry("CLEANUP A7 no cookie"))
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(Event.count(), equalTo(eventCount));
    }

    /** BR-003: the form lets the administrator enter a short name of the full 255 characters. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void short_name_field_allows_255_characters() {
        given().when().get(addEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("name=\"slug\" maxlength=\"255\""));
    }

    /** A7, BR-009: a change without the token of the system's own form is refused, the event is unchanged. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void change_without_form_token_is_refused() {
        long id = EventFixture.createEvent("CLEANUP A7 edit");
        Map<String, String> entry = validEntry("CLEANUP A7 edit changed");
        entry.put("id", Long.toString(id));

        submit(Csrf.givenWithoutToken(), entry).then().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(((Event) Event.findById((int) id)).title, equalTo("CLEANUP A7 edit"));
    }

    /**
     * A6: the store refuses the new event. Nothing is recorded, and the form comes back with the
     * entered values instead of an error page.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void new_event_that_cannot_be_stored_returns_the_form() {
        createFailureTrigger("uc002_a6_insert", "INSERT", FAILING_TITLE);
        long eventCount = Event.count();
        Map<String, String> entry = validEntry(FAILING_TITLE);

        String html = submit(Csrf.given(), entry).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertThat(html, containsString("Neuen Anlass anlegen"));
        assertValuesKept(html, entry, null);
        assertThat(Event.count(), equalTo(eventCount));
    }

    /**
     * A6, A1: the store refuses the change when it is committed. The previous state is preserved,
     * and the form comes back with the entered values instead of an error page.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void change_that_cannot_be_stored_keeps_the_previous_state() {
        long id = EventFixture.createEvent("CLEANUP A6 edit original");
        createFailureTrigger("uc002_a6_update", "UPDATE", FAILING_EDIT_TITLE);
        Map<String, String> entry = validEntry(FAILING_EDIT_TITLE);
        entry.put("id", Long.toString(id));

        String html = submit(Csrf.given(), entry).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertThat(html, containsString("Anlass bearbeiten"));
        assertValuesKept(html, entry, null);
        assertThat(((Event) Event.findById((int) id)).title, equalTo("CLEANUP A6 edit original"));
    }

    private void createFailureTrigger(String name, String operation, String title) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "CREATE TRIGGER " + name + " BEFORE " + operation + " ON events WHEN NEW.title = '"
                        + title + "' BEGIN SELECT RAISE(ABORT, 'UC-002 A6 test'); END").executeUpdate());
    }

    private static Map<String, String> validEntry(String title) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", "0");
        entry.put("title", title);
        entry.put("slug", "");
        entry.put("date", "Sommer 2099");
        entry.put("displayDate", "2099-07-01");
        entry.put("location", "Genf");
        entry.put("description", "Beschreibung");
        return entry;
    }

    private io.restassured.response.Response submit(RequestSpecification request, Map<String, String> entry) {
        return request.redirects().follow(false)
                .formParams(entry)
                .when()
                .post(eventEndpoint);
    }

    private static void assertFieldMarked(String html, String field) {
        Pattern marked = Pattern.compile("id=\"" + field + "\"[^>]*aria-invalid=\"true\"");
        assertThat("field '" + field + "' is marked for correction", marked.matcher(html).find(), is(true));
    }

    /** A2, A6: the values the administrator typed come back in the form, except the one left out. */
    private static void assertValuesKept(String html, Map<String, String> entry, String except) {
        for (String field : new String[]{"title", "date", "location"}) {
            if (!field.equals(except)) {
                assertThat(html, containsString("value=\"" + entry.get(field) + "\""));
            }
        }
    }

    private static String fieldOf(Event event, String field) {
        return switch (field) {
            case "slug" -> event.slug;
            case "date" -> event.date;
            case "location" -> event.location;
            case "description" -> event.description;
            default -> throw new IllegalArgumentException(field);
        };
    }
}
