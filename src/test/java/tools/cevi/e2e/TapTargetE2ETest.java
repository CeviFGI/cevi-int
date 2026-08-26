package tools.cevi.e2e;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFR-038: on a phone every control is big enough to hit with a thumb.
 *
 * <p>44 × 44 CSS pixels is the WCAG 2.5.5 figure and the one {@code --tap-target} encodes. The
 * measurement is the rendered box, not the token: a rule that sets {@code min-height} on an
 * element the browser lays out inline changes nothing, and only a browser can tell the difference.
 *
 * <p>Two exemptions, both from WCAG 2.5.5 itself and both listed rather than filtered silently:
 * a link inside a sentence (making it 44 px tall would break the line it sits in), and a control
 * that is not on screen — the navigation checkbox, the honeypot, the skip link. Everything else
 * is measured.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class TapTargetE2ETest extends PlaywrightTestBase {

    private static final int PHONE_WIDTH = 360;
    private static final double MINIMUM_PX = 44.0;

    /**
     * Sub-pixel rounding again: a 2.75rem box measures 44 px, but a border or a fractional line
     * height can leave it at 43.98.
     */
    private static final double TOLERANCE_PX = 0.5;

    private static final String UNDERSIZED_CONTROLS = """
            (minimum) => {
              const inline = element =>
                  element.tagName === 'A'
                  && getComputedStyle(element).display === 'inline'
                  && element.closest('p, li, .prose, .site-footer__base, dd') !== null;

              // A card title is a link whose ::after is stretched over the whole card, so the
              // thumb aims at the card and not at the two lines of text. Measuring the anchor
              // itself would report a defect the visitor cannot experience — and, worse, would
              // invite someone to "fix" it by padding the title until the card fell apart.
              const targetBox = element => {
                const after = getComputedStyle(element, '::after');
                const stretched = after.content !== 'none' && after.position === 'absolute'
                    && after.inset === '0px' && element.offsetParent !== null;
                return stretched ? element.offsetParent.getBoundingClientRect()
                                 : element.getBoundingClientRect();
              };

              const offenders = new Map();
              for (const element of document.querySelectorAll('a[href], button, summary, input, select, textarea')) {
                const style = getComputedStyle(element);
                if (style.display === 'none' || style.visibility === 'hidden') continue;
                if (element.type === 'hidden') continue;
                if (element.closest('.visually-hidden, .skip-link, [hidden]')) continue;

                const box = targetBox(element);
                // Off-screen by design: the navigation checkbox and the honeypot are both 1px or
                // parked outside the viewport, and neither is ever aimed at.
                if (box.width <= 1 || box.height <= 1) continue;
                if (box.right < 0 || box.bottom < 0 || box.left > innerWidth) continue;

                if (inline(element)) continue;
                if (box.width + 0.0001 >= minimum && box.height + 0.0001 >= minimum) continue;

                const where = element.tagName.toLowerCase()
                    + (element.className && typeof element.className === 'string'
                        ? '.' + element.className.trim().split(/\\s+/).join('.') : '')
                    + ` "${(element.textContent || element.value || '').trim().slice(0, 30)}"`;
                offenders.set(where, `${box.width.toFixed(1)}x${box.height.toFixed(1)}`);
              }
              return [...offenders].map(([where, size]) => `${where} measures ${size}`).join('\\n  ');
            }
            """;

    @Test
    void every_control_on_a_phone_is_at_least_44_by_44() {
        page.setViewportSize(PHONE_WIDTH, 800);

        for (String route : PublicPages.ROUTES) {
            page.navigate(url(route));
            openEverythingThatCollapses();

            String offenders = (String) page.evaluate(UNDERSIZED_CONTROLS, MINIMUM_PX - TOLERANCE_PX);

            assertTrue(offenders.isEmpty(),
                    () -> route + " has controls under " + MINIMUM_PX + " px at " + PHONE_WIDTH
                            + " px (NFR-038):\n  " + offenders);
        }
    }

    /** The chrome an administrator sees, which no visitor and therefore no reviewer ever opens. */
    @Test
    void the_maintenance_controls_are_at_least_44_by_44_too() {
        signInAsAdministrator();
        page.setViewportSize(PHONE_WIDTH, 800);

        for (String route : List.of("/anlaesse", "/volontariat")) {
            page.navigate(url(route));
            openEverythingThatCollapses();

            String offenders = (String) page.evaluate(UNDERSIZED_CONTROLS, MINIMUM_PX - TOLERANCE_PX);
            assertTrue(offenders.isEmpty(),
                    () -> route + " has maintenance controls under " + MINIMUM_PX
                            + " px at " + PHONE_WIDTH + " px (NFR-038):\n  " + offenders);
        }
    }

    /**
     * The navigation is a drawer at this width and the offer descriptions sit in a
     * {@code <details>}. A control nobody can see is a control nobody measured, which is how an
     * undersized menu row survives a test like this one.
     */
    @Test
    void the_controls_hidden_behind_a_disclosure_are_measured_too() {
        page.setViewportSize(PHONE_WIDTH, 800);
        page.navigate(url("/volontariat"));

        page.locator("label.site-nav__button").click();
        openEverythingThatCollapses();

        assertTrue(page.getByLabel("Hauptnavigation").locator("a").first().isVisible(),
                "the drawer should be open, or this test measures nothing that was hidden");

        String offenders = (String) page.evaluate(UNDERSIZED_CONTROLS, MINIMUM_PX - TOLERANCE_PX);
        assertTrue(offenders.isEmpty(),
                () -> "controls revealed by the drawer or a disclosure are under " + MINIMUM_PX
                        + " px (NFR-038):\n  " + offenders);
    }

    private void openEverythingThatCollapses() {
        page.evaluate("() => document.querySelectorAll('details').forEach(d => d.open = true)");
    }
}
