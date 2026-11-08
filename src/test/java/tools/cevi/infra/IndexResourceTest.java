package tools.cevi.infra;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import tools.cevi.event.Event;
import tools.cevi.fixture.EventFixture;
import tools.cevi.fixture.VoluntaryFixture;
import tools.cevi.voluntary.VoluntaryService;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

/**
 * The routes of {@link IndexResource} that belong to UC-008 Enter the Platform (start page) and UC-006 (administration
 * entry). The information pages and the version page of UC-007 are tested in {@link SiteInformationTest}.
 */
@QuarkusTest
public class IndexResourceTest {
    /** The way to the working group an empty section offers in place of its extract (A1, A2). */
    private static final String WRITE_TO_US = "<a class=\"btn btn--primary\" href=\"/kontakt\">Schreib uns</a>";
    /** The page's own way to the working group (step 5), not the header or footer link. */
    private static final String STEP_5_LINK = "href=\"/kontakt\">Kontakt aufnehmen</a>";

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource
    URL indexEndpoint;

    @TestHTTPEndpoint(IndexResource.class)
    @TestHTTPResource("admin")
    URL adminEndpoint;

    /**
     * Main success scenario steps 1-2, BR-001: the start page answers instead of forwarding. Until
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

    /**
     * BR-002, A3: the complete lists keep their own addresses and are presented directly, so every
     * link shared before the change still works.
     */
    @Test
    public void the_complete_lists_stay_reachable_under_their_own_addresses() {
        given().when().get("/anlaesse").then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Anlässe"));
        given().when().get("/volontariat").then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Volontariat"));
    }

    /**
     * A1, A2, BR-003, failure postcondition: where an extract has nothing to show, the section says
     * so and offers the way to the working group instead; the use case still continues with the
     * offer of step 5, and no data is changed. An empty area on the first screen is what tells a
     * visitor a site is abandoned, so this is a requirement, not a nicety.
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

            String noEvents = emptyBlock(body, "Gerade ist nichts ausgeschrieben");
            String noOffers = emptyBlock(body, "Noch kein Angebot erfasst");
            assertThat(noEvents, containsString(WRITE_TO_US));
            assertThat(noOffers, containsString(WRITE_TO_US));
            assertThat(body.indexOf(STEP_5_LINK), greaterThan(body.indexOf("Noch kein Angebot erfasst")));
            assertThat(Event.count(), is(0L));
            assertThat(VoluntaryService.count(), is(0L));
        } finally {
            QuarkusTransaction.begin();
            events.forEach(IndexResourceTest::restore);
            services.forEach(IndexResourceTest::restore);
            QuarkusTransaction.commit();
        }
    }

    /** The {@code .empty} block that carries {@code heading}. */
    private static String emptyBlock(String page, String heading) {
        int at = page.indexOf(">" + heading + "<");
        assertThat(heading, at, greaterThan(-1));
        int start = page.lastIndexOf("<div class=\"empty\">", at);
        assertThat(heading + " stands in an empty block", start, greaterThan(-1));
        return page.substring(start, page.indexOf("</div>", at));
    }

    /** Main success scenario step 2: the first screen says what the platform is and who runs it. */
    @Test
    public void the_start_page_says_who_runs_the_platform() {
        String body = fetch(indexEndpoint);

        assertThat(body, containsString("<h1 class=\"hero__title\">Raus aus der Schweiz.<br>Rein in die Welt.</h1>"));
        assertThat(body, containsString("gesammelt von der Fachgruppe International des Cevi Schweiz."));
    }

    /**
     * Main success scenario step 5: the start page offers the way to the working group for a visitor
     * who does not yet know what suits them. Anchored on the page's own link, since header and footer
     * link {@code /kontakt} on every page.
     */
    @Test
    public void the_start_page_offers_the_way_to_the_working_group() {
        String body = fetch(indexEndpoint);

        int teaser = body.indexOf("Du weisst noch nicht, was passt?");
        assertThat(teaser, greaterThan(-1));
        assertThat(body.indexOf(STEP_5_LINK, teaser), greaterThan(teaser));
    }

    /**
     * Main success scenario step 3, BR-004: an event still ahead appears in the extract and leads to
     * its Event Page; an event whose display date has passed does not appear.
     */
    @Test
    public void an_upcoming_event_in_the_extract_leads_to_its_event_page() {
        String upcoming = "CLEANUP UC-008 upcoming";
        String past = "CLEANUP UC-008 past";
        long id = EventFixture.createEvent(upcoming, LocalDate.now());
        Event event = QuarkusTransaction.requiringNew().call(() -> Event.<Event>findById((int) id));
        EventFixture.createEvent(past, LocalDate.now().minusDays(1));

        String home = fetch(indexEndpoint);
        String card = card(home, upcoming);

        String href = "/anlaesse/detail?slug=" + event.slug;
        assertThat(card, containsString("<a href=\"" + href + "\">" + upcoming + "</a>"));
        assertThat(home, not(containsString(past)));
        given().urlEncodingEnabled(false).when().get(href).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(upcoming));
    }

    /**
     * BR-004: an offer in the extract leads only to its Offer Page — the start page offers no direct
     * link to the Organisation.
     */
    @Test
    public void an_offer_in_the_extract_does_not_link_to_the_organisation() {
        String organization = "CLEANUP UC-008 no organisation link";
        String link = "https://uc008.example.org/organisation";
        VoluntaryFixture.createVoluntaryService(organization, "<p>Kurz</p>", link);

        String card = card(fetch(indexEndpoint), organization);

        assertThat(card, not(containsString(link)));
        assertThat(Pattern.compile("<a ").matcher(card).results().count(), is(1L));
        assertThat(card, containsString("href=\"/volontariat/detail?slug="));
    }

    /**
     * BR-002: the start page shows only an extract of the events still ahead — the nearest three. A
     * fourth, further away than the others, is left to the complete list.
     */
    @Test
    public void the_event_extract_is_limited() {
        for (int i = 1; i <= 3; i++) {
            EventFixture.createEvent("CLEANUP UC-008 near " + i, LocalDate.now());
        }
        String far = "CLEANUP UC-008 far";
        EventFixture.createEvent(far, LocalDate.now().plusYears(5));

        String home = fetch(indexEndpoint);
        String events = home.substring(home.indexOf("Die nächsten Anlässe"), home.indexOf("Länger bleiben"));

        assertThat(Pattern.compile("<article ").matcher(events).results().count(), is(3L));
        assertThat(events, not(containsString(far)));
        given().when().get("/anlaesse").then().statusCode(HttpStatus.SC_OK).body(containsString(far));
    }

    /**
     * BR-002: the start page shows only an extract of the offers — the two most recently recorded.
     * An older one is left to the complete list.
     */
    @Test
    public void the_offer_extract_is_limited() {
        String oldest = "CLEANUP UC-008 offer oldest";
        VoluntaryFixture.createVoluntaryService(oldest);
        VoluntaryFixture.createVoluntaryService("CLEANUP UC-008 offer middle");
        VoluntaryFixture.createVoluntaryService("CLEANUP UC-008 offer newest");

        String home = fetch(indexEndpoint);
        String offers = home.substring(home.indexOf("Länger bleiben"), home.indexOf("Du weisst noch nicht"));

        assertThat(Pattern.compile("<article ").matcher(offers).results().count(), is(2L));
        assertThat(offers, containsString("CLEANUP UC-008 offer newest"));
        assertThat(offers, containsString("CLEANUP UC-008 offer middle"));
        assertThat(offers, not(containsString(oldest)));
        given().when().get("/volontariat").then().statusCode(HttpStatus.SC_OK).body(containsString(oldest));
    }

    /**
     * A4: a signed-in administrator additionally gets the maintenance functions, as a separate group
     * of quieter controls than those meant for visitors.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void an_administrator_gets_the_maintenance_functions_as_a_quieter_group() {
        String body = fetch(indexEndpoint);

        int group = body.indexOf("<div class=\"admin-bar\">");
        assertThat(group, greaterThan(-1));
        String bar = body.substring(group, body.indexOf("</div>", group));
        assertThat(bar, containsString("<a class=\"btn btn--ghost\" href=\"/anlaesse/add\">"));
        assertThat(bar, containsString("<a class=\"btn btn--ghost\" href=\"/volontariat/add\">"));
        assertThat(bar, not(containsString("btn--primary")));
    }

    /** A4, the other side: a visitor without a session gets no maintenance functions. */
    @Test
    public void a_visitor_gets_no_maintenance_functions() {
        String body = fetch(indexEndpoint);

        assertThat(body, not(containsString("<div class=\"admin-bar\">")));
        assertThat(body, not(containsString("/anlaesse/add")));
    }

    /** Success postcondition: opening the start page changes no data. */
    @Test
    public void entering_the_platform_changes_no_data() {
        long eventId = EventFixture.createEvent("CLEANUP UC-008 unchanged event");
        long offerId = VoluntaryFixture.createVoluntaryService("CLEANUP UC-008 unchanged offer");
        long events = Event.count();
        long offers = VoluntaryService.count();
        Event eventBefore = QuarkusTransaction.requiringNew().call(() -> Event.<Event>findById((int) eventId));
        VoluntaryService offerBefore = QuarkusTransaction.requiringNew().call(() -> VoluntaryService.<VoluntaryService>findById((int) offerId));

        fetch(indexEndpoint);

        Event eventAfter = QuarkusTransaction.requiringNew().call(() -> Event.<Event>findById((int) eventId));
        VoluntaryService offerAfter = QuarkusTransaction.requiringNew().call(() -> VoluntaryService.<VoluntaryService>findById((int) offerId));
        assertThat(Event.count(), is(events));
        assertThat(VoluntaryService.count(), is(offers));
        assertThat(eventAfter.title, is(eventBefore.title));
        assertThat(eventAfter.slug, is(eventBefore.slug));
        assertThat(eventAfter.displayDate, is(eventBefore.displayDate));
        assertThat(eventAfter.description, is(eventBefore.description));
        assertThat(offerAfter.organization, is(offerBefore.organization));
        assertThat(offerAfter.slug, is(offerBefore.slug));
        assertThat(offerAfter.organizationLink, is(offerBefore.organizationLink));
        assertThat(offerAfter.description, is(offerBefore.description));
    }

    /** The markup of the one card in {@code page} whose link names {@code name}. */
    private static String card(String page, String name) {
        int at = page.indexOf(">" + name + "<");
        assertThat(name, at, greaterThan(-1));
        int start = page.lastIndexOf("<article", at);
        return page.substring(start, page.indexOf("</article>", at));
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
