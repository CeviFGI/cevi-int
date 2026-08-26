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

/**
 * The event list in a real browser.
 *
 * <p>The two navigation cases this class used to carry moved to {@link NavigationDrawerE2ETest} in
 * phase 5: they were about the header, which every page shares, and had ended up here only because
 * this was the first e2e test to exist.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
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
}
