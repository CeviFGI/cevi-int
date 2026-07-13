package tools.cevi.e2e;

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

    @Test
    public void hamburger_menu_toggles_navigation() {
        // nav.css only shows the hamburger below the 768px breakpoint; above that the nav is
        // permanently visible instead.
        page.setViewportSize(375, 667);
        page.navigate(url("/anlaesse"));

        var sideMenu = page.locator("input#side-menu");
        assertThat(sideMenu.isChecked(), is(false));

        page.locator("label.hamb").click();
        assertThat(sideMenu.isChecked(), is(true));

        page.locator("label.hamb").click();
        assertThat(sideMenu.isChecked(), is(false));
    }
}
