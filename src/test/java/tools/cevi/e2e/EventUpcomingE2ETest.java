package tools.cevi.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tools.cevi.event.Event;
import tools.cevi.fixture.EventFixture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

/**
 * UC-001 Browse Upcoming Events: the event list in a real browser.
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

    /**
     * Main success scenario: the visitor opens the list, sees the event with its date text,
     * location, excerpt and remaining time, selects it and reads the whole description on its own
     * page, where its address is ready to be passed on.
     */
    @Test
    public void visitor_compares_the_events_and_reads_the_chosen_one() {
        String title = "CLEANUP e2e browse";
        String tail = "SCHLUSSDESTEXTES";
        long id = EventFixture.createEvent(title, LocalDate.now().plusDays(7),
                "1. - 8. September 2099", "Lissabon",
                "<p>" + "Programm ".repeat(40) + "<strong>" + tail + "</strong></p>");
        String slug = ((Event) Event.findById(id)).slug;

        // Steps 1-3
        page.navigate(url("/anlaesse"));
        Locator card = page.locator("article.card").filter(new Locator.FilterOptions().setHasText(title));
        assertThat(card.isVisible(), is(true));
        assertThat(card.innerText(), containsString("1. - 8. September 2099"));
        assertThat(card.innerText(), containsString("Lissabon"));
        assertThat(card.innerText(), containsString("Noch 7 Tage"));
        assertThat(card.locator(".card__excerpt").innerText(), containsString("Programm"));
        assertThat(card.getByText(tail).count(), is(0));

        // Steps 4-5
        card.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(title)).click();
        page.waitForURL("**/anlaesse/detail?slug=" + slug);
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title)).isVisible(), is(true));
        assertThat(page.locator(".prose strong").innerText(), is(tail));

        // Step 6: the address of the event, to pass it on
        assertThat(page.locator("#share-address").inputValue(), endsWith("/anlaesse/detail?slug=" + slug));
    }
}
