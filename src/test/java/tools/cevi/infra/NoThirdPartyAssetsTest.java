package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFR-042: a public page fetches nothing from anywhere but this application's own origin.
 *
 * <p>This is a data-protection requirement before it is a performance one. A stylesheet, a font or
 * an icon loaded from a third party discloses every visitor's IP address to that party — something
 * neither C-011 nor the data protection statement covers — and each such source would have to be
 * written into the Content-Security-Policy of NFR-024, which is what turns a policy into a
 * formality. The web fonts are in this repository for exactly that reason (see {@code base.css}).
 *
 * <p>The distinction the test has to keep is between <em>fetching</em> and <em>linking</em>. An
 * event description pointing at the organiser's site, the footer crediting the host — those are
 * navigation targets a visitor chooses to follow, and the requirement says nothing against them.
 * Only the attributes a browser resolves on its own are checked.
 */
@QuarkusTest
class NoThirdPartyAssetsTest {

    /** Attributes the browser fetches without being asked, plus what a stylesheet pulls in. */
    private static final Pattern FETCHED_BY_THE_BROWSER = Pattern.compile(
            "(?i)\\b(?:src|srcset|poster|data)\\s*=\\s*\"([^\"]+)\""
                    + "|<link\\b[^>]*\\bhref\\s*=\\s*\"([^\"]+)\"");

    private static final Pattern CSS_URL = Pattern.compile("url\\(\\s*[\"']?([^)\"']+)");

    private static final Pattern CSS_IMPORT = Pattern.compile("@import\\s+\"([^\"]+)\"");

    /**
     * A {@code data:} URI carries its own bytes and reaches no host — the triangle motif and the
     * location pin are drawn that way on purpose.
     */
    private static boolean reachesAnotherHost(String url) {
        String trimmed = url.trim().toLowerCase(java.util.Locale.ROOT);
        return trimmed.startsWith("//")
                || trimmed.startsWith("http://")
                || trimmed.startsWith("https://");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/anlaesse", "/volontariat", "/kontakt", "/fgi",
                            "/datenschutzinformation", "/auth/login", "/version"})
    void a_public_page_fetches_nothing_from_another_host(String path) {
        List<String> external = new ArrayList<>();

        Matcher matcher = FETCHED_BY_THE_BROWSER.matcher(fetch(path));
        while (matcher.find()) {
            String url = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (reachesAnotherHost(url)) {
                external.add(url);
            }
        }

        assertTrue(external.isEmpty(),
                () -> path + " asks the browser to fetch from another host (NFR-042):\n  "
                        + String.join("\n  ", external));
    }

    /**
     * And the same for the stylesheets, which are the likelier route: a single
     * {@code @import url("https://fonts.googleapis.com/…")} would put every visitor's address in
     * front of a third party without changing a line of markup.
     */
    @org.junit.jupiter.api.Test
    void no_stylesheet_pulls_in_anything_from_another_host() {
        List<String> external = new ArrayList<>();
        String hub = fetch("/css/site.css");

        List<String> sheets = new ArrayList<>();
        sheets.add(hub);
        for (String imported : CSS_IMPORT.matcher(hub).results().map(r -> r.group(1)).toList()) {
            if (reachesAnotherHost(imported)) {
                external.add("site.css imports " + imported);
            } else {
                sheets.add(fetch("/css/" + imported));
            }
        }

        for (String css : sheets) {
            Matcher matcher = CSS_URL.matcher(css);
            while (matcher.find()) {
                if (reachesAnotherHost(matcher.group(1))) {
                    external.add(matcher.group(1));
                }
            }
        }

        assertTrue(external.isEmpty(),
                () -> "the stylesheets reach another host (NFR-042):\n  "
                        + String.join("\n  ", external));
    }

    private static String fetch(String path) {
        return given().when().get(path).then().statusCode(200).extract().body().asString();
    }
}
