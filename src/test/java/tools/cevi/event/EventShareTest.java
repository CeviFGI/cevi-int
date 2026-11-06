package tools.cevi.event;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.nio.charset.StandardCharsets;
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
 * UC-009 Share an Event — what the server delivers for the share control. The behaviour in the
 * browser (Share Dialog, clipboard, confirmation) is covered by {@code ShareEventE2ETest}.
 */
@QuarkusTest
class EventShareTest {

    private static final String SHARE_SCRIPT = "/js/share.js";

    @TestHTTPResource("/")
    URL root;

    private String origin() {
        return root.getProtocol() + "://" + root.getHost() + ":" + root.getPort();
    }

    private String slugOf(long id) {
        return ((Event) Event.findById(id)).slug;
    }

    private String eventPage(String query) {
        return given().when().get("/anlaesse/detail?" + query).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    private static String inputValue(String body) {
        Matcher matcher = Pattern.compile("<input id=\"share-address\"[^>]*value=\"([^\"]*)\"").matcher(body);
        assertThat("the event page carries the address field", matcher.find(), is(true));
        return matcher.group(1);
    }

    /**
     * A4 and BR-005: without script the page shows the Public Address as selectable text,
     * labelled as the link to the event, and the share button stays hidden until the script
     * reveals it.
     */
    @Test
    void without_script_the_page_shows_the_public_address_as_labelled_text() {
        String slug = slugOf(EventFixture.createEvent("CLEANUP share without script"));

        String body = eventPage("slug=" + slug);

        assertThat(body, containsString("<label for=\"share-address\">Link zu diesem Anlass</label>"));
        assertThat(body, matchesPattern("(?s).*<input id=\"share-address\" type=\"text\" readonly .*"));
        assertThat(inputValue(body), is(origin() + "/anlaesse/detail?slug=" + slug));
        assertThat(body, matchesPattern("(?s).*<button class=\"[^\"]*share__button\" type=\"button\" hidden>.*"));
    }

    /**
     * BR-003: the address passed on is the scheme, host and port the page was opened under and
     * the path from the Short Name — nothing else from the address the visitor arrived with.
     */
    @Test
    void the_public_address_drops_every_parameter_of_the_request() {
        String slug = slugOf(EventFixture.createEvent("CLEANUP share drops parameters"));

        String body = eventPage("utm_source=newsletter&slug=" + slug + "&session=abc");

        String expectedPath = "/anlaesse/detail?slug=" + slug;
        assertThat(inputValue(body), is(origin() + expectedPath));
        assertThat(body, containsString("data-path=\"" + expectedPath + "\""));
        assertThat(body, not(containsString("utm_source")));
        assertThat(body, not(containsString("session=abc")));
    }

    /**
     * BR-002: the Share Dialog receives the event's title and its date text next to the address,
     * so a recipient sees what the link is about.
     */
    @Test
    void the_share_control_carries_title_and_date_text() {
        String title = "CLEANUP share title and date";
        String slug = slugOf(EventFixture.createEvent(title));

        String body = eventPage("slug=" + slug);

        assertThat(body, containsString("data-title=\"" + title + "\""));
        assertThat(body, containsString("data-text=\"" + title + ", some date\""));
    }

    /** NFR-041 and C-021: the event page loads the one share script, from the own origin. */
    @Test
    void the_event_page_loads_the_share_script_from_its_own_origin() {
        String slug = slugOf(EventFixture.createEvent("CLEANUP share script"));

        String body = eventPage("slug=" + slug);

        assertThat(body, containsString("<script src=\"" + SHARE_SCRIPT + "\" defer></script>"));
        assertThat(Pattern.compile("(?i)<script\\b").matcher(body).results().count(), is(1L));
    }

    /**
     * NFR-041: the share script stays within 2 KB uncompressed. BR-004: it addresses no other
     * host and sends nothing anywhere — no fetch, no beacon, no request object.
     */
    @Test
    void the_share_script_is_small_and_talks_to_no_host() {
        byte[] script = given().when().get(SHARE_SCRIPT).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asByteArray();
        String source = new String(script, StandardCharsets.UTF_8);

        assertThat(script.length, lessThanOrEqualTo(2048));
        assertThat(source, not(containsString("http")));
        assertThat(source, not(containsString("fetch")));
        assertThat(source, not(containsString("sendBeacon")));
        assertThat(source, not(containsString("XMLHttpRequest")));
    }
}
