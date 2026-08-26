package tools.cevi.e2e;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.cevi.fixture.EventFixture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFR-013, the half of it a browser can answer: at every width the requirement names, no public
 * page scrolls sideways.
 *
 * <p>This was marked <em>Open (assumed)</em> for as long as the requirement existed, because
 * nothing in a REST-Assured assertion can see a layout. A page that overflows horizontally is the
 * single most common phone defect and the one a desktop review never finds — the window is wide
 * enough that the overflowing element still fits.
 *
 * <p>One test per width rather than one per width × route: the viewport is what costs a page load
 * to change, the route is not, and a failure names the routes that broke.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class ResponsiveLayoutE2ETest extends PlaywrightTestBase {

    /**
     * A table far wider than any phone, inside a description — the one shape NFR-013 singles out
     * as having to scroll inside its own container instead of taking the page with it. Written the
     * way the sanitiser stores it, because that is what the detail page renders.
     */
    private static final String DESCRIPTION_WITH_A_WIDE_TABLE = """
            <p>Kosten pro Person, alles inbegriffen:</p>
            <table>
              <thead><tr>
                <th>Unterkunft</th><th>Verpflegung</th><th>Anreise</th>
                <th>Versicherung</th><th>Programmbeitrag</th><th>Total</th>
              </tr></thead>
              <tbody><tr>
                <td>900 USD</td><td>240 USD</td><td>1200 USD</td>
                <td>85 USD</td><td>150 USD</td><td>2575 USD</td>
              </tr></tbody>
            </table>
            <p>Angemeldet wird über die Fachgruppe.</p>
            """;

    @ParameterizedTest
    @ValueSource(ints = {320, 360, 768, 1024, 1440, 1920})
    void no_public_page_scrolls_sideways(int width) {
        String detail = anEventWithAWideTable();
        page.setViewportSize(width, 900);

        List<String> overflowing = new ArrayList<>();
        for (String route : withDetail(detail)) {
            page.navigate(url(route));
            Measurement document = measure("document.scrollingElement");
            if (document.overflows()) {
                overflowing.add(route + " scrolls to " + document.scrollWidth + " px in a "
                        + document.clientWidth + " px viewport, pushed by "
                        + whatSticksOutOf("body"));
            }
        }

        assertTrue(overflowing.isEmpty(),
                () -> "at " + width + " px these pages scroll sideways (NFR-013):\n  "
                        + String.join("\n  ", overflowing));
    }

    /**
     * The other half of the requirement, and the reason the assertion above is not vacuous: the
     * wide table has to be genuinely wider than the phone and still not move the page. If it ever
     * stopped overflowing its own box, the test above would be passing on a table that fits.
     */
    @ParameterizedTest
    @ValueSource(ints = {320, 360})
    void a_table_wider_than_the_phone_scrolls_inside_its_own_container(int width) {
        String detail = anEventWithAWideTable();
        page.setViewportSize(width, 900);
        page.navigate(url(detail));

        Measurement container = measure("document.querySelector('.prose .prose-table')");
        assertTrue(container.exists,
                "a table in a description needs a scroll container of its own, or it takes the page with it");
        assertTrue(container.overflows(),
                () -> "the probe table should be wider than a " + width + " px phone, but the container "
                        + "measured " + container.scrollWidth + " px in " + container.clientWidth + " px "
                        + "— the assertion above would be proving nothing");

        Measurement document = measure("document.scrollingElement");
        assertTrue(!document.overflows(),
                () -> "the table scrolls, but so does the page: " + document.scrollWidth
                        + " px in " + document.clientWidth + " px (NFR-013)");
    }

    /**
     * The column an ordinary page lays itself out in. A block that outgrows it does not always take
     * the document with it — the column is centred, so up to a gutter's worth of overflow stays
     * inside the viewport and the assertion above sees nothing. It is the same defect that a
     * narrower gutter would have turned into a sideways-scrolling page, so it is worth its own name.
     */
    @ParameterizedTest
    @ValueSource(ints = {320, 360, 768, 1024, 1440, 1920})
    void nothing_outgrows_the_column_it_was_laid_out_in(int width) {
        page.setViewportSize(width, 900);

        List<String> bursting = new ArrayList<>();
        for (String route : PublicPages.ROUTES) {
            page.navigate(url(route));
            Measurement column = measure("document.querySelector('.page-column, .container')");
            if (column.overflows()) {
                bursting.add(route + ": content is " + column.scrollWidth + " px wide in a "
                        + column.clientWidth + " px column, pushed by "
                        + whatSticksOutOf(".page-column, .container"));
            }
        }

        assertTrue(bursting.isEmpty(),
                () -> "at " + width + " px content bursts out of its column (NFR-013):\n  "
                        + String.join("\n  ", bursting));
    }

    /**
     * Names the elements reaching past the right edge of {@code container}, skipping any that an
     * intervening scroll container already clips — a cell inside a scrolling table still reports a
     * rect far past the edge, and reporting it would send the reader after the wrong element.
     *
     * <p>A bare "the page is 84 px too wide" sends whoever reads it back into the browser; this
     * hands them the selector.
     */
    private String whatSticksOutOf(String container) {
        return (String) page.evaluate("""
                (selector) => {
                  const box = document.querySelector(selector);
                  if (!box) return 'no ' + selector + ' on this page';
                  const edge = box.getBoundingClientRect().right;

                  const name = node => node.tagName.toLowerCase()
                      + (node.className && typeof node.className === 'string'
                          ? '.' + node.className.trim().split(/\\s+/).join('.') : '');

                  // Inclusive of `start`, because a text node's own parent may be the thing that
                  // clips it — .visually-hidden is a 1px box holding a whole sentence.
                  const clippedFrom = start => {
                    for (let node = start; node && node !== box; node = node.parentElement) {
                      if (getComputedStyle(node).overflowX !== 'visible') return true;
                    }
                    return false;
                  };

                  const guilty = [...box.querySelectorAll('*')]
                      .filter(element => element.getBoundingClientRect().right > edge + 0.5)
                      .filter(element => !clippedFrom(element.parentElement))
                      .map(element => `${name(element)} reaches ${element.getBoundingClientRect().right.toFixed(0)}px`);

                  // A word too long to break overflows its block without any element box growing,
                  // so the element sweep above cannot see it. Measuring the text itself can.
                  const walker = document.createTreeWalker(box, NodeFilter.SHOW_TEXT);
                  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
                    if (!node.textContent.trim()) continue;
                    if (clippedFrom(node.parentElement)) continue;
                    const range = document.createRange();
                    range.selectNodeContents(node);
                    const rect = range.getBoundingClientRect();
                    if (rect.right > edge + 0.5) {
                      guilty.push(`text in ${name(node.parentElement)} reaches ${rect.right.toFixed(0)}px`
                          + ` ("${node.textContent.trim().slice(0, 40)}")`);
                    }
                  }

                  return guilty.length ? [...new Set(guilty)].slice(0, 5).join(', ')
                                       : 'nothing that is not already clipped';
                }
                """, container);
    }

    private List<String> withDetail(String detail) {
        List<String> routes = new ArrayList<>(PublicPages.ROUTES);
        routes.add(detail);
        return routes;
    }

    /** @return the route of the detail page showing it */
    private String anEventWithAWideTable() {
        EventFixture.createEvent("Breite Tabelle", LocalDate.now().plusDays(4),
                DESCRIPTION_WITH_A_WIDE_TABLE);
        return "/anlaesse/detail?slug=breite-tabelle";
    }

    /**
     * Playwright hands numbers back as {@code Integer} or {@code Double} depending on the value,
     * so the measurement crosses as text and is parsed here.
     */
    private Measurement measure(String elementExpression) {
        String reading = (String) page.evaluate(
                "() => { const element = " + elementExpression + ";"
                        + " return element ? `${element.scrollWidth}:${element.clientWidth}` : 'none'; }");
        if ("none".equals(reading)) {
            return new Measurement(false, 0, 0);
        }
        String[] parts = reading.split(":");
        return new Measurement(true, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private record Measurement(boolean exists, int scrollWidth, int clientWidth) {
        boolean overflows() {
            return exists && scrollWidth > clientWidth;
        }
    }
}
