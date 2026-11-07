package tools.cevi.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * UC-007 View Site Information, as a visitor sees it in a browser: the information pages are found
 * from the navigation, and on a phone a channel group of the working group's page can be collapsed
 * and opened again without a script.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class SiteInformationE2ETest extends PlaywrightTestBase {

    private static final int PHONE_WIDTH = 375;
    private static final int DESKTOP_WIDTH = 1280;

    /**
     * Main success scenario with BR-001 and BR-002: from an ordinary page the visitor chooses the
     * data protection information, then the working group, from the navigation.
     */
    @Test
    public void the_visitor_reaches_both_information_pages_from_the_navigation() {
        page.setViewportSize(DESKTOP_WIDTH, 800);
        page.navigate(url("/anlaesse"));

        page.locator("footer").getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("Datenschutzinformation")).click();
        page.waitForURL("**/datenschutzinformation");
        assertThat(page.getByRole(AriaRole.HEADING,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Datenschutzinformation").setLevel(1)))
                .isVisible();

        page.getByLabel("Hauptnavigation").getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("FGI")).click();
        page.waitForURL("**/fgi");
        assertThat(page.getByRole(AriaRole.HEADING,
                new com.microsoft.playwright.Page.GetByRoleOptions().setLevel(1)))
                .containsText("Fachgruppe International");
    }

    /**
     * Main success scenario, steps 3–4, and BR-004: on a phone the groups are open on arrival; the
     * visitor jumps to one channel from the index and can collapse it and open it again.
     */
    @Test
    public void a_channel_group_is_open_on_arrival_and_collapsible() {
        page.setViewportSize(PHONE_WIDTH, 667);
        page.navigate(url("/fgi"));

        Locator newsletter = page.locator("details#newsletter");
        assertThat(newsletter).hasAttribute("open", "");

        page.getByLabel("Kanäle").getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("Newsletter")).click();
        assertThat(newsletter.locator("summary")).isInViewport();

        newsletter.locator("summary").click();
        assertThat(newsletter).not().hasAttribute("open", "");

        newsletter.locator("summary").click();
        assertThat(newsletter).hasAttribute("open", "");
    }

    /** A2: an unknown address shows a page that says so, and its button leads back to the site. */
    @Test
    public void an_unknown_address_offers_the_way_back() {
        page.navigate(url("/gibt-es-nicht"));

        assertThat(page.getByRole(AriaRole.HEADING,
                new com.microsoft.playwright.Page.GetByRoleOptions().setLevel(1)))
                .hasText("Diese Seite gibt es nicht");

        page.locator("main").getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("Zu den Anlässen")).click();
        page.waitForURL("**/anlaesse");
    }
}
