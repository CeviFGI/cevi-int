package tools.cevi.e2e;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * UC-008 Enter the Platform in the browser. The start page exists for one visitor: the one who
 * tapped a link in a chat group and has not chosen this site from a menu. What that visitor gets in the first screen is
 * therefore the thing worth testing, and it can only be measured in a browser.
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class StartPageE2ETest extends PlaywrightTestBase {

    /** The smallest phone still in use — if it works here it works everywhere above. */
    private static final int PHONE_WIDTH = 360;
    private static final int PHONE_HEIGHT = 640;

    /** Main success scenario step 2, BR-001: the platform says what it is within the first screen of a phone. */
    @Test
    public void the_hero_says_what_the_site_is_without_scrolling() {
        page.setViewportSize(PHONE_WIDTH, PHONE_HEIGHT);
        page.navigate(url("/"));

        Locator title = page.locator(".hero__title");
        assertThat(title.isVisible(), is(true));

        double bottom = (double) (Double) title.evaluate(
                "element => element.getBoundingClientRect().bottom + 0.0");
        assertThat("the headline should sit above the fold at " + PHONE_HEIGHT + " px",
                bottom < PHONE_HEIGHT, is(true));
    }

    /** Main success scenario steps 3-4, BR-002: each extract names the way to its complete list. */
    @Test
    public void both_teasers_lead_to_the_complete_list() {
        page.setViewportSize(PHONE_WIDTH, PHONE_HEIGHT);
        page.navigate(url("/"));

        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Alle Anlässe")).click();
        page.waitForURL("**/anlaesse");
        assertThat(page.url(), containsString("/anlaesse"));

        page.navigate(url("/"));
        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Alle Angebote")).click();
        page.waitForURL("**/volontariat");
        assertThat(page.url(), containsString("/volontariat"));
    }
}
