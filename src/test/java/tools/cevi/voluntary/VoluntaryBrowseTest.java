package tools.cevi.voluntary;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.VoluntaryFixture;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

/**
 * UC-003 Browse Voluntary Service Offers over HTTP: the Offer List (steps 1-2, A1-A3) and the
 * offer's own Offer Page (step 4, A4-A6). That the whole entry is the target and the
 * Organisation opens separately in the browser is {@code VoluntaryBrowseE2ETest}.
 */
@QuarkusTest
class VoluntaryBrowseTest {
    private static final String LIST = "/volontariat";
    private static final String PAGE = "/volontariat/detail";

    private static VoluntaryService offer(long id) {
        return QuarkusTransaction.requiringNew().call(() -> VoluntaryService.findById((int) id));
    }

    private static String list() {
        return given().when().get(LIST).then().statusCode(HttpStatus.SC_OK).extract().asString();
    }

    private static String offerPage(String slug) {
        return given().queryParam("slug", slug).when().get(PAGE).then()
                .statusCode(HttpStatus.SC_OK).extract().asString();
    }

    /** The markup of the one card in {@code page} that names {@code organization}. */
    private static String card(String page, String organization) {
        int name = page.indexOf(">" + organization + "<");
        assertThat(organization, name, greaterThan(-1));
        int start = page.lastIndexOf("<article", name);
        return page.substring(start, page.indexOf("</article>", name));
    }

    /**
     * Main success scenario step 2, BR-003: the entry names Organisation and location, and its one
     * action is the link to the offer's Offer Page — not to the Organisation.
     */
    @Test
    void an_entry_leads_to_the_offer_page_and_not_to_the_organisation() {
        String organization = "CLEANUP UC-003 entry";
        String link = "https://entry.example.org/volunteering";
        VoluntaryService offer = offer(VoluntaryFixture.createVoluntaryService(organization, "<p>Kurz</p>", link));

        String card = card(list(), organization);

        assertThat(card, containsString("<a href=\"/volontariat/detail?slug=" + offer.slug + "\">" + organization + "</a>"));
        assertThat(card, containsString("some location"));
        assertThat(card, not(containsString(link)));
        assertThat(Pattern.compile("<a ").matcher(card).results().count(), is(1L));
    }

    /**
     * Step 2, BR-005: an entered Short Name is stored as typed (UC-004 BR-009), so it may hold
     * characters with a meaning in an address. The entry's link still leads to exactly that offer.
     */
    @Test
    void an_entry_leads_to_its_offer_page_whatever_the_short_name_holds() {
        String organization = "CLEANUP UC-003 odd short name";
        long id = VoluntaryFixture.createVoluntaryService(organization);
        String slug = "cleanup-uc-003 a&b#c+d%e";
        QuarkusTransaction.requiringNew().run(() -> VoluntaryService.<VoluntaryService>findById((int) id).slug = slug);

        Matcher href = Pattern.compile("<a href=\"(/volontariat/detail\\?slug=[^\"]+)\">" + organization + "</a>")
                .matcher(card(list(), organization));
        assertThat(href.find(), is(true));

        given().urlEncodingEnabled(false).when().get(href.group(1)).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("<h1>" + organization + "</h1>"));
    }

    /**
     * BR-004: the entry shows an Excerpt, the Offer Page the whole text; the list no longer opens
     * the rest of the text in place.
     */
    @Test
    void a_long_description_is_an_excerpt_in_the_list_and_complete_on_the_offer_page() {
        String organization = "CLEANUP UC-003 long";
        String tail = "SCHLUSSDESANGEBOTS";
        String description = "<p>" + "Wort ".repeat(60) + tail + "</p>";
        VoluntaryService offer = offer(VoluntaryFixture.createVoluntaryService(organization, description, "https://example.org/x"));

        String card = card(list(), organization);
        assertThat(card, containsString("…"));
        assertThat(card, not(containsString(tail)));
        assertThat(card, not(containsString("<details")));

        assertThat(offerPage(offer.slug), containsString(tail));
    }

    /** A2: a Description shorter than the Excerpt stands in full, without a sign of a cut. */
    @Test
    void a_short_description_is_presented_in_full_without_an_ellipsis() {
        String organization = "CLEANUP UC-003 short";
        VoluntaryFixture.createVoluntaryService(organization, "<p>Kurz und vollständig</p>", "https://example.org/x");

        assertThat(card(list(), organization), containsString("<p class=\"card__excerpt\">Kurz und vollständig</p>"));
    }

    /**
     * Step 2, BR-001: every recorded offer is listed, by Organisation in alphabetical order, umlauts
     * beside their base letter and case ignored.
     */
    @Test
    void every_offer_is_listed_by_organisation_in_alphabetical_order() {
        VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 zqorder Österreich");
        VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 zqorder Polen");
        VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 zqorder albanien");

        Matcher matcher = Pattern.compile(">CLEANUP UC-003 zqorder ([^<]+)</a>").matcher(list());
        List<String> order = matcher.results().map(r -> r.group(1)).toList();

        assertThat(order, is(List.of("albanien", "Österreich", "Polen")));
        assertThat(VoluntaryService.offerList().size(), is((int) VoluntaryService.count()));
    }

    /**
     * A1: without any offer the list says so and offers the way to the working group. The demo
     * data is removed and put back around the assertion, as for the start page: the branch under
     * test is exactly "no offer recorded".
     */
    @Test
    void an_empty_list_explains_and_offers_the_way_to_the_working_group() {
        List<VoluntaryService> services = VoluntaryService.listAll();
        try {
            QuarkusTransaction.begin();
            VoluntaryService.deleteAll();
            QuarkusTransaction.commit();

            String page = list();

            assertThat(page, containsString("Zurzeit ist kein Volontariat ausgeschrieben"));
            // not the navigation's "Kontakt" link, which every page has
            assertThat(page, containsString("href=\"/kontakt\">Schreib uns</a>"));
            assertThat(page, not(containsString("<article")));
        } finally {
            QuarkusTransaction.begin();
            services.forEach(VoluntaryBrowseTest::restore);
            QuarkusTransaction.commit();
        }
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

    /**
     * A3: a signed-in administrator additionally gets the maintenance links of each entry, in
     * their own group after the visitor's "Details" control.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_maintenance_links_of_an_entry_form_a_separate_group() {
        String organization = "CLEANUP UC-003 maintenance list";
        long id = VoluntaryFixture.createVoluntaryService(organization);

        String card = card(list(), organization);
        int details = card.indexOf("card__cta");
        int group = card.indexOf("<div class=\"card__admin\">");

        assertThat(details, greaterThan(-1));
        assertThat(group, greaterThan(details));
        assertThat(card.indexOf("href=\"/volontariat/edit?id=" + id + "\""), greaterThan(group));
        assertThat(card.indexOf("href=\"/volontariat/delete?id=" + id + "\""), greaterThan(group));
    }

    /**
     * Step 4, BR-002, BR-003, BR-005: under its Short Name the offer presents Organisation, location,
     * the complete Description as formatted, and the link to the Organisation, which opens
     * separately (step 6) and cannot reach back into the page.
     */
    @Test
    void the_offer_page_presents_the_complete_offer_and_leads_to_the_organisation() {
        String organization = "CLEANUP UC-003 page";
        String link = "https://page.example.org/volunteering";
        VoluntaryService offer = offer(VoluntaryFixture.createVoluntaryService(organization,
                "<p>Sechs Monate im <strong>Jugendzentrum</strong></p><ul><li>Spitak</li></ul>", link));

        String page = offerPage(offer.slug);

        assertThat(page, containsString("<h1>" + organization + "</h1>"));
        assertThat(page, containsString("some location"));
        assertThat(page, containsString("<strong>Jugendzentrum</strong>"));
        assertThat(page, containsString("<li>Spitak</li>"));
        assertThat(page, containsString("<a class=\"btn btn--primary\" href=\"" + link
                + "\" target=\"_blank\" rel=\"noopener noreferrer\">"));
        // BR-003: the reader passes the complete Description before the way out
        assertThat(page.indexOf("Spitak"), lessThan(page.indexOf("href=\"" + link + "\"")));
    }

    /** A4: an Offer Page opened directly offers the way to the complete Offer List. */
    @Test
    void the_offer_page_offers_the_way_to_the_offer_list() {
        VoluntaryService offer = offer(VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 back"));

        assertThat(offerPage(offer.slug), containsString("<a class=\"backlink\" href=\"/volontariat\">"));
    }

    /** A5: an unknown Short Name leads to a page that says so and offers the way to the Offer List. */
    @Test
    void an_unknown_short_name_says_the_page_does_not_exist() {
        given().queryParam("slug", "cleanup-uc-003-gibt-es-nicht").when().get(PAGE).then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"))
                // the error page's own way, not the navigation link every page has
                .body(containsString("href=\"/volontariat\">Zum Volontariat</a>"));
    }

    /** A5: an Offer Page address without a Short Name is just as unknown. */
    @Test
    void an_offer_page_without_a_short_name_does_not_exist() {
        given().when().get(PAGE).then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"));
    }

    /** A6, UC-006 BR-003: a visitor sees no maintenance links on an Offer Page. */
    @Test
    void a_visitor_gets_no_maintenance_links_on_the_offer_page() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 visitor page");

        String page = offerPage(offer(id).slug);

        assertThat(page, not(containsString("/volontariat/edit?id=")));
        assertThat(page, not(containsString("/volontariat/delete?id=")));
    }

    /**
     * A6: a signed-in administrator additionally gets the maintenance links of the offer on its
     * page, in their own group apart from the control leading to the Organisation, which stays the
     * primary one.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_administrator_gets_the_maintenance_links_on_the_offer_page_as_a_separate_group() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 admin page");

        String page = offerPage(offer(id).slug);
        int group = page.indexOf("<div class=\"admin-bar\">");
        int end = page.indexOf("</div>", group);

        assertThat(group, greaterThan(-1));
        String bar = page.substring(group, end);
        assertThat(bar, containsString("href=\"/volontariat/edit?id=" + id + "\""));
        assertThat(bar, containsString("href=\"/volontariat/delete?id=" + id + "\""));
        assertThat(bar, not(containsString("btn--primary")));
        assertThat(page, containsString("<a class=\"btn btn--primary\" href=\"https://example.org/some-link\""));
    }

    /** Success postcondition: browsing changes no data. */
    @Test
    void browsing_changes_no_data() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-003 no change");
        long count = VoluntaryService.count();

        list();
        offerPage(offer(id).slug);

        assertThat(VoluntaryService.count(), is(count));
    }
}
