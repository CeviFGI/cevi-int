package tools.cevi.e2e;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFR-039 and WCAG 2.4.7: whatever the keyboard lands on, the visitor can see where it is.
 *
 * <p>The ring is one rule in {@code base.css} and nothing is supposed to remove it — which is
 * precisely the kind of invariant that survives until someone adds {@code outline: none} to quieten
 * a focus ring they found ugly on a mouse click. The rule is written on {@code :focus-visible} so
 * that a mouse click leaves nothing behind; pressing Tab is what makes the browser apply it, so
 * this test has to be a real Tab press rather than a scripted {@code .focus()}.
 *
 * <p>A ring can also be drawn with {@code box-shadow} instead of {@code outline} — the header's
 * menu label does the opposite and borrows the outline — so both count.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class FocusVisibleE2ETest extends PlaywrightTestBase {

    /** Enough to walk past the header and into the content of any page on the site. */
    private static final int STOPS = 40;

    static List<String> routes() {
        return PublicPages.ROUTES;
    }

    /**
     * Reads the indicator on whatever currently holds focus. Returns a description of the element
     * when it has none, and an empty string when it is fine — so the assertion message names the
     * offender rather than a stop number.
     */
    private static final String UNMARKED_FOCUS = """
            () => {
              const element = document.activeElement;
              if (!element || element === document.body) return '';
              const style = getComputedStyle(element);

              const outlined = style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) >= 1;
              const shadowed = style.boxShadow !== 'none';
              // The label of the navigation checkbox carries the ring on the visitor's behalf,
              // because the checkbox that actually receives focus is parked off-screen.
              const label = document.querySelector('label.site-nav__button');
              const borrowed = element.id === 'site-nav-toggle' && label !== null
                  && label.getClientRects().length > 0
                  && getComputedStyle(label).outlineStyle !== 'none';

              if (outlined || shadowed || borrowed) return '';
              return element.tagName.toLowerCase()
                  + (element.className && typeof element.className === 'string'
                      ? '.' + element.className.trim().split(/\\s+/).join('.') : '')
                  + ` "${(element.textContent || element.value || '').trim().slice(0, 30)}"`;
            }
            """;

    @ParameterizedTest
    @MethodSource("routes")
    void tabbing_through_a_page_never_lands_on_an_invisible_focus(String route) {
        page.setViewportSize(1280, 900);
        page.navigate(url(route));

        for (int stop = 0; stop < STOPS; stop++) {
            page.keyboard().press("Tab");
            String unmarked = (String) page.evaluate(UNMARKED_FOCUS);
            final int reached = stop + 1;
            assertTrue(unmarked.isEmpty(),
                    () -> route + " stop " + reached + ": " + unmarked
                            + " takes focus without showing it (NFR-039, WCAG 2.4.7)");
        }
    }

    /**
     * The skip link only skips something if it is the first thing the keyboard reaches, and it
     * only reads as a link if it becomes visible when it does (WCAG 2.4.1, NFR-043).
     */
    @ParameterizedTest
    @MethodSource("routes")
    void the_skip_link_is_the_first_stop_and_shows_itself(String route) {
        page.setViewportSize(1280, 900);
        page.navigate(url(route));

        page.keyboard().press("Tab");

        assertEquals("skip-link", page.evaluate("() => document.activeElement.className"),
                route + " should offer the skip link as the very first tab stop");

        // It slides in over --dur-fast; measured on the same tick it is still off the top edge.
        page.waitForTimeout(300);

        boolean onScreen = Boolean.TRUE.equals(page.evaluate("""
                () => {
                  const box = document.activeElement.getBoundingClientRect();
                  return box.top >= 0 && box.left >= 0 && box.height > 0;
                }
                """));
        assertTrue(onScreen, route + ": the skip link has to become visible when it takes focus");
    }

    /** Following it has to put the keyboard into the content, not merely scroll the page. */
    @Test
    void following_the_skip_link_moves_the_keyboard_into_the_content() {
        page.setViewportSize(1280, 900);
        page.navigate(url("/anlaesse"));

        page.keyboard().press("Tab");
        page.keyboard().press("Enter");
        page.keyboard().press("Tab");

        assertTrue(Boolean.TRUE.equals(page.evaluate(
                        "() => document.activeElement.closest('#inhalt') !== null")),
                "after the skip link the next stop has to be inside <main>, or the link skipped nothing");
    }
}
