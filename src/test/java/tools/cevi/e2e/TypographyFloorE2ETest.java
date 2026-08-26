package tools.cevi.e2e;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The other threshold NFR-013 sets: nothing a visitor is meant to read renders below 14 px.
 *
 * <p>The type scale is fluid — every size is a {@code clamp()} whose floor was chosen for a 320 px
 * viewport. That makes the whole scale one edit away from dropping under the floor without anyone
 * noticing on a desktop, which is exactly what this test is for. It measures the size the browser
 * actually computed, not the token that was written.
 *
 * <p>Measured at the narrow end only: {@code clamp()} is monotonic in the viewport width, so the
 * smallest text on the site is the text at the smallest viewport.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class TypographyFloorE2ETest extends PlaywrightTestBase {

    private static final double FLOOR_PX = 14.0;

    /** Sub-pixel rounding: 13.9968 px from a clamp is 14 px, not a defect. */
    private static final double TOLERANCE_PX = 0.05;

    /**
     * Collects every element that directly holds readable text, skips what is not rendered, and
     * reports the smallest computed size per offending selector.
     *
     * <p>Off-screen text is skipped rather than measured: the skip link, the visually hidden
     * labels and the honeypot are there for a screen reader or a keyboard, and a screen reader has
     * no font size.
     */
    private static final String SMALLEST_RENDERED_TEXT = """
            (floor) => {
              const offenders = new Map();
              for (const element of document.querySelectorAll('body *')) {
                const hasOwnText = [...element.childNodes].some(
                    node => node.nodeType === Node.TEXT_NODE && node.textContent.trim().length > 0);
                if (!hasOwnText) continue;

                const box = element.getBoundingClientRect();
                if (box.width === 0 || box.height === 0) continue;

                const style = getComputedStyle(element);
                if (style.visibility === 'hidden' || style.display === 'none') continue;
                if (element.closest('.visually-hidden, .skip-link, [hidden]')) continue;

                const size = parseFloat(style.fontSize);
                if (size >= floor) continue;

                const where = element.tagName.toLowerCase()
                    + (element.className && typeof element.className === 'string'
                        ? '.' + element.className.trim().split(/\\s+/).join('.') : '');
                if (!offenders.has(where) || offenders.get(where) > size) {
                  offenders.set(where, size);
                }
              }
              return [...offenders].map(([where, size]) => `${where} at ${size.toFixed(2)}px`).join('\\n  ');
            }
            """;

    @ParameterizedTest
    @ValueSource(ints = {320, 360})
    void no_rendered_text_falls_below_the_readable_floor(int width) {
        page.setViewportSize(width, 900);

        for (String route : PublicPages.ROUTES) {
            page.navigate(url(route));
            String offenders = (String) page.evaluate(SMALLEST_RENDERED_TEXT, FLOOR_PX - TOLERANCE_PX);

            assertTrue(offenders.isEmpty(),
                    () -> route + " renders text below " + FLOOR_PX + " px at " + width
                            + " px (NFR-013):\n  " + offenders);
        }
    }

    /**
     * The maintenance chrome, for the same reason the other phase-5 gates check it: it is the part
     * of the interface no visitor ever sees, so a label set two steps too small survives there.
     */
    @Test
    void the_maintenance_chrome_stays_above_the_floor_too() {
        signInAsAdministrator();
        page.setViewportSize(360, 900);

        for (String route : java.util.List.of("/anlaesse", "/volontariat")) {
            page.navigate(url(route));
            String offenders = (String) page.evaluate(SMALLEST_RENDERED_TEXT, FLOOR_PX - TOLERANCE_PX);

            assertTrue(offenders.isEmpty(),
                    () -> route + " renders maintenance text below " + FLOOR_PX
                            + " px (NFR-013):\n  " + offenders);
        }
    }

    /**
     * The floor holds because the type scale's own floor is 14 px, so it is worth asserting the
     * scale directly as well: a page could pass above merely by not using the smallest step today.
     */
    @ParameterizedTest
    @ValueSource(ints = {320, 360, 1920})
    void every_step_of_the_type_scale_stays_above_the_floor(int width) {
        page.setViewportSize(width, 900);
        page.navigate(url("/"));

        String tooSmall = (String) page.evaluate("""
                (floor) => {
                  const probe = document.createElement('span');
                  probe.textContent = 'x';
                  document.body.appendChild(probe);
                  const bad = [];
                  for (const step of ['--step--1', '--step-0', '--step-1', '--step-2', '--step-3', '--step-4']) {
                    probe.style.fontSize = `var(${step})`;
                    const size = parseFloat(getComputedStyle(probe).fontSize);
                    if (size < floor) bad.push(`${step} resolves to ${size.toFixed(2)}px`);
                  }
                  probe.remove();
                  return bad.join('\\n  ');
                }
                """, FLOOR_PX - TOLERANCE_PX);

        assertTrue(tooSmall.isEmpty(),
                () -> "the type scale drops below " + FLOOR_PX + " px at " + width + " px (NFR-013):\n  " + tooSmall);
    }
}
