package tools.cevi.infra;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;

import tools.cevi.event.Event;
import tools.cevi.voluntary.VoluntaryService;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

/**
 * The routes of {@link IndexResource} that belong to UC-008 (start page) and UC-006 (administration
 * entry). The information pages and the version page of UC-007 are tested in {@link SiteInformationTest}.
 */
@QuarkusTest
public class IndexResourceTest {
    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource
    URL indexEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("admin")
    URL adminEndpoint;

    /**
     * The start page answers instead of forwarding (UC-008 BR-001, FR-023 as amended, UC-008). Until
     * requirements revision 5 this asserted the 303 to /anlaesse.
     */
    @Test
    public void index_is_a_page_of_its_own() {
        String body = given().redirects().follow(false)
                .when().get(indexEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertThat(body, containsString("Rein in die Welt."));
        assertThat(body, containsString("Die nächsten Anlässe"));
        assertThat(body, containsString("Länger bleiben"));
    }

    /** The lists keep their own addresses, so every link shared before the change still works. */
    @Test
    public void the_complete_lists_stay_reachable_under_their_own_addresses() {
        given().when().get("/anlaesse").then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Anlässe"));
        given().when().get("/volontariat").then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat"));
    }

    /**
     * A1 and A2 of UC-008: where an extract has nothing to show, the section says so and offers a
     * way onward. An empty area on the first screen is what tells a visitor a site is abandoned,
     * so this is a requirement, not a nicety (UC-008 BR-003).
     *
     * <p>The demo data is removed and put back around the assertion rather than worked around: the
     * branch under test is exactly "the database holds nothing".
     */
    @Test
    public void an_empty_database_renders_the_empty_state_rather_than_an_empty_grid() {
        List<Event> events = Event.listAll();
        List<VoluntaryService> services = VoluntaryService.listAll();
        try {
            QuarkusTransaction.begin();
            Event.deleteAll();
            VoluntaryService.deleteAll();
            QuarkusTransaction.commit();

            String body = fetch(indexEndpoint);

            assertThat(body, containsString("Gerade ist nichts ausgeschrieben"));
            assertThat(body, containsString("Noch kein Angebot erfasst"));
        } finally {
            QuarkusTransaction.begin();
            events.forEach(IndexResourceTest::restore);
            services.forEach(IndexResourceTest::restore);
            QuarkusTransaction.commit();
        }
    }

    private static void restore(Event source) {
        Event copy = new Event();
        copy.title = source.title;
        copy.slug = source.slug;
        copy.date = source.date;
        copy.location = source.location;
        copy.displayDate = source.displayDate;
        copy.description = source.description;
        copy.persist();
    }

    private static void restore(VoluntaryService source) {
        VoluntaryService copy = new VoluntaryService();
        copy.slug = source.slug;
        copy.organization = source.organization;
        copy.organizationLink = source.organizationLink;
        copy.location = source.location;
        copy.description = source.description;
        copy.persist();
    }

    private static String fetch(URL endpoint) {
        return given().when().get(endpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    /** UC-006 BR-010, A5: without a session the general administration entry leads to the sign-in page. */
    @Test
    public void admin_redirect_to_login() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .when()
                .get(adminEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/auth/login"));
    }

    /** UC-006 BR-010, A2: with a session the general administration entry leads to the start page. */
    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void admin_redirect_to_home_if_logged_in() {
        given().contentType(ContentType.URLENC)
                .redirects().follow(false)
                .cookie("quarkus-credential")
                .when()
                .get(adminEndpoint)
                .then()
                .statusCode(HttpStatus.SC_SEE_OTHER)
                .header("location", is("http://localhost:8081/"));
    }
}
