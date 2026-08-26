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

@QuarkusTest
public class IndexResourceTest {
    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource
    URL indexEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("fgi")
    URL fgiEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("version")
    URL versionEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("admin")
    URL adminEndpoint;

    @Test
    public void fgi_page() {
        given().when().get(fgiEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Fachgruppe International"));
    }

    /**
     * The collected sources are grouped by channel, and each group is a native disclosure so that
     * one link can be found without scrolling past fifty (FR-039, BR-025).
     */
    @Test
    public void fgi_page_groups_the_sources_by_channel() {
        String body = fetch(fgiEndpoint);

        assertThat(body, containsString("id=\"instagram\""));
        assertThat(body, containsString("id=\"newsletter\""));
        assertThat(body, containsString("<details class=\"channel\""));
        // The index at the top is what makes the grouping usable on a phone
        assertThat(body, containsString("href=\"#webseiten\""));
    }

    /**
     * The start page answers instead of forwarding (BR-047, FR-023 as amended, UC-008). Until
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
     * so this is a requirement, not a nicety (BR-049).
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

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void version_working() {
        given().when().get(versionEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                // The page names both the application and the schema version (BR-027). The two
                // German labels are content, not markup, so this survives the page being relaid
                // out — which the previous assertion on the literal "Version:" did not.
                .body(containsString("Anwendung"))
                .body(containsString("Datenbankschema"));
    }

    /** The version page names application and schema version and is therefore not public (BR-039). */
    @Test
    public void version_requires_authentication() {
        given()
                .redirects().follow(false)
                .when()
                .get(versionEndpoint)
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }

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
