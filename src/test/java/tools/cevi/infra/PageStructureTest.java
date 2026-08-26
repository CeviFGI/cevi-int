package tools.cevi.infra;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The structure every page shares (NFR-043).
 *
 * <p>Before the redesign every page opened at {@code h2} with no {@code h1} at all, and the data
 * protection page mixed {@code h2} and {@code h3} for sections at the same level. Both are
 * invisible to a sighted reader and both break navigation by headings, which is how a screen
 * reader user moves through a long page — so neither shows up until someone tests for it.
 */
@QuarkusTest
class PageStructureTest {

    private static final Pattern HEADING = Pattern.compile("<h([1-6])[\\s>]");

    @ParameterizedTest
    @ValueSource(strings = {"/", "/anlaesse", "/volontariat", "/kontakt", "/fgi",
                            "/datenschutzinformation", "/auth/login"})
    void every_public_page_has_exactly_one_top_level_heading(String path) {
        String body = fetch(path);

        assertEquals(1, count(body, "<h1"),
                path + " should carry exactly one h1 — the title of the page");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/anlaesse", "/volontariat", "/kontakt", "/fgi",
                            "/datenschutzinformation", "/auth/login"})
    void every_public_page_skips_no_heading_level(String path) {
        String body = fetch(path);

        int previous = 0;
        Matcher matcher = HEADING.matcher(body);
        while (matcher.find()) {
            int level = Integer.parseInt(matcher.group(1));
            if (previous != 0) {
                assertTrue(level <= previous + 1,
                        path + " jumps from h" + previous + " to h" + level);
            }
            previous = level;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/anlaesse", "/volontariat", "/kontakt", "/fgi",
                            "/datenschutzinformation", "/auth/login"})
    void every_public_page_offers_a_skip_link_before_anything_else(String path) {
        String body = fetch(path);

        int skipLink = body.indexOf("class=\"skip-link\"");
        assertTrue(skipLink > 0, path + " should offer a skip link (WCAG 2.4.1)");

        int firstNavLink = body.indexOf("site-nav__link");
        assertTrue(firstNavLink < 0 || skipLink < firstNavLink,
                path + " should place the skip link before the navigation, or it skips nothing");
    }

    /**
     * The data protection information must be reachable from every page without signing in
     * (BR-026, C-011). It sits in the navigation drawer and in the footer; either satisfies this.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/", "/anlaesse", "/volontariat", "/kontakt", "/fgi", "/auth/login"})
    void every_public_page_links_to_the_data_protection_information(String path) {
        assertTrue(fetch(path).contains("/datenschutzinformation"),
                path + " should link to the data protection information");
    }

    private static int count(String haystack, String needle) {
        int found = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    private static String fetch(String path) {
        return given().when().get(path).then().statusCode(200).extract().body().asString();
    }
}
