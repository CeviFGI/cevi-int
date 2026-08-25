package tools.cevi.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/** Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base class. */
@QuarkusTest
public class EventUpcomingE2ETest extends PlaywrightTestBase {

    @Test
    public void upcoming_events_page_renders_without_console_errors() {
        List<String> consoleErrors = new ArrayList<>();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                consoleErrors.add(message.text());
            }
        });

        page.navigate(url("/anlaesse"));

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Anlässe")).isVisible(), is(true));
        assertThat(consoleErrors, empty());
    }

    /**
     * The drawer is a checkbox and a label, with no JavaScript behind it (C-021). This asserts what
     * a visitor sees rather than the checkbox state: the navigation has to actually appear and
     * disappear, which a checked checkbox alone would not prove if the stylesheet stopped
     * reacting to it.
     */
    @Test
    public void menu_button_opens_and_closes_the_navigation() {
        // nav.css lays the navigation out as a row from 900px; below that it is a drawer.
        page.setViewportSize(375, 667);
        page.navigate(url("/anlaesse"));

        // "Kontakt" also appears in the footer, which is always visible — the locator has to be
        // scoped to the navigation landmark or it would answer about the wrong link.
        var contactLink = page.getByLabel("Hauptnavigation")
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Kontakt"));
        var menuButton = page.locator("label.site-nav__button");

        assertThat(contactLink.isVisible(), is(false));

        menuButton.click();
        assertThat(contactLink.isVisible(), is(true));

        menuButton.click();
        assertThat(contactLink.isVisible(), is(false));
    }

    /** From 900px the navigation is a row and needs no menu button at all. */
    @Test
    public void navigation_is_always_visible_on_a_wide_screen() {
        page.setViewportSize(1280, 800);
        page.navigate(url("/anlaesse"));

        assertThat(page.getByLabel("Hauptnavigation")
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Kontakt"))
                .isVisible(), is(true));
        assertThat(page.locator("label.site-nav__button").isVisible(), is(false));
    }
}
