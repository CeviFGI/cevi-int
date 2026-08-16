package tools.cevi.e2e;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.voluntary.VoluntaryService;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Exercises the Summernote/jQuery rich-text editor on the "add voluntary service" form —
 * behaviour the existing REST Assured tests can't cover since it's purely client-side JS.
 * Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base class.
 */
@QuarkusTest
public class VoluntaryFormE2ETest extends PlaywrightTestBase {

    private final String organization = "CLEANUP e2e-" + UUID.randomUUID();

    @Test
    public void admin_can_create_voluntary_service_with_rich_text_description() {
        loginAsAdmin();

        page.navigate(url("/volontariat/add"));
        page.locator("input[name='organization']").fill(organization);
        page.locator("input[name='organizationLink']").fill("https://example.org");
        page.locator("input[name='location']").fill("Bern");

        // Summernote replaces the textarea with a rich-text contenteditable div.
        page.locator(".note-editable").click();
        page.keyboard().type("Freiwilligenarbeit in Bern");

        page.locator("input[type='submit']").click();
        page.waitForURL("**/volontariat");

        assertThat(page.getByText(organization).isVisible(), is(true));

        VoluntaryService saved = VoluntaryService.find("organization", organization).firstResult();
        assertThat(saved, notNullValue());
        assertThat(saved.description, containsString("Freiwilligenarbeit in Bern"));
    }

    private void loginAsAdmin() {
        page.navigate(url("/auth/login"));
        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("admin");
        page.locator("input[type='submit']").click();
        page.waitForURL("**/anlaesse");
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.begin();
        VoluntaryService toDelete = VoluntaryService.find("organization", organization).firstResult();
        if (toDelete != null) {
            toDelete.delete();
        }
        QuarkusTransaction.commit();
    }
}
