package tools.cevi.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.cevi.fixture.EventFixture;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * UC-013 Subscribe to the News Feed in a real browser: a visitor finds the News Feed on a public
 * page, the address it names delivers the feed a reader subscribes to, and a Feed Item leads back to
 * the Event Page. What the feed holds in detail is {@code NewsFeedTest}.
 */
@QuarkusTest
public class NewsFeedE2ETest extends PlaywrightTestBase {

    private final String title = "CLEANUP UC-013 e2e " + UUID.randomUUID();

    private static Document parse(String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
    }

    /** The link of the Feed Item with the given title, as a feed reader would open it. */
    private static String linkOfItem(Document feed, String itemTitle) {
        NodeList items = feed.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            if (item.getElementsByTagName("title").item(0).getTextContent().equals(itemTitle)) {
                return item.getElementsByTagName("link").item(0).getTextContent();
            }
        }
        throw new AssertionError("no Feed Item titled " + itemTitle);
    }

    /** Main success scenario steps 1–6, A1, BR-001: find, fetch and follow the News Feed. */
    @Test
    public void a_visitor_finds_the_news_feed_and_follows_an_item_to_its_event_page() throws Exception {
        EventFixture.createEvent(title, LocalDate.now().plusDays(7));

        // Step 1, BR-001: the visible link in the footer of a public page
        page.navigate(url("/volontariat"));
        Locator footerLink = page.locator(".site-footer")
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("RSS-Feed"));
        assertThat(footerLink).isVisible();
        String visibleAddress = URI.create(page.url()).resolve(footerLink.getAttribute("href")).toString();

        // A1, BR-001: the same address, named to the browser and the feed reader in the page head
        Locator alternate = page.locator("head link[rel='alternate'][type='application/rss+xml']");
        assertThat(alternate).hasCount(1);
        String discoveredAddress = URI.create(page.url()).resolve(alternate.getAttribute("href")).toString();
        assertThat(discoveredAddress, equalTo(visibleAddress));
        assertThat(visibleAddress, equalTo(url("/feed.xml")));

        // Steps 2–4: the feed reader fetches the News Feed under that address
        APIResponse response = page.request().get(visibleAddress);
        assertThat(response.status(), equalTo(200));
        assertThat(response.headers().get("content-type"), startsWith("application/rss+xml"));
        String itemLink = linkOfItem(parse(response.text()), title);

        // Steps 5–6: opening the Feed Item presents the Event Page under its Public Address
        page.navigate(itemLink);
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title))).isVisible();
    }
}
