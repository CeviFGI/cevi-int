package tools.cevi.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tools.cevi.e2e.ShareStandIns.Outcome;
import tools.cevi.event.Event;
import tools.cevi.fixture.EventFixture;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tools.cevi.e2e.ShareStandIns.NO_SHARE_DIALOG;
import static tools.cevi.e2e.ShareStandIns.REFUSING_CLIPBOARD;
import static tools.cevi.e2e.ShareStandIns.shareDialog;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * UC-009 Share an Event in a real browser.
 *
 * <p>The Share Dialog is a stand-in ({@link ShareStandIns}). The clipboard is the browser's own,
 * read back with the permission the context grants.
 */
@QuarkusTest
public class ShareEventE2ETest extends PlaywrightTestBase {

    private static final String TITLE = "CLEANUP Share Workcamp Ghana";

    private String slug;

    private String openEventPage(String initScript) {
        slug = Event.<Event>findById(EventFixture.createEvent(TITLE)).slug;
        if (initScript != null) {
            page.addInitScript(initScript);
        }
        page.navigate(url("/anlaesse/detail?slug=" + slug));
        return page.url();
    }

    private String publicAddress() {
        URI base = URI.create(url("/"));
        return base.getScheme() + "://" + base.getHost() + ":" + base.getPort()
                + "/anlaesse/detail?slug=" + slug;
    }

    private Locator shareButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Teilen"));
    }

    private Locator status() {
        return page.locator(".share [role=status]");
    }

    private Locator addressField() {
        return page.getByLabel("Link zu diesem Anlass");
    }

    private void allowClipboard() {
        context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    }

    private String clipboard() {
        return (String) page.evaluate("() => navigator.clipboard.readText()");
    }

    /** Collects every request that leaves the application's own host (BR-004). */
    private List<String> foreignRequests() {
        List<String> foreign = new ArrayList<>();
        String ownHost = URI.create(url("/")).getHost();
        page.onRequest(request -> {
            String host = URI.create(request.url()).getHost();
            if (host != null && !host.equals(ownHost)) {
                foreign.add(request.url());
            }
        });
        return foreign;
    }

    /**
     * Collects every request the page makes from now on, to its own host included, so that a
     * share that reports itself back to the platform shows up (BR-004).
     */
    private List<String> requestsFromNowOn() {
        List<String> requests = new ArrayList<>();
        page.onRequest(request -> requests.add(request.method() + " " + request.url()));
        return requests;
    }

    /**
     * Main success scenario, BR-001, BR-002 and BR-004: a browser that offers a Share Dialog opens
     * it, filled with the title, the date text and the Public Address; afterwards the visitor is on
     * the Event Page as before, no confirmation is shown, and the share issued no request at all —
     * nothing is recorded, no other host is contacted.
     */
    @Test
    void the_share_dialog_receives_title_date_text_and_public_address() {
        List<String> foreign = foreignRequests();
        String before = openEventPage(shareDialog(Outcome.SENT));
        List<String> requests = requestsFromNowOn();

        shareButton().click();

        page.waitForFunction("() => window.__shared.length === 1");
        @SuppressWarnings("unchecked")
        Map<String, Object> shared = (Map<String, Object>) page.evaluate("() => window.__shared[0]");
        assertThat(shared.get("title"), is(TITLE));
        assertThat(shared.get("text"), is(TITLE + ", some date"));
        assertThat(shared.get("url"), is(publicAddress()));
        assertThat(page.url(), is(before));
        assertThat(status()).isEmpty();
        assertThat(foreign, empty());
        assertThat(requests, empty());
    }

    /**
     * BR-003: the shared address is the Public Address, even when the address the browser shows
     * carries parameters and a fragment.
     */
    @Test
    void the_shared_address_drops_parameters_and_fragment() {
        slug = Event.<Event>findById(EventFixture.createEvent(TITLE)).slug;
        page.addInitScript(shareDialog(Outcome.SENT));
        page.navigate(url("/anlaesse/detail?utm_source=chat&slug=" + slug + "#programm"));

        shareButton().click();

        page.waitForFunction("() => window.__shared.length === 1");
        assertThat((String) page.evaluate("() => window.__shared[0].url"), is(publicAddress()));
    }

    /**
     * A1, BR-002, BR-004 and BR-006: without a Share Dialog the Public Address alone lands on the
     * clipboard, the copy is confirmed beside the control as a status message, and the
     * confirmation disappears on its own. Copying issues no request at all.
     */
    @Test
    void without_a_share_dialog_the_address_is_copied_and_confirmed() {
        allowClipboard();
        List<String> foreign = foreignRequests();
        // A controlled clock, so the five seconds of BR-006 pass without the test waiting them out.
        page.clock().install();
        openEventPage(NO_SHARE_DIALOG);
        List<String> requests = requestsFromNowOn();

        shareButton().click();

        assertThat(status()).hasText("Link kopiert");
        assertThat(clipboard(), is(publicAddress()));
        assertThat(addressField()).isHidden();
        page.clock().runFor(4_000);
        assertThat(status()).hasText("Link kopiert");
        page.clock().runFor(1_000);
        assertThat(status()).isEmpty();
        assertThat(foreign, empty());
        assertThat(requests, empty());
    }

    /**
     * BR-001 and A1: a Share Dialog that fails for a reason other than cancelling falls back to
     * the clipboard.
     */
    @Test
    void a_failing_share_dialog_falls_back_to_the_clipboard() {
        allowClipboard();
        openEventPage(shareDialog(Outcome.FAILED));

        shareButton().click();

        assertThat(status()).hasText("Link kopiert");
        assertThat(clipboard(), is(publicAddress()));
    }

    /** A3 and BR-006: a cancelled Share Dialog is answered with nothing and copies nothing. */
    @Test
    void a_cancelled_share_dialog_leaves_the_page_unchanged() {
        allowClipboard();
        String before = openEventPage(shareDialog(Outcome.CANCELLED));
        page.evaluate("() => navigator.clipboard.writeText('untouched')");

        shareButton().click();

        page.waitForFunction("() => window.__shared.length === 1");
        assertThat(page.url(), is(before));
        assertThat(status()).isEmpty();
        assertThat(addressField()).isHidden();
        assertThat(page.locator(".share__hint")).isHidden();
        assertThat(clipboard(), is("untouched"));
    }

    /**
     * A2: without a Share Dialog and with a clipboard that refuses, the Public Address appears as
     * selected text next to the control, with the hint to copy it by hand — and no confirmation.
     */
    @Test
    void a_refusing_clipboard_shows_the_address_selected_for_copying_by_hand() {
        openEventPage(REFUSING_CLIPBOARD);

        shareButton().click();

        assertThat(addressField()).isVisible();
        assertThat(addressField()).hasValue(publicAddress());
        assertThat(page.getByText("Kopiere den Link von Hand.")).isVisible();
        assertThat(status()).isEmpty();
        assertThat((Boolean) page.evaluate("() => { const f = document.getElementById('share-address');"
                + " return document.activeElement === f && f.selectionStart === 0"
                + " && f.selectionEnd === f.value.length; }"), is(true));
    }

    /**
     * A4 and BR-005: with scripting switched off there is no share control; the Public Address is
     * shown as selectable text, labelled as the link to the event, and the page reads as before.
     */
    @Test
    void without_script_the_public_address_is_shown_instead_of_the_control() {
        Browser browser = context.browser();
        try (BrowserContext noScript = browser.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false))) {
            Page plain = noScript.newPage();
            slug = Event.<Event>findById(EventFixture.createEvent(TITLE)).slug;
            plain.navigate(url("/anlaesse/detail?slug=" + slug));

            assertThat(plain.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(TITLE))).isVisible();
            assertThat(plain.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Teilen"))).isHidden();
            assertThat(plain.getByLabel("Link zu diesem Anlass")).isVisible();
            assertThat(plain.getByLabel("Link zu diesem Anlass")).hasValue(publicAddress());
        }
    }

    /** With script the address field gives way to the share control (A4 is the fallback only). */
    @Test
    void with_script_the_share_control_replaces_the_address_field() {
        openEventPage(null);

        assertThat(shareButton()).isVisible();
        assertThat(addressField()).isHidden();
    }
}
