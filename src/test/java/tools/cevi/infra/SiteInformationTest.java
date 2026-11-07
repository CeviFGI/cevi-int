package tools.cevi.infra;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import tools.cevi.auth.User;
import tools.cevi.contact.ContactFormEntry;
import tools.cevi.event.Event;
import tools.cevi.voluntary.VoluntaryService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * UC-007 View Site Information: the presentation of the working group, the data protection
 * information, the version page for operators, and the pages a visitor sees instead of the
 * requested content (unknown address, unexpected failure).
 */
@QuarkusTest
public class SiteInformationTest {

    /** The channels BR-004 names, by the id of their group on the page. */
    private static final List<String> CHANNEL_GROUPS =
            List.of("blogs", "instagram", "facebook", "webseiten", "newsletter", "fotoalben");

    /**
     * Main success scenario, steps 1–2, and BR-001: both information pages are offered in the
     * navigation of every page, not only on an entry page.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/", "/anlaesse", "/volontariat", "/volontariat/detail?slug=horyzon-kolumbien-palastina", "/kontakt", "/fgi", "/datenschutzinformation"})
    void the_navigation_of_every_page_offers_both_information_pages(String path) {
        String navigation = navigationOf(fetch(path));

        assertThat(path + " should offer the working group in its navigation",
                navigation, containsString("href=\"/fgi\""));
        assertThat(path + " should offer the data protection information in its navigation",
                navigation, containsString("href=\"/datenschutzinformation\""));
    }

    /** Main success scenario, step 2: the presentation of the working group says who runs the platform. */
    @Test
    void the_working_group_presents_itself() {
        String body = fetch("/fgi");

        assertThat(body, containsString("<h1>FGI — Fachgruppe International</h1>"));
        assertThat(body, containsString("betreibt diese Seite"));
    }

    /**
     * Main success scenario, step 3, and BR-004: the external sources are grouped by channel, every
     * group is open on arrival and collapsible by the visitor without a script (a native
     * {@code <details>}), and an index leads to each group.
     */
    @Test
    void the_sources_are_grouped_by_channel_open_and_collapsible() {
        String body = fetch("/fgi");

        for (String group : CHANNEL_GROUPS) {
            assertThat("group " + group + " should be an open disclosure",
                    body, containsString("<details class=\"channel\" id=\"" + group + "\" open>"));
            assertThat("the index should lead to group " + group,
                    body, containsString("href=\"#" + group + "\""));
        }
    }

    /**
     * Main success scenario, step 2, and BR-002: the data protection information is presented
     * without signing in and says how the visitor's data is handled.
     */
    @Test
    void the_data_protection_information_is_public_and_explains_the_data_handling() {
        String body = given().redirects().follow(false)
                .when().get("/datenschutzinformation")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertThat(body, containsString("<h1>Datenschutzinformation</h1>"));
        assertThat(body, containsString("Bearbeitung von Personendaten"));
        assertThat(body, containsString("Beantworten von Kontaktanfragen"));
    }

    /**
     * A1 and BR-003: with an administrator session the version page names the application version
     * and the version of the database schema in use — the newest migration Flyway recorded as
     * applied.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_version_page_names_application_and_schema_version() {
        String applicationVersion = ConfigProvider.getConfig()
                .getValue("quarkus.application.version", String.class);
        String schemaVersion = appliedSchemaVersion();

        String body = fetch("/version");

        assertThat(body, containsString("<dt>Anwendung</dt>\n    <dd>" + applicationVersion + "</dd>"));
        assertThat(body, containsString("<dt>Datenbankschema (Flyway)</dt>\n    <dd>" + schemaVersion + "</dd>"));
    }

    /** A4 and BR-005: without an administrator session the version page leads to the sign-in page. */
    @Test
    void the_version_page_leads_to_sign_in_without_a_session() {
        String applicationVersion = ConfigProvider.getConfig()
                .getValue("quarkus.application.version", String.class);

        String body = given().redirects().follow(false)
                .when().get("/version")
                .then().statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"))
                .extract().body().asString();

        assertThat(body, not(containsString(applicationVersion)));
    }

    /**
     * A2: an unknown address presents a page that says the content does not exist and offers the way
     * back to the site.
     */
    @Test
    void an_unknown_address_explains_itself_and_offers_the_way_back() {
        String body = given().when().get("/gibt-es-nicht")
                .then().statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().body().asString();

        assertThat(body, containsString("<h1>Diese Seite gibt es nicht</h1>"));
        // Anchored on the page's own button: header and footer link /anlaesse on every page.
        assertThat(body, containsString("href=\"/anlaesse\">Zu den Anlässen</a>"));
    }

    /** A3: a page the system cannot produce is answered by a general error page without technical internals. */
    @Test
    void an_unexpected_failure_presents_the_general_error_page() {
        String body = given().when().get("/test-only/failing-page")
                .then().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .contentType(containsString("text/html"))
                .extract().body().asString();

        assertThat(body, containsString("<h1>Da ist etwas schiefgelaufen</h1>"));
        assertThat(body, not(containsString(FailingPageResource.INTERNAL_DETAIL)));
        assertThat(body, not(containsString("IllegalStateException")));
    }

    /**
     * Success and failure postconditions: no data is changed — neither by the information pages,
     * nor the version page, nor the pages shown instead of the requested content.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void no_data_is_changed() {
        List<Long> before = recordCounts();

        fetch("/fgi");
        fetch("/datenschutzinformation");
        fetch("/version");
        given().when().get("/gibt-es-nicht").then().statusCode(HttpStatus.SC_NOT_FOUND);
        given().when().get("/test-only/failing-page").then().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        assertThat(recordCounts(), is(before));
    }

    private static List<Long> recordCounts() {
        return QuarkusTransaction.requiringNew().call(() -> List.of(
                Event.count(), VoluntaryService.count(), ContactFormEntry.count(), User.count()));
    }

    private static String appliedSchemaVersion() {
        return QuarkusTransaction.requiringNew().call(() -> (String) Panache.getEntityManager()
                .createNativeQuery("select version from flyway_schema_history"
                        + " where success = 1 and version is not null"
                        + " order by installed_rank desc limit 1")
                .getSingleResult());
    }

    private static String navigationOf(String body) {
        int start = body.indexOf("aria-label=\"Hauptnavigation\"");
        assertThat("the page should carry the main navigation", start >= 0, is(true));
        return body.substring(start, body.indexOf("</nav>", start));
    }

    private static String fetch(String path) {
        return given().when().get(path).then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }
}
