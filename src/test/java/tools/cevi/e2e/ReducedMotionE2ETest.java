package tools.cevi.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ReducedMotion;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFR-040: when the visitor's system asks for less motion, the site stops moving.
 *
 * <p>Every animation here is decorative — the card lift, the burger folding into a cross, the
 * underline sliding under the current navigation item, the skip link dropping in — so honouring the
 * request costs nothing at all. That is exactly why the rule tends to be written once and then
 * quietly outgrown: a transition added to a new component three months later is not covered by
 * anything unless something checks.
 *
 * <p>{@code base.css} answers with a blanket rule rather than per-component overrides, so this test
 * asks the question the same way — no rendered element may still be carrying a perceptible
 * transition or animation.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class ReducedMotionE2ETest extends PlaywrightTestBase {

    /**
     * The blanket rule sets 0.01 ms rather than 0, because a hard zero never fires
     * {@code transitionend}. Anything above a millisecond is real motion.
     */
    private static final double PERCEPTIBLE_MS = 1.0;

    private static final String THINGS_STILL_MOVING = """
            (limit) => {
              const milliseconds = value => value.split(',')
                  .map(part => part.trim())
                  .map(part => part.endsWith('ms') ? parseFloat(part)
                                                   : parseFloat(part) * 1000)
                  .reduce((longest, one) => Math.max(longest, one || 0), 0);

              const moving = new Map();
              for (const element of document.querySelectorAll('body *')) {
                const box = element.getBoundingClientRect();
                if (box.width === 0 || box.height === 0) continue;

                for (const [property, value] of [['transition', getComputedStyle(element).transitionDuration],
                                                 ['animation', getComputedStyle(element).animationDuration]]) {
                  const longest = milliseconds(value);
                  if (longest <= limit) continue;
                  const where = element.tagName.toLowerCase()
                      + (element.className && typeof element.className === 'string'
                          ? '.' + element.className.trim().split(/\\s+/).join('.') : '');
                  moving.set(where + ' (' + property + ')', longest);
                }
              }
              return [...moving].map(([where, ms]) => `${where} still runs for ${ms}ms`).join('\\n  ');
            }
            """;

    @Test
    void nothing_animates_when_the_visitor_asks_for_less_motion() {
        page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
        page.setViewportSize(1280, 900);

        for (String route : PublicPages.ROUTES) {
            page.navigate(url(route));
            String moving = (String) page.evaluate(THINGS_STILL_MOVING, PERCEPTIBLE_MS);

            assertTrue(moving.isEmpty(),
                    () -> route + " keeps moving under prefers-reduced-motion (NFR-040):\n  " + moving);
        }
    }

    /**
     * And the counter-check, or the test above would pass just as happily on a site that had no
     * transitions left to disable.
     */
    @Test
    void the_same_pages_do_animate_when_nobody_asked_for_less() {
        page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.NO_PREFERENCE));
        page.setViewportSize(1280, 900);
        page.navigate(url("/anlaesse"));

        String moving = (String) page.evaluate(THINGS_STILL_MOVING, PERCEPTIBLE_MS);
        assertTrue(!moving.isEmpty(),
                "the event list should carry transitions by default, or the assertion above proves nothing");
    }

    /**
     * Nothing is only conveyed by movement (NFR-040, WCAG 2.2.2): with motion off, the drawer still
     * opens and the current page is still marked.
     */
    @Test
    void the_interface_still_works_with_every_transition_off() {
        page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
        page.setViewportSize(375, 667);
        page.navigate(url("/anlaesse"));

        page.locator("label.site-nav__button").click();
        assertTrue(page.getByLabel("Hauptnavigation")
                        .getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                                new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Kontakt"))
                        .isVisible(),
                "the drawer has to open without the animation that normally accompanies it");

        assertTrue(page.locator(".site-nav__link[aria-current]").count() == 1,
                "the current page stays marked when nothing moves");
    }
}
