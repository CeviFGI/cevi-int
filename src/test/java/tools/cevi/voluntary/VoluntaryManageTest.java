package tools.cevi.voluntary;

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
import org.junit.jupiter.params.provider.ValueSource;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.EventFixture;
import tools.cevi.infra.Slug;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-004 Manage Voluntary Service Offers: the Short Name of an offer (BR-006 … BR-009), changing an
 * offer (A1), an entry that is rejected (A2, BR-002, BR-004), an offer that no longer exists (A5), a
 * change or removal the store refuses (A6, A8). Who may maintain offers, the confirmation and the
 * form check stay in {@link VoluntaryResourceTest}.
 */
@QuarkusTest
class VoluntaryManageTest {
    private static final String FAILING_ORGANIZATION = "CLEANUP UC-004 A6 cannot be stored";
    private static final String UNDELETABLE_ORGANIZATION = "CLEANUP UC-004 A8 cannot be removed";

    @Inject
    EntityManager entityManager;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource
    URL voluntaryEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

    @AfterEach
    void cleanUp() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc004_a6_insert").executeUpdate();
            entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc004_a6_update").executeUpdate();
            entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc004_a8_delete").executeUpdate();
            VoluntaryService.delete("organization like ?1", "CLEANUP UC-004 %");
        });
    }

    /**
     * Main success scenario steps 6 and 9, BR-007: without a Short Name the system derives one from
     * the Organisation followed by the location, in the readable form of an event's. Success
     * postcondition: the new offer is at once in the Offer List and on its Offer Page.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_short_name_is_derived_from_organisation_and_location() {
        Map<String, String> entry = entry("CLEANUP UC-004 Jugendhaus Zürich", "");
        entry.put("location", "Bogotá, Kolumbien");

        submit(entry).then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", containsString("/volontariat"));

        String slug = "cleanup-uc-004-jugendhaus-zurich-bogota-kolumbien";
        assertThat(stored(slug), notNullValue());
        offerList().then().statusCode(HttpStatus.SC_OK)
                .body(containsString(">CLEANUP UC-004 Jugendhaus Zürich</a>"));
        offerPage(slug).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("<h1>CLEANUP UC-004 Jugendhaus Zürich</h1>"))
                .body(containsString("Bogotá, Kolumbien"));
    }

    /** BR-007: a derived Short Name another offer already carries gets a counter until it is free. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_derived_short_name_that_is_taken_gets_a_counter() {
        String organization = "CLEANUP UC-004 Doppelt " + UUID.randomUUID();
        submit(entry(organization, "")).then().statusCode(HttpStatus.SC_SEE_OTHER);
        submit(entry(organization, "")).then().statusCode(HttpStatus.SC_SEE_OTHER);
        submit(entry(organization, "")).then().statusCode(HttpStatus.SC_SEE_OTHER);

        String base = Slug.of(organization + " Genf").toString();
        assertThat(stored(base), notNullValue());
        assertThat(stored(base + "0"), notNullValue());
        assertThat(stored(base + "1"), notNullValue());
    }

    /**
     * BR-007, BR-004: a derived Short Name is cut to 252 characters, so that a counter still fits
     * into the 255 characters of the field.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_derived_short_name_is_cut_to_252_characters() {
        String organization = "CLEANUP UC-004 " + "a".repeat(240);
        Map<String, String> entry = entry(organization, "");
        entry.put("location", "b".repeat(30));

        submit(entry).then().statusCode(HttpStatus.SC_SEE_OTHER);
        submit(entry).then().statusCode(HttpStatus.SC_SEE_OTHER);

        String base = ("cleanup-uc-004-" + "a".repeat(240) + "-" + "b".repeat(30)).substring(0, 252);
        assertThat(stored(base), notNullValue());
        assertThat(stored(base + "0"), notNullValue());
    }

    /**
     * Main success scenario step 7, BR-009: a Short Name the administrator enters is stored as
     * entered, not brought into the derived form.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_entered_short_name_is_taken_as_entered() {
        String slug = "Mein Volontariat " + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 entered short name", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);

        assertThat(stored(slug), notNullValue());
    }

    /**
     * BR-009, BR-007: a Short Name of nothing but spaces counts as none — it is derived, as for an
     * event, rather than rejected or stored as spaces.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_short_name_of_only_spaces_is_derived() {
        String organization = "CLEANUP UC-004 Leerzeichen " + UUID.randomUUID();
        submit(entry(organization, "   ")).then().statusCode(HttpStatus.SC_SEE_OTHER);

        assertThat(stored(Slug.of(organization + " Genf").toString()), notNullValue());
    }

    /**
     * A2, BR-006: a Short Name another offer carries is refused; the typed values come back and
     * the Short Name field is marked.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_entered_short_name_of_another_offer_is_rejected() {
        String slug = "cleanup-uc-004-vergeben-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 first owner", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        long count = VoluntaryService.count();

        String html = submit(entry("CLEANUP UC-004 second owner", slug)).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertThat(html, containsString("Es existiert bereits ein anderes Volontariat mit demselben Slug"));
        assertFieldMarked(html, "slug");
        assertThat(html, containsString("value=\"CLEANUP UC-004 second owner\""));
        assertThat(html, containsString("value=\"" + slug + "\""));
        assertThat(VoluntaryService.count(), equalTo(count));
    }

    /**
     * BR-006: an offer may carry the Short Name of an event — Offer Pages and Event Pages have
     * addresses of their own.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_offer_may_carry_the_short_name_of_an_event() {
        String title = "CLEANUP UC-004 Wie ein Anlass " + UUID.randomUUID();
        EventFixture.createEvent(title);
        String slug = Slug.of(title).toString();

        submit(entry("CLEANUP UC-004 same as event", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);

        assertThat(stored(slug), notNullValue());
    }

    /**
     * A2, BR-002: a missing mandatory value is refused; the field is marked and nothing is stored.
     * The Short Name is not among them: left empty it is derived (BR-007).
     */
    @ParameterizedTest
    @ValueSource(strings = {"organization", "organizationLink", "location", "description"})
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_missing_mandatory_value_is_rejected(String field) {
        Map<String, String> entry = entry("CLEANUP UC-004 missing " + field, "");
        entry.put(field, "");
        long count = VoluntaryService.count();

        String html = submit(entry).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract().asString();

        assertThat(html, containsString("Das Volontariat wurde nicht gespeichert."));
        assertFieldMarked(html, field);
        assertThat(VoluntaryService.count(), equalTo(count));
    }

    /** A2, BR-004: Organisation, Organisation Link, location and Short Name hold 255 characters at most. */
    @ParameterizedTest
    @ValueSource(strings = {"organization", "organizationLink", "location", "slug"})
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_value_longer_than_255_characters_is_rejected(String field) {
        Map<String, String> entry = entry("CLEANUP UC-004 too long " + field, "");
        entry.put(field, field.equals("organizationLink")
                ? "https://example.org/" + "x".repeat(236)
                : "CLEANUP UC-004 " + "x".repeat(241));
        long count = VoluntaryService.count();

        String html = submit(entry).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract().asString();

        assertFieldMarked(html, field);
        assertThat(VoluntaryService.count(), equalTo(count));
    }

    /** A2, BR-004: the Description holds 65535 characters at most. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_description_longer_than_65535_characters_is_rejected() {
        Map<String, String> entry = entry("CLEANUP UC-004 description too long", "");
        entry.put("description", "x".repeat(65536));
        long count = VoluntaryService.count();

        String html = submit(entry).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract().asString();

        assertFieldMarked(html, "description");
        assertThat(VoluntaryService.count(), equalTo(count));
    }

    /** A1 step 1: the edit form shows the current values of the offer, its Short Name included. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_edit_form_shows_the_short_name() {
        String slug = "cleanup-uc-004-im-formular-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 edit form", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);

        given().queryParam("id", stored(slug).id)
                .when().get(editEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat bearbeiten"))
                .body(containsString("value=\"" + slug + "\""))
                .body(containsString("value=\"CLEANUP UC-004 edit form\""));
    }

    /**
     * A1, BR-008, success postcondition: changing the Organisation and the location leaves the
     * Short Name — and with it the Public Address visitors have passed on — as it was, and the
     * Offer Page under that address shows the change at once.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void changing_organisation_and_location_keeps_the_short_name() {
        String slug = "cleanup-uc-004-bleibt-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 before", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(slug).id;

        Map<String, String> change = entry("CLEANUP UC-004 after", slug);
        change.put("id", Integer.toString(id));
        change.put("location", "Anderswo");
        submit(change).then().statusCode(HttpStatus.SC_SEE_OTHER);

        VoluntaryService changed = stored(slug);
        assertThat(changed.id, equalTo(id));
        assertThat(changed.organization, equalTo("CLEANUP UC-004 after"));
        assertThat(changed.location, equalTo("Anderswo"));
        offerPage(slug).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("<h1>CLEANUP UC-004 after</h1>"))
                .body(containsString("Anderswo"));
        offerList().then().statusCode(HttpStatus.SC_OK)
                .body(containsString(">CLEANUP UC-004 after</a>"))
                .body(not(containsString(">CLEANUP UC-004 before</a>")));
    }

    /**
     * A1, BR-008: an administrator who edits the Short Name changes it; the old one is free again
     * and its address no longer leads to the offer.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_edited_short_name_replaces_the_previous_one() {
        String before = "cleanup-uc-004-alt-" + UUID.randomUUID();
        String after = "cleanup-uc-004-neu-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 renamed", before)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(before).id;

        Map<String, String> change = entry("CLEANUP UC-004 renamed", after);
        change.put("id", Integer.toString(id));
        submit(change).then().statusCode(HttpStatus.SC_SEE_OTHER);

        assertThat(stored(before), nullValue());
        assertThat(stored(after).id, equalTo(id));
        offerPage(before).then().statusCode(HttpStatus.SC_NOT_FOUND);
        offerPage(after).then().statusCode(HttpStatus.SC_OK).body(containsString("<h1>CLEANUP UC-004 renamed</h1>"));
    }

    /**
     * A1, BR-008, BR-007: a Short Name emptied while editing is derived anew from the current
     * Organisation and location.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_emptied_short_name_is_derived_anew() {
        String before = "cleanup-uc-004-geleert-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 before emptying", before)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(before).id;

        String organization = "CLEANUP UC-004 Neu abgeleitet " + UUID.randomUUID();
        Map<String, String> change = entry(organization, "");
        change.put("id", Integer.toString(id));
        submit(change).then().statusCode(HttpStatus.SC_SEE_OTHER);

        assertThat(stored(before), nullValue());
        String derived = Slug.of(organization + " Genf").toString();
        assertThat(stored(derived).id, equalTo(id));
        offerPage(before).then().statusCode(HttpStatus.SC_NOT_FOUND);
        offerPage(derived).then().statusCode(HttpStatus.SC_OK);
    }

    /**
     * A1, BR-007: an emptied Short Name may be derived to the one the offer already has — its own
     * name does not count as taken, so no counter is appended.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_emptied_short_name_may_come_back_unchanged() {
        String organization = "CLEANUP UC-004 Eigener Name " + UUID.randomUUID();
        String derived = Slug.of(organization + " Genf").toString();
        submit(entry(organization, "")).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(derived).id;

        Map<String, String> change = entry(organization, "");
        change.put("id", Integer.toString(id));
        submit(change).then().statusCode(HttpStatus.SC_SEE_OTHER);

        assertThat(stored(derived).id, equalTo(id));
    }

    /** A1, A2, BR-006: changing an offer to the Short Name of another is refused; both stay as they were. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void changing_to_the_short_name_of_another_offer_is_rejected() {
        String taken = "cleanup-uc-004-besetzt-" + UUID.randomUUID();
        String own = "cleanup-uc-004-eigen-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 owner", taken)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        submit(entry("CLEANUP UC-004 contender", own)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(own).id;

        Map<String, String> change = entry("CLEANUP UC-004 contender", taken);
        change.put("id", Integer.toString(id));
        String html = submit(change).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract().asString();

        assertThat(html, containsString("Volontariat bearbeiten"));
        assertFieldMarked(html, "slug");
        assertThat(stored(own).id, equalTo(id));
        assertThat(stored(taken).organization, equalTo("CLEANUP UC-004 owner"));
    }

    /** A5: the edit page of an offer that no longer exists says so. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_edit_page_of_a_removed_offer_does_not_exist() {
        given().queryParam("id", 10000000)
                .when().get(editEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));
    }

    /** A5: the delete page of an offer that no longer exists says so. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_delete_page_of_a_removed_offer_does_not_exist() {
        given().queryParam("id", 10000000)
                .when().get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));
    }

    /** A5: a change submitted for an offer removed after the form was opened stores nothing. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_change_to_a_removed_offer_stores_nothing() {
        long count = VoluntaryService.count();
        Map<String, String> change = entry("CLEANUP UC-004 removed meanwhile", "");
        change.put("id", "10000000");

        submit(change).then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));

        assertThat(VoluntaryService.count(), equalTo(count));
    }

    /** A5: confirming the removal of an offer that no longer exists says so. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void confirming_the_removal_of_a_removed_offer_says_so() {
        Csrf.given().formParam("id", 10000000)
                .when().post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));
    }

    /**
     * A6: the store refuses a new offer. Nothing is stored and the form comes back with the entered
     * values instead of an error page.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_offer_that_cannot_be_stored_brings_the_form_back() {
        createFailureTrigger("uc004_a6_insert", "INSERT", "NEW", FAILING_ORGANIZATION);
        long count = VoluntaryService.count();

        String html = submit(entry(FAILING_ORGANIZATION, "")).then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertThat(html, containsString("Neues Volontariat eintragen"));
        assertThat(html, containsString("value=\"" + FAILING_ORGANIZATION + "\""));
        assertThat(VoluntaryService.count(), equalTo(count));
    }

    /** A6, A1: the store refuses a change; the previous state is preserved and the form comes back. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_change_that_cannot_be_stored_keeps_the_previous_state() {
        String slug = "cleanup-uc-004-a6-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 A6 original", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(slug).id;
        createFailureTrigger("uc004_a6_update", "UPDATE", "NEW", FAILING_ORGANIZATION);

        Map<String, String> change = entry(FAILING_ORGANIZATION, slug);
        change.put("id", Integer.toString(id));
        String html = submit(change).then().statusCode(HttpStatus.SC_BAD_REQUEST).extract().asString();

        assertThat(html, containsString("Volontariat bearbeiten"));
        assertThat(html, containsString("value=\"" + FAILING_ORGANIZATION + "\""));
        assertThat(stored(slug).organization, equalTo("CLEANUP UC-004 A6 original"));
    }

    /**
     * A8: the store refuses the confirmed removal. The offer is preserved and the Confirmation Page
     * comes back with a notice.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_removal_that_cannot_be_completed_keeps_the_offer() {
        String slug = "cleanup-uc-004-a8-" + UUID.randomUUID();
        submit(entry(UNDELETABLE_ORGANIZATION, slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);
        int id = stored(slug).id;
        createFailureTrigger("uc004_a8_delete", "DELETE", "OLD", UNDELETABLE_ORGANIZATION);

        String html = Csrf.given().formParam("id", id)
                .when().post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .extract().asString();

        assertThat(html, containsString("Volontariat löschen"));
        assertThat(html, containsString("Das Volontariat konnte nicht gelöscht werden"));
        assertThat(html, containsString(UNDELETABLE_ORGANIZATION));
        assertThat(stored(slug), notNullValue());
    }

    /**
     * A3 step 4, success postcondition: a confirmed removal leads back to the updated Offer List;
     * the offer is gone from it and its Offer Page no longer exists.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_confirmed_removal_leads_to_the_offer_list() {
        String slug = "cleanup-uc-004-weg-" + UUID.randomUUID();
        submit(entry("CLEANUP UC-004 removed", slug)).then().statusCode(HttpStatus.SC_SEE_OTHER);

        Csrf.given().formParam("id", stored(slug).id)
                .redirects().follow(false)
                .when().post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", containsString("/volontariat"));

        assertThat(stored(slug), nullValue());
        offerList().then().statusCode(HttpStatus.SC_OK)
                .body(not(containsString(">CLEANUP UC-004 removed</a>")));
        offerPage(slug).then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Main success scenario step 8: the system reduces the Description to the permitted formatting
     * before it records the offer — script, event handlers and script addresses do not reach the
     * store, the formatted text does.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_description_is_reduced_to_the_permitted_formatting() {
        String slug = "cleanup-uc-004-bereinigt-" + UUID.randomUUID();
        Map<String, String> entry = entry("CLEANUP UC-004 sanitised", slug);
        entry.put("description", "<p onclick=\"steal()\">Sechs <strong>Monate</strong></p>"
                + "<script>steal()</script><a href=\"javascript:steal()\">Link</a>");

        submit(entry).then().statusCode(HttpStatus.SC_SEE_OTHER);

        String description = stored(slug).description;
        assertThat(description, containsString("Sechs <strong>Monate</strong>"));
        assertThat(description, not(containsString("<script")));
        assertThat(description, not(containsString("onclick")));
        assertThat(description, not(containsString("javascript:")));
        assertThat(description, not(containsString("steal()")));
    }

    private void createFailureTrigger(String name, String operation, String row, String organization) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "CREATE TRIGGER " + name + " BEFORE " + operation + " ON voluntary_services WHEN " + row
                        + ".organization = '" + organization + "' BEGIN SELECT RAISE(ABORT, 'UC-004 test'); END")
                .executeUpdate());
    }

    /** The Offer Page under {@code slug}, as a visitor opens it (UC-003 step 4). */
    private static io.restassured.response.Response offerPage(String slug) {
        return given().queryParam("slug", slug).when().get("/volontariat/detail");
    }

    /** The Offer List, as a visitor opens it (UC-003 step 1). */
    private static io.restassured.response.Response offerList() {
        return given().when().get("/volontariat");
    }

    /** Read in a transaction of its own, so the test never sees a copy its session cached earlier. */
    private static VoluntaryService stored(String slug) {
        return QuarkusTransaction.requiringNew().call(() -> VoluntaryService.findBySlug(slug));
    }

    private static Map<String, String> entry(String organization, String slug) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", "0");
        entry.put("slug", slug);
        entry.put("organization", organization);
        entry.put("organizationLink", "https://example.org/volontariat");
        entry.put("location", "Genf");
        entry.put("description", "<p>Beschreibung</p>");
        return entry;
    }

    private io.restassured.response.Response submit(Map<String, String> entry) {
        RequestSpecification request = Csrf.given();
        return request.redirects().follow(false).formParams(entry).when().post(voluntaryEndpoint);
    }

    private static void assertFieldMarked(String html, String field) {
        Pattern marked = Pattern.compile("id=\"" + field + "\"[^>]*aria-invalid=\"true\"");
        assertThat("field '" + field + "' is marked for correction", marked.matcher(html).find(), is(true));
    }
}
