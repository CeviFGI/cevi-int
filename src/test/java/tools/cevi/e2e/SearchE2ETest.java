package tools.cevi.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.event.Event;
import tools.cevi.fixture.EventFixture;
import tools.cevi.voluntary.VoluntaryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * UC-010 Search Events and Offers in a real browser: the Live Results while typing (A1), the way
 * from a hit to the event or offer (step 5), and the search without script (BR-006). What the
 * server answers is covered by {@code SearchResourceTest}.
 */
@QuarkusTest
public class SearchE2ETest extends PlaywrightTestBase {

    private static final String EVENT = "CLEANUP Zqbrowser Workcamp";
    private static final String OFFER = "Zqbrowser Verein";

    private final List<Integer> offers = new ArrayList<>();

    @AfterEach
    void removeOffers() {
        QuarkusTransaction.begin();
        offers.forEach(id -> VoluntaryService.deleteById(id));
        QuarkusTransaction.commit();
        offers.clear();
    }

    private String anEvent() {
        long id = EventFixture.createEvent(EVENT, LocalDate.now().plusDays(3));
        return Event.<Event>findById(id).slug;
    }

    private int anOffer() {
        QuarkusTransaction.begin();
        VoluntaryService offer = new VoluntaryService();
        offer.organization = OFFER;
        offer.organizationLink = "https://example.org/";
        offer.location = "Lima";
        offer.description = "<p>" + "Ein Jahr in Peru mit vielen Aufgaben im Quartier. ".repeat(8)
                + "Ende der Beschreibung.</p>";
        offer.persist();
        QuarkusTransaction.commit();
        offers.add(offer.id);
        return offer.id;
    }

    private Locator field() {
        return page.locator("#site-search-q");
    }

    private Locator liveResults() {
        return page.locator("#site-search-live");
    }

    private Locator status() {
        return page.locator("[data-search-status]");
    }

    private void typeInSearchField(String text) {
        field().click();
        field().pressSequentially(text);
    }

    /**
     * A1 steps 1 and 2: after two characters and a pause, Live Results appear below the field —
     * events and offers with title and location, and a link to all results — and their number is
     * announced to assistive technology.
     */
    @Test
    void live_results_appear_while_typing_and_are_announced() {
        page.navigate(url("/"));
        anEvent();
        anOffer();

        typeInSearchField("zqbrowser");

        assertThat(liveResults()).isVisible();
        assertThat(liveResults()).containsText(EVENT);
        assertThat(liveResults()).containsText(OFFER);
        assertThat(liveResults()).containsText("Lima");
        assertThat(liveResults().getByText("Alle Treffer anzeigen")).isVisible();
        assertThat(status()).hasText("2 Treffer");
    }

    /** A1 step 3: fewer than two characters other than spaces remove the Live Results. */
    @Test
    void live_results_disappear_below_two_characters() {
        page.navigate(url("/"));
        anEvent();
        typeInSearchField("zqbrowser");
        assertThat(liveResults()).isVisible();

        field().fill("z ");

        assertThat(liveResults()).isHidden();
        assertThat(status()).hasText("");
    }

    /** A1 step 3: typing on replaces the Live Results with those for the changed Search Term. */
    @Test
    void live_results_follow_the_changed_search_term() {
        page.navigate(url("/"));
        anEvent();
        anOffer();
        typeInSearchField("zqbrowser");
        assertThat(status()).hasText("2 Treffer");

        field().press("End");
        field().pressSequentially(" lima");

        assertThat(status()).hasText("1 Treffer");
        assertThat(liveResults()).containsText(OFFER);
        assertThat(liveResults()).not().containsText(EVENT);
    }

    /** A1 step 3: leaving the search field removes the Live Results. */
    @Test
    void leaving_the_search_field_removes_the_live_results() {
        page.navigate(url("/anlaesse"));
        anEvent();
        typeInSearchField("zqbrowser");
        assertThat(liveResults()).isVisible();

        field().blur();

        assertThat(liveResults()).isHidden();
    }

    /** A1 step 2: the arrow keys move through the Live Results, Escape closes them. */
    @Test
    void the_keyboard_moves_through_the_live_results() {
        page.navigate(url("/"));
        anEvent();
        anOffer();
        typeInSearchField("zqbrowser");
        Locator links = liveResults().locator("a");
        assertThat(links).hasCount(3);

        field().press("ArrowDown");
        assertThat(links.nth(0)).isFocused();
        page.keyboard().press("ArrowDown");
        assertThat(links.nth(1)).isFocused();
        page.keyboard().press("ArrowUp");
        page.keyboard().press("ArrowUp");
        assertThat(field()).isFocused();

        page.keyboard().press("Escape");
        assertThat(liveResults()).isHidden();
        assertThat(field()).isFocused();
    }

    /** A1 step 4 and step 5: picking an event among the Live Results opens its Event Page. */
    @Test
    void picking_a_live_event_opens_its_event_page() {
        page.navigate(url("/"));
        String slug = anEvent();
        typeInSearchField("zqbrowser");

        liveResults().getByText(EVENT).click();

        page.waitForURL(Pattern.compile(".*/anlaesse/detail\\?slug=" + slug));
        assertThat(page.locator("h1")).hasText(EVENT);
    }

    /**
     * Step 5: picking an offer leads to its entry in the Offer List — brought into view, marked as
     * the one chosen, with its complete Description open.
     */
    @Test
    void picking_an_offer_shows_its_marked_and_opened_entry() {
        page.setViewportSize(360, 740);
        page.navigate(url("/"));
        int id = anOffer();
        typeInSearchField("zqbrowser");

        liveResults().getByText(OFFER).click();

        page.waitForURL(Pattern.compile(".*/volontariat\\?angebot=" + id + "#angebot-" + id));
        Locator card = page.locator("#angebot-" + id);
        assertThat(card).hasClass(Pattern.compile("card--chosen"));
        assertThat(card).containsText("Dein Suchtreffer");
        assertThat(card.locator("details")).hasAttribute("open", "");
        assertThat(card.getByText("Ende der Beschreibung.")).isVisible();
        assertThat(card).isInViewport();
        double top = ((Number) card.evaluate("e => e.getBoundingClientRect().top")).doubleValue();
        double header = ((Number) page.locator(".site-header").evaluate("e => e.getBoundingClientRect().bottom")).doubleValue();
        assertThat("the sticky header does not cover the chosen entry", top >= header, is(true));
    }

    /** A1 step 4: starting the search instead of picking a Live Result opens the Search Results Page. */
    @Test
    void starting_the_search_opens_the_search_results_page() {
        page.navigate(url("/"));
        anEvent();
        typeInSearchField("zqbrowser");

        field().press("Enter");

        page.waitForURL(Pattern.compile(".*/suche\\?q=zqbrowser"));
        assertThat(page.locator("h1")).hasText("Suche nach «zqbrowser»");
        assertThat(field()).hasValue("zqbrowser");
    }

    /**
     * BR-006: without script, entering a Search Term, starting the search, reading the Search
     * Results Page and following a hit all work; there are simply no Live Results.
     */
    @Test
    void the_search_works_without_script() {
        String slug = anEvent();
        Browser browser = context.browser();
        try (BrowserContext noScript = browser.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false))) {
            Page plain = noScript.newPage();
            plain.navigate(url("/volontariat"));

            plain.locator("#site-search-q").fill("zqbrowser");
            plain.locator("#site-search-q").press("Enter");

            plain.waitForURL(Pattern.compile(".*/suche\\?q=zqbrowser"));
            assertThat(plain.locator("#site-search-live")).isHidden();
            assertThat(plain.locator("h1")).hasText("Suche nach «zqbrowser»");
            plain.getByText(EVENT).click();
            plain.waitForURL(Pattern.compile(".*/anlaesse/detail\\?slug=" + slug));
            assertThat(plain.locator("h1")).hasText(EVENT);
        }
    }

    /** BR-007: the search field is in the header, reachable without navigating, on a phone too. */
    @Test
    void the_search_field_is_visible_in_the_header_on_a_phone() {
        page.setViewportSize(360, 740);
        for (String route : List.of("/", "/anlaesse", "/volontariat", "/kontakt")) {
            page.navigate(url(route));

            assertThat(page.locator(".site-header #site-search-q")).isVisible();
        }
    }
}
