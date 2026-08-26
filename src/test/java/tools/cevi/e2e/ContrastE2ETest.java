package tools.cevi.e2e;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WCAG 1.4.3, the half of NFR-014 that can be measured: every piece of text reaches 4.5 : 1 against
 * what is actually painted behind it — 3 : 1 where the text is large enough for the exception.
 *
 * <p>{@code tokens.css} states the ratios of the palette in its own header, and those figures are
 * checked in {@link ContrastTest}. That is not the same thing as this test. A palette can be sound
 * and still be used wrongly: a muted grey chosen against white and then placed on the sunken
 * surface, a colour faded with {@code opacity} because it looked better, a card that inherits a
 * light text colour from the dark band it was placed on. All three are combinations rather than
 * colours, and only a rendered page holds them.
 *
 * <p>What the browser reports is composited in Java rather than in the page script — see
 * {@link Contrast}. Backgrounds are gathered by walking up from the element until an opaque one is
 * found, and every colour stop of a gradient on the way is treated as a backdrop the text might
 * land on. That is a worst case on purpose: a gate that guesses in the visitor's favour is a gate
 * that passes a page nobody can read.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class ContrastE2ETest extends PlaywrightTestBase {

    /** A separator no rendered text can contain, so the reading needs no quoting. */
    private static final String FIELD = "";

    private static final Pattern COLOUR = Pattern.compile("rgba?\\([^)]*\\)");

    static List<String> routes() {
        return PublicPages.ROUTES;
    }

    /**
     * Reports one line per element that holds text of its own: where it is, the colour it is
     * painted in, the opacity it inherited, the backdrops behind it, and the size and weight that
     * decide which threshold applies.
     */
    private static final String TEXT_SAMPLES = """
            (separator) => {
              const alphaOf = colour => {
                const parts = colour.replace(/rgba?\\(|\\)/g, '').split(/[,\\s\\/]+/).filter(Boolean);
                return parts.length > 3 ? parseFloat(parts[3]) : 1;
              };

              const backdropsBehind = element => {
                const colours = [];
                for (let node = element; node; node = node.parentElement) {
                  const style = getComputedStyle(node);
                  let hidesWhatIsBelow = false;
                  if (style.backgroundImage && style.backgroundImage !== 'none') {
                    const stops = [...style.backgroundImage.matchAll(/rgba?\\([^)]*\\)/g)].map(m => m[0]);
                    for (const stop of stops) if (alphaOf(stop) > 0) colours.push(stop);
                    // A gradient whose every stop is opaque paints over everything beneath it, so
                    // the walk ends there. One with a transparent stop — the hero's two radial
                    // washes — lets the ground through, and the walk carries on.
                    hidesWhatIsBelow = stops.length > 0 && stops.every(stop => alphaOf(stop) === 1);
                  }
                  const behind = style.backgroundColor;
                  if (alphaOf(behind) > 0) {
                    colours.push(behind);
                    if (alphaOf(behind) === 1) break;
                  }
                  if (hidesWhatIsBelow) break;
                }
                return colours;
              };

              const opacityOf = element => {
                let total = 1;
                for (let node = element; node; node = node.parentElement) {
                  total *= parseFloat(getComputedStyle(node).opacity);
                }
                return total;
              };

              const lines = [];
              for (const element of document.querySelectorAll('body *')) {
                const hasOwnText = [...element.childNodes].some(
                    node => node.nodeType === Node.TEXT_NODE && node.textContent.trim().length > 0);
                if (!hasOwnText) continue;

                const box = element.getBoundingClientRect();
                if (box.width === 0 || box.height === 0) continue;

                const style = getComputedStyle(element);
                if (style.visibility === 'hidden' || style.display === 'none') continue;
                // Off-screen on purpose and never read by eye: the skip link until it is focused,
                // the hidden labels, the honeypot.
                if (element.closest('.visually-hidden, .skip-link, [hidden]')) continue;

                const where = element.tagName.toLowerCase()
                    + (element.className && typeof element.className === 'string'
                        ? '.' + element.className.trim().split(/\\s+/).join('.') : '')
                    + ` "${element.textContent.trim().replace(/\\s+/g, ' ').slice(0, 28)}"`;

                lines.push([where, style.color, opacityOf(element),
                            backdropsBehind(element).join(' '),
                            parseFloat(style.fontSize), parseInt(style.fontWeight, 10)]
                           .join(separator));
              }
              return lines.join('\\n');
            }
            """;

    @ParameterizedTest
    @MethodSource("routes")
    void every_piece_of_text_reaches_its_contrast_threshold(String route) {
        page.setViewportSize(1280, 900);
        page.navigate(url(route));
        page.evaluate("() => document.querySelectorAll('details').forEach(d => d.open = true)");

        assertNothingIsTooFaint(route);
    }

    /**
     * The state a visitor only reaches by getting something wrong. It carries the one palette on
     * the site that exists to be alarming, which is exactly the palette that gets chosen for how it
     * looks rather than for whether it can be read.
     */
    @Test
    void the_rejected_contact_form_reaches_its_contrast_threshold() {
        page.setViewportSize(1280, 900);
        page.navigate(url("/kontakt"));

        // The fields are `required`, so an empty form never leaves the browser. A wrong answer to
        // the spam question is the shortest way to a server-side rejection (BR-017).
        page.locator("#message").fill("Wie kommt man an eine Stelle in Armenien?");
        page.locator("#spam").fill("7");
        page.locator(".form-actions button[type='submit']").click();
        page.waitForSelector(".error-summary");

        assertNothingIsTooFaint("/kontakt after a rejected submission");
    }

    /** The maintenance chrome, which only an administrator sees and therefore nobody reviews. */
    @Test
    void the_administrator_chrome_reaches_its_contrast_threshold() {
        page.setViewportSize(1280, 900);
        signInAsAdministrator();

        for (String route : List.of("/", "/anlaesse", "/volontariat")) {
            page.navigate(url(route));
            page.evaluate("() => document.querySelectorAll('details').forEach(d => d.open = true)");
            assertNothingIsTooFaint(route + " signed in as administrator");
        }
    }

    private void assertNothingIsTooFaint(String where) {
        String samples = (String) page.evaluate(TEXT_SAMPLES, FIELD);

        List<String> tooFaint = new ArrayList<>();
        for (String line : samples.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(FIELD, -1);
            String element = parts[0];
            Contrast colour = Contrast.parse(parts[1]);
            double opacity = Double.parseDouble(parts[2]);
            double fontSize = Double.parseDouble(parts[4]);
            int fontWeight = Integer.parseInt(parts[5]);

            List<Contrast> backdrops = backdrops(parts[3]);
            if (backdrops.isEmpty()) {
                continue;
            }
            Contrast ground = opaqueGroundOf(backdrops);

            double threshold = Contrast.thresholdFor(fontSize, fontWeight);
            double worst = Double.MAX_VALUE;
            Contrast worstBackdrop = ground;
            for (Contrast candidate : backdrops) {
                Contrast backdrop = candidate.over(ground);
                double ratio = colour.fadedTo(opacity, backdrop).ratioAgainst(backdrop);
                if (ratio < worst) {
                    worst = ratio;
                    worstBackdrop = backdrop;
                }
            }

            if (worst + 0.005 < threshold) {
                tooFaint.add(String.format(Locale.ROOT,
                        "%s: %s on %s is %.2f:1, needs %.1f:1 at %.1fpx weight %d",
                        element, colour, worstBackdrop, worst, threshold, fontSize, fontWeight));
            }
        }

        assertTrue(tooFaint.isEmpty(),
                () -> where + " has text below its contrast threshold (WCAG 1.4.3, NFR-014):\n  "
                        + String.join("\n  ", tooFaint));
    }

    /**
     * What the semi-transparent layers are ultimately composited onto. Normally the last colour
     * the walk found, since it ends at an opaque one; white only if a page ever manages not to
     * paint a ground at all, which is what a browser would show.
     */
    private static Contrast opaqueGroundOf(List<Contrast> backdrops) {
        for (int layer = backdrops.size() - 1; layer >= 0; layer--) {
            if (backdrops.get(layer).alpha() >= 1.0) {
                return backdrops.get(layer);
            }
        }
        return Contrast.parse("rgb(255, 255, 255)");
    }

    /** Nearest first, as the page reported them. */
    private static List<Contrast> backdrops(String reported) {
        List<Contrast> colours = new ArrayList<>();
        Matcher matcher = COLOUR.matcher(reported);
        while (matcher.find()) {
            colours.add(Contrast.parse(matcher.group()));
        }
        return colours;
    }
}
