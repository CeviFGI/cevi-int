package tools.cevi.e2e;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.event.Event;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * UC-002 Manage Events in a real browser: the administrator adds an event with a formatted
 * description through the rich-text editor, corrects it and removes it again.
 * Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base class.
 */
@QuarkusTest
public class EventFormE2ETest extends PlaywrightTestBase {

    private final String title = "CLEANUP e2e-" + UUID.randomUUID();

    /**
     * Main success scenario, A1 and A3: add with today's display date pre-filled, see the event in
     * the list, change it, then remove it after confirming.
     */
    @Test
    public void admin_adds_changes_and_removes_an_event() {
        loginAsAdmin();

        // Steps 1-2: the form starts empty with today's date as the display date
        page.navigate(url("/anlaesse/add"));
        assertThat(page.locator("#displayDate").inputValue(), is(LocalDate.now().toString()));

        // Step 3-4: the administrator fills in the event, the description through the editor
        page.locator("#title").fill(title);
        page.locator("#date").fill("Sommer 2099");
        page.locator("#displayDate").fill("2099-07-01");
        page.locator("#location").fill("Genf");
        page.locator(".jodit-wysiwyg").click();
        page.keyboard().type("Ein Lager am See");
        page.locator(".form-actions button[type='submit']").click();

        // Step 9: back on the list, the new event is visible
        page.waitForURL("**/anlaesse");
        assertThat(page.getByText(title).isVisible(), is(true));
        Event saved = Event.find("title", title).firstResult();
        assertThat(saved, notNullValue());
        assertThat(saved.description, containsString("Ein Lager am See"));

        // A1: the form comes back filled with the current values, the change is recorded
        page.navigate(url("/anlaesse/edit?id=" + saved.id));
        assertThat(page.locator("#location").inputValue(), is("Genf"));
        page.locator("#location").fill("Lausanne");
        page.locator(".form-actions button[type='submit']").click();
        page.waitForURL("**/anlaesse");
        assertThat(page.locator("[aria-invalid='true']").count(), is(0));
        assertThat(page.locator("h1").textContent(), is("Anlässe"));
        assertThat(reload(saved.id).location, is("Lausanne"));

        // A3: the removal is confirmed on its own page
        page.navigate(url("/anlaesse/delete?id=" + saved.id));
        page.getByText("Endgültig löschen").click();
        page.waitForURL("**/anlaesse");
        assertThat(page.getByText(title).count(), is(0));
        assertThat(reload(saved.id), nullValue());
    }

    /** Reads the event in a transaction of its own, so the test sees what is stored, not a cached copy. */
    private static Event reload(int id) {
        return QuarkusTransaction.requiringNew().call(() -> Event.findById(id));
    }

    private void loginAsAdmin() {
        page.navigate(url("/auth/login"));
        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("admin");
        page.locator(".form-actions button[type='submit']").click();
        // Signing in lands on the start page, not on the event list — see UC-008 A4.
        page.waitForURL(url("/"));
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.begin();
        Event.delete("title", title);
        QuarkusTransaction.commit();
    }
}
