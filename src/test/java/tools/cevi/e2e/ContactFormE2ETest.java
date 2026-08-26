package tools.cevi.e2e;

import java.util.UUID;

import com.microsoft.playwright.Locator;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.contact.ContactFormEntry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * The contact form was the second element the audience named. What it now owes is testable only in
 * a browser: that a rejected submission puts the visitor at the fault instead of leaving them to
 * find it, and that the field which recognises automated senders is invisible to a human without
 * being removed from the page (FR-037, BR-032, BR-045).
 *
 * <p>Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base.
 */
@QuarkusTest
public class ContactFormE2ETest extends PlaywrightTestBase {

    private final String message = "CLEANUP e2e-" + UUID.randomUUID();

    @Test
    public void a_visitor_can_write_and_send_a_message() {
        page.navigate(url("/kontakt"));

        page.locator("#message").fill(message);
        page.locator("#spam").fill("50");
        page.locator(".form-actions button[type='submit']").click();

        assertThat(page.getByText("Nachricht ist unterwegs").isVisible(), is(true));
        assertThat(ContactFormEntry.find("message", message).count(), is(1L));
    }

    /**
     * The summary is what a screen reader announces first, so it has to hold the focus when the
     * page comes back — and it has to lead at the field rather than merely name it (BR-045).
     */
    @Test
    public void a_rejected_submission_keeps_the_text_and_lands_on_the_error_summary() {
        page.navigate(url("/kontakt"));

        page.locator("#message").fill(message);
        page.locator("#spam").fill("7");
        page.locator(".form-actions button[type='submit']").click();

        assertThat(page.locator("#message").inputValue(), is(message));

        Locator summary = page.locator(".error-summary");
        assertThat(summary.isVisible(), is(true));
        assertThat(page.evaluate("document.activeElement.className").toString(),
                containsString("error-summary"));

        summary.locator("a").click();
        assertThat(page.evaluate("document.activeElement.id").toString(), is("spam"));
        assertThat(page.locator("#spam").getAttribute("aria-invalid"), is("true"));

        assertThat(ContactFormEntry.find("message", message).count(), is(0L));
    }

    /**
     * Off-screen, never {@code display: none}: a simple sender fills in every input it can read in
     * the markup, which is the whole mechanism. Hiding it the obvious way would switch the check
     * off without any test noticing (BR-032, FR-032).
     */
    @Test
    public void the_honeypot_is_out_of_sight_but_still_in_the_page() {
        page.navigate(url("/kontakt"));

        Locator honeypot = page.locator("#website");
        assertThat(honeypot.count(), is(1));

        String display = (String) honeypot.evaluate("element => getComputedStyle(element).display");
        String visibility = (String) honeypot.evaluate("element => getComputedStyle(element).visibility");
        assertThat(display, is(not("none")));
        assertThat(visibility, is(not("hidden")));

        // Off the left edge of the viewport, so no sighted visitor ever meets it
        Object offScreen = honeypot.evaluate("element => element.getBoundingClientRect().right < 0");
        assertThat(offScreen, is(Boolean.TRUE));
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.begin();
        ContactFormEntry.delete("message", message);
        QuarkusTransaction.commit();
    }
}
