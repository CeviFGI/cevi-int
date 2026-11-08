package tools.cevi.voluntary;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.VoluntaryFixture;

import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

/**
 * UC-011 Share a Voluntary Service Offer — what the server delivers for the share control on the
 * Offer Page. The behaviour in the browser (Share Dialog, clipboard, confirmation) is covered by
 * {@code ShareOfferE2ETest}.
 */
@QuarkusTest
class VoluntaryShareTest {

    private static final String SHARE_SCRIPT = "/js/share.js";

    @TestHTTPResource("/")
    URL root;

    private String origin() {
        return root.getProtocol() + "://" + root.getHost() + ":" + root.getPort();
    }

    private static String slugOf(long id) {
        return QuarkusTransaction.requiringNew().call(() -> VoluntaryService.<VoluntaryService>findById(id).slug);
    }

    private static String offerPage(String query) {
        return given().when().get("/volontariat/detail?" + query).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    private static String inputValue(String body) {
        Matcher matcher = Pattern.compile("<input id=\"share-address\"[^>]*value=\"([^\"]*)\"").matcher(body);
        assertThat("the offer page carries the address field", matcher.find(), is(true));
        return matcher.group(1).replace("&amp;", "&");
    }

    /**
     * A4 and BR-005: without script the Offer Page shows the Public Address as selectable text,
     * labelled as the link to the offer, and the share button stays hidden until the script
     * reveals it. The Description and the Organisation Link are there as before.
     */
    @Test
    void without_script_the_page_shows_the_public_address_as_labelled_text() {
        String slug = slugOf(VoluntaryFixture.createVoluntaryService("CLEANUP UC-011 without script",
                "<p>Ganze Beschreibung</p>", "https://example.org/uc-011"));

        String body = offerPage("slug=" + slug);

        assertThat(body, containsString("<label for=\"share-address\">Link zu diesem Angebot</label>"));
        assertThat(body, matchesPattern("(?s).*<input id=\"share-address\" type=\"text\" readonly .*"));
        assertThat(inputValue(body), is(origin() + "/volontariat/detail?slug=" + slug));
        assertThat(body, matchesPattern("(?s).*<button class=\"[^\"]*share__button\" type=\"button\" hidden>.*"));
        assertThat(body, containsString("<p>Ganze Beschreibung</p>"));
        assertThat(body, containsString("href=\"https://example.org/uc-011\""));
    }

    /**
     * BR-003: the address passed on is the scheme, host and port the page was opened under and
     * the Offer Page with the Short Name as its one parameter — nothing else from the address the
     * visitor arrived with.
     */
    @Test
    void the_public_address_drops_every_parameter_of_the_request() {
        String slug = slugOf(VoluntaryFixture.createVoluntaryService("CLEANUP UC-011 drops parameters"));

        String body = offerPage("utm_source=newsletter&slug=" + slug + "&session=abc");

        String expectedPath = "/volontariat/detail?slug=" + slug;
        assertThat(inputValue(body), is(origin() + expectedPath));
        assertThat(body, containsString("data-path=\"" + expectedPath + "\""));
        assertThat(body, not(containsString("utm_source")));
        assertThat(body, not(containsString("session=abc")));
    }

    /**
     * BR-003: a Short Name is stored as entered and may carry {@code &}, {@code +} and {@code #};
     * the Public Address encodes them, so that following it leads to this very offer.
     */
    @Test
    void the_public_address_of_a_short_name_with_reserved_characters_leads_to_the_offer() {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-011 reserved characters");
        String slug = "uc-011 a&b+c#d " + id;
        QuarkusTransaction.requiringNew().run(() -> VoluntaryService.<VoluntaryService>findById(id).slug = slug);

        String body = given().queryParam("slug", slug).when().get("/volontariat/detail").then()
                .statusCode(HttpStatus.SC_OK).extract().body().asString();
        String address = inputValue(body);

        assertThat(address, not(containsString("#")));
        URI uri = URI.create(address);
        given().urlEncodingEnabled(false).when().get(uri.getRawPath() + "?" + uri.getRawQuery()).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("CLEANUP UC-011 reserved characters"));
    }

    /**
     * BR-002: the Share Dialog receives the offer's Organisation and its location next to the
     * address, because an offer has no title.
     */
    @Test
    void the_share_control_carries_organisation_and_location() {
        String organisation = "CLEANUP UC-011 Organisation & Ort";
        String slug = slugOf(VoluntaryFixture.createVoluntaryService(organisation));

        String body = offerPage("slug=" + slug);

        String escaped = organisation.replace("&", "&amp;");
        assertThat(body, containsString("data-title=\"" + escaped + "\""));
        assertThat(body, containsString("data-text=\"" + escaped + ", some location\""));
    }

    /**
     * BR-005, NFR-041 and C-021: the Offer Page loads the same share script as the Event Page from
     * its own origin; every script it carries is self-hosted, none is inline, and together they
     * stay within 8 KB.
     */
    @Test
    void the_offer_page_loads_the_share_script_from_its_own_origin() {
        String slug = slugOf(VoluntaryFixture.createVoluntaryService("CLEANUP UC-011 script"));

        String body = offerPage("slug=" + slug);

        assertThat(body, containsString("<script src=\"" + SHARE_SCRIPT + "\" defer></script>"));
        Matcher script = Pattern.compile("(?i)<script\\b[^>]*>").matcher(body);
        int total = 0;
        while (script.find()) {
            Matcher src = Pattern.compile("src=\"(/js/[^\"]+)\"").matcher(script.group());
            assertThat(script.group(), src.find(), is(true));
            total += given().when().get(src.group(1)).then().statusCode(HttpStatus.SC_OK)
                    .extract().body().asByteArray().length;
        }
        assertThat(total, lessThanOrEqualTo(8 * 1024));
    }
}
