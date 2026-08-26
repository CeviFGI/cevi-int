package tools.cevi.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The navigation, which is the one part of the site with a state — and it has that state without a
 * line of JavaScript, because it is a checkbox and a label (C-021, NFR-024).
 *
 * <p>That mechanism is why it needs a keyboard test specifically. A {@code <label>} is not
 * focusable; the checkbox behind it is, and it is parked off-screen so it cannot be seen. If the
 * off-screen rule ever became {@code display: none} the drawer would keep working for a mouse and
 * stop existing for a keyboard, and nothing else in the suite would notice.
 *
 * <p>The two mouse cases used to live in {@code EventUpcomingE2ETest}, which was about the event
 * list and had inherited them for want of a better home. They are here now with the rest of the
 * navigation.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class NavigationDrawerE2ETest extends PlaywrightTestBase {

    /** nav.css lays the navigation out as a row from 900 px; below that it is a drawer. */
    private static final int PHONE_WIDTH = 375;
    private static final int DESKTOP_WIDTH = 1280;

    private Locator navigationLink(String name) {
        // "Kontakt" also appears in the footer, which is always visible — the locator has to be
        // scoped to the navigation landmark or it would answer about the wrong link.
        return page.getByLabel("Hauptnavigation")
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(name));
    }

    @Test
    public void menu_button_opens_and_closes_the_navigation() {
        page.setViewportSize(PHONE_WIDTH, 667);
        page.navigate(url("/anlaesse"));

        Locator contact = navigationLink("Kontakt");
        Locator menuButton = page.locator("label.site-nav__button");

        assertThat(contact.isVisible(), is(false));

        menuButton.click();
        assertThat(contact.isVisible(), is(true));

        menuButton.click();
        assertThat(contact.isVisible(), is(false));
    }

    /** From 900 px the navigation is a row and needs no menu button at all. */
    @Test
    public void navigation_is_always_visible_on_a_wide_screen() {
        page.setViewportSize(DESKTOP_WIDTH, 800);
        page.navigate(url("/anlaesse"));

        assertThat(navigationLink("Kontakt").isVisible(), is(true));
        assertThat(page.locator("label.site-nav__button").isVisible(), is(false));
    }

    /**
     * The keyboard path, end to end: tab to the toggle, open it with Space, reach a link with Tab,
     * and close it again with Space. Nothing here touches the mouse (WCAG 2.1.1, NFR-014).
     */
    @Test
    public void the_drawer_opens_and_closes_from_the_keyboard_alone() {
        page.setViewportSize(PHONE_WIDTH, 667);
        page.navigate(url("/anlaesse"));

        Locator contact = navigationLink("Kontakt");
        Locator toggle = page.locator("#site-nav-toggle");

        toggle.focus();
        assertEquals("site-nav-toggle", activeElementId(),
                "the checkbox behind the menu label has to be focusable, or the drawer is mouse-only");

        page.keyboard().press("Space");
        assertThat(contact.isVisible(), is(true));

        // The brand link sits between the toggle and the panel in the document, so the drawer is
        // the second stop, not the first. What matters is that it is reachable at all — a panel
        // that opens but cannot be tabbed into is a drawer for the mouse only.
        page.keyboard().press("Tab");
        page.keyboard().press("Tab");
        assertTrue(insideTheNavigation(),
                "tabbing on from the toggle has to reach the drawer it just opened, "
                        + "but focus stopped at " + activeElementDescription());

        toggle.focus();
        page.keyboard().press("Space");
        assertThat(contact.isVisible(), is(false));
    }

    /**
     * A visitor has to be able to tell which page they are on without reading the address bar
     * (WCAG 2.4.8). The marker is {@code aria-current="page"}, which the stylesheet turns into the
     * red edge on a phone and the underline on a desktop.
     */
    @ParameterizedTest
    @CsvSource({"/anlaesse, Anlässe", "/volontariat, Volontariat", "/fgi, FGI", "/kontakt, Kontakt"})
    public void the_current_page_is_marked_in_the_navigation(String route, String label) {
        page.setViewportSize(DESKTOP_WIDTH, 800);
        page.navigate(url(route));

        assertEquals("page", navigationLink(label).getAttribute("aria-current"),
                label + " should be marked as the current page on " + route);

        String marked = (String) page.evaluate(
                "() => [...document.querySelectorAll('.site-nav__link[aria-current]')].length + ''");
        assertEquals("1", marked, "exactly one navigation item may be marked as current");
    }

    /**
     * Signing out changes state, so it is a form submission and not a link (NFR-021) — and it has
     * to keep looking and behaving like a menu row while it is one. The header is the only place
     * on the site where a {@code <button>} sits inside the navigation, which is why it is asserted
     * here rather than with the sign-in tests.
     */
    @Test
    public void signing_out_is_a_post_carrying_a_token_and_still_reads_as_a_menu_row() {
        page.setViewportSize(DESKTOP_WIDTH, 800);
        signIn();

        Locator form = page.locator("form.site-nav__form");
        assertEquals("post", form.getAttribute("method").toLowerCase(),
                "signing out must not be reachable by a GET (NFR-021)");
        assertEquals(1, form.locator("input[type=hidden]").count(),
                "the sign-out form must carry a CSRF token like every other state change");

        Locator control = form.getByRole(AriaRole.BUTTON);
        assertEquals("site-nav__link", control.getAttribute("class"),
                "the sign-out control has to wear the same row styling as the links beside it");

        control.click();
        page.waitForURL("**/auth/loggedOut**");
        assertTrue(page.getByLabel("Hauptnavigation").getByRole(AriaRole.BUTTON).count() == 0,
                "after signing out the maintenance row should be gone");
    }

    private void signIn() {
        page.navigate(url("/auth/login"));
        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("admin");
        page.locator(".form-actions button[type='submit']").click();
        page.waitForURL(url("/"));
    }

    private boolean insideTheNavigation() {
        return Boolean.TRUE.equals(page.evaluate(
                "() => document.activeElement.closest('.site-nav__panel') !== null"));
    }

    private String activeElementId() {
        return (String) page.evaluate("() => document.activeElement.id");
    }

    private String activeElementDescription() {
        return (String) page.evaluate(
                "() => document.activeElement.tagName + '.' + document.activeElement.className");
    }
}
