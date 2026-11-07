package tools.cevi.voluntary;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.VoluntaryFixture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.StringContains.containsString;

/**
 * UC-003 Browse Voluntary Service Offers and UC-004 Manage Voluntary Service Offers over HTTP, plus
 * UC-006 BR-003: the maintenance links on the offer list appear only for a signed-in administrator.
 */
@QuarkusTest
public class VoluntaryResourceTest {
    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource
    URL voluntaryEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

    /**
     * FR-010 and UC-003 BR-003. The field was captured and validated from the beginning, but no template
     * ever rendered it, so an offer told the visitor an opportunity existed and not where to
     * pursue it. This is the regression test that was missing while the requirement stood at
     * "Verified".
     */
    @Test
    public void an_offer_leads_to_the_organisation_running_it() {
        String organization = "CLEANUP offer_links_to_organisation";
        String link = "https://ymca-spitak.example.org/volunteering";
        VoluntaryFixture.createVoluntaryService(organization, "Sechs Monate im Jugendzentrum.", link);

        given()
                .when()
                .get(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(link))
                // A new tab must not be able to reach back into this page through window.opener.
                .body(containsString("rel=\"noopener noreferrer\""));
    }

    /** UC-003 BR-004: the overview compares offers; the whole text is opened in place, not elsewhere. */
    @Test
    public void a_long_offer_is_shortened_but_its_full_text_stays_reachable() {
        String organization = "CLEANUP offer_is_shortened";
        String tail = "SCHLUSSDESANGEBOTS";
        String description = "<p>" + "Wort ".repeat(60) + tail + "</p>";
        VoluntaryFixture.createVoluntaryService(organization, description, "https://example.org/x");

        given()
                .when()
                .get(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Ganzen Text lesen"))
                .body(containsString(tail));
    }

    /** UC-006 BR-003: a visitor sees the offers but no maintenance links. */
    @Test
    public void list_no_auth() {
        List<VoluntaryService> voluntaryServices = VoluntaryService.listAll();
        assertThat(voluntaryServices, is(not(empty())));
        given()
                .when()
                .get(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(voluntaryServices.get(0).organization))
                .body(not(containsString("Neues Volontariat eintragen<")))
                .body(not(containsString("Bearbeiten")));
    }

    /** UC-006 BR-003, main success scenario step 7: a signed-in administrator sees the maintenance links. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void list_with_auth() {
        List<VoluntaryService> voluntaryServices = VoluntaryService.listAll();
        assertThat(voluntaryServices, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(voluntaryServices.get(0).organization))
                .body(containsString("Neues Volontariat eintragen"))
                .body(containsString("Bearbeiten"));
    }

    /** UC-004 BR-001, Pre-1: a visitor who is not signed in is sent to the sign-in page instead of the empty form. */
    @Test
    public void add_form_no_auth() {
        given()
                .redirects()
                .follow(false)
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }

    /** UC-004 main success scenario steps 1 and 2: the administrator gets an empty offer form. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_form_with_auth() {
        given()
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Neues Volontariat eintragen"));
    }

    /** UC-004 BR-001, Pre-1 (NFR-006): adding an offer without a session leads to the sign-in page. */
    @Test
    public void add_submit_no_auth() {
        Csrf.given().formParam("organization", "test org vol")
                .formParam("organizationLink", "http://test.ch")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"))
                .cookie("quarkus-redirect-location", containsString(voluntaryEndpoint.toString()));
    }

    /** UC-004 main success scenario: a complete entry is recorded and the Offer List presented. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_submit_with_auth() {
        String organization = "CLEANUP add_submit_with_auth";
        VoluntaryFixture.createVoluntaryServiceWithRest(voluntaryEndpoint, organization);

        List<VoluntaryService> voluntaryService = VoluntaryService.listAll();
        assertThat(voluntaryService, is(not(empty())));
        assertThat(voluntaryService.get(voluntaryService.size()-1).organization, equalTo(organization));
    }

    /** UC-004 BR-001, Pre-1: a visitor who is not signed in gets no edit form. */
    @Test
    public void edit_form_no_auth() {
        String title = "CLEANUP edit_form_no_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        given()
                .redirects()
                .follow(false)
                .queryParam("id", id)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }

    /** UC-004 A1 step 1: the administrator gets the filled form of an existing offer. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_form_with_auth() {
        String title = "CLEANUP edit_form_with_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        given()
                .queryParam("id", id)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat bearbeiten"));
    }

    /** UC-004 BR-001, Pre-1 (NFR-006): changing an offer without a session leads to the sign-in page. */
    @Test
    public void edit_submit_no_auth() {
        String title = "CLEANUP edit_submit_no_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        Csrf.given()
                .formParam("id", id)
                .formParam("organization", "test title")
                .formParam("organizationLink", "https://example.org")
                .formParam("location", "hier")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }

    /** UC-004 A1: the changed offer is recorded and no further offer is added. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_submit_with_auth() {
        String title = "CLEANUP edit_submit_with_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        long voluntaryServiceCount = VoluntaryService.count();
        var uuid = UUID.randomUUID();

        Csrf.given()
                .formParam("id", id)
                .formParam("organization", "organization_" + uuid)
                .formParam("organizationLink", "https://example.org")
                .formParam("location", "hier")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));

        QuarkusTransaction.begin();
        assertThat(((VoluntaryService) VoluntaryService.findById(id)).organization, equalTo("organization_" + uuid));
        QuarkusTransaction.rollback();
    }

    /** UC-004 BR-001, Pre-1: a visitor who is not signed in gets no Confirmation Page. */
    @Test
    public void delete_no_auth() {
        String title = "CLEANUP delete_no_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        given()
                .redirects()
                .follow(false)
                .queryParam("id", id)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }

    /** UC-004 A3 step 1, BR-003: the removal is first asked for on a Confirmation Page. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_with_auth() {
        String title = "CLEANUP delete_with_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        given()
                .queryParam("id", id)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat löschen"));
    }

    /** UC-004 A3: the confirmed removal removes the offer. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_confirmed_with_auth() {
        String title = "CLEANUP delete_confirmed_with_auth";
        long id = VoluntaryFixture.createVoluntaryService(title);

        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.given()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat"));

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount-1));
    }

    /**
     * UC-004 A4, BR-003: a GET must not delete: a prefetching browser, a link preview or a plain link from another
     * site would otherwise be enough to remove an offer (UC-002 BR-009, NFR-021).
     */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void get_with_confirmed_parameter_does_not_delete() {
        String title = "CLEANUP get_with_confirmed_parameter_does_not_delete";
        long id = VoluntaryFixture.createVoluntaryService(title);

        long voluntaryServiceCount = VoluntaryService.count();

        given()
                .queryParam("id", id)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat löschen"));

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
    }

    /** UC-004 A7: without a CSRF token the change is refused before it reaches the endpoint (NFR-011). */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_submit_without_csrf_token_is_rejected() {
        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.givenWithoutToken()
                .formParam("organization", "CLEANUP add_submit_without_csrf_token_is_rejected")
                .formParam("organizationLink", "https://example.org")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
    }

    /** UC-004 A2, BR-005: the Organisation Link has to be a web address so it can never become a script link. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_submit_rejects_non_web_organization_link() {
        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.given()
                .formParam("organization", "CLEANUP add_submit_rejects_non_web_organization_link")
                .formParam("organizationLink", "javascript:alert(1)")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(containsString("muss mit http:// oder https:// beginnen"));

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
    }

    /** UC-004 BR-001, Pre-2 (NFR-006): a signed-in user without the administrator role cannot add or change an offer. */
    @Test
    @TestSecurity(user = "visitor", roles = { "user"})
    public void submit_without_administrator_role_is_refused() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-004 role check");
        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.given()
                .formParam("organization", "CLEANUP UC-004 role check added")
                .formParam("organizationLink", "https://example.org")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        Csrf.given()
                .formParam("id", id)
                .formParam("organization", "CLEANUP UC-004 role check changed")
                .formParam("organizationLink", "https://example.org")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
        assertThat(QuarkusTransaction.requiringNew().call(() -> VoluntaryService.<VoluntaryService>findById((int) id).organization),
                equalTo("CLEANUP UC-004 role check"));
    }

    /** UC-004 BR-001, Pre-2 (NFR-006): a signed-in user without the administrator role cannot remove an offer. */
    @Test
    @TestSecurity(user = "visitor", roles = { "user"})
    public void confirm_delete_without_administrator_role_is_refused() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-004 role check delete");
        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.given()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
    }

    /** UC-004 BR-001, Pre-1 (NFR-006): confirming a removal without a session leads to the sign-in page. */
    @Test
    public void confirm_delete_without_session_is_refused() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-004 anonymous delete");
        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.given()
                .formParam("id", id)
                .redirects().follow(false)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
    }

    /** UC-004 A7: a change or removal without a CSRF token is refused and stores nothing (NFR-011). */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void change_and_removal_without_csrf_token_are_rejected() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-004 csrf");
        long voluntaryServiceCount = VoluntaryService.count();

        Csrf.givenWithoutToken()
                .formParam("id", id)
                .formParam("organization", "CLEANUP UC-004 csrf changed")
                .formParam("organizationLink", "https://example.org")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        Csrf.givenWithoutToken()
                .formParam("id", id)
                .when()
                .post(deleteEndpoint)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));
        assertThat(QuarkusTransaction.requiringNew().call(() -> VoluntaryService.<VoluntaryService>findById((int) id).organization),
                equalTo("CLEANUP UC-004 csrf"));
    }
}
