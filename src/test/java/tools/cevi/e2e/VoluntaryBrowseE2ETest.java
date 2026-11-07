package tools.cevi.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.VoluntaryFixture;
import tools.cevi.voluntary.VoluntaryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

/**
 * UC-003 Browse Voluntary Service Offers in a real browser: what only a browser can show — that
 * selecting anywhere on an entry opens the Offer Page (BR-003) and that the control leading to the
 * Organisation opens it separately, so the Offer Page stays (step 6).
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class VoluntaryBrowseE2ETest extends PlaywrightTestBase {

    /**
     * Main success scenario: the visitor selects an entry anywhere rather than on its name,
     * reads the complete offer on its Offer Page and follows the control to the Organisation,
     * which opens in a page of its own.
     */
    @Test
    public void visitor_reads_the_offer_and_reaches_the_organisation_separately() {
        String organization = "CLEANUP UC-003 e2e";
        String tail = "SCHLUSSDESANGEBOTS";
        long id = VoluntaryFixture.createVoluntaryService(organization,
                "<p>" + "Einsatz ".repeat(40) + "<strong>" + tail + "</strong></p>", "https://example.org/");
        String slug = QuarkusTransaction.requiringNew().call(() -> VoluntaryService.<VoluntaryService>findById((int) id).slug);

        // Steps 1-3: the list shows the Excerpt only; a click in the middle of the entry, away
        // from its name, opens the Offer Page (BR-003)
        page.navigate(url("/volontariat"));
        Locator card = page.locator("article.card").filter(new Locator.FilterOptions().setHasText(organization));
        assertThat(card.getByText(tail).count(), is(0));
        card.click();

        // Step 4
        page.waitForURL("**/volontariat/detail?slug=" + slug);
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(organization)).isVisible(), is(true));
        assertThat(page.locator(".prose strong").innerText(), is(tail));

        // Steps 5-6: the Organisation opens separately; the Offer Page stays where it was. The
        // target host is never contacted: the browser is stopped at the request.
        page.context().route("https://example.org/**", route -> route.fulfill(
                new com.microsoft.playwright.Route.FulfillOptions().setStatus(200).setBody("Organisation")));
        Page organisation = page.waitForPopup(() ->
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Zur Organisation")).click());
        organisation.waitForLoadState();
        assertThat(organisation.url(), is("https://example.org/"));
        assertThat(page.url(), endsWith("/volontariat/detail?slug=" + slug));
        organisation.close();
    }
}
