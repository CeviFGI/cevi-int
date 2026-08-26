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
 * Exercises the Jodit rich-text editor on the "add voluntary service" form —
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

        // The editor hides the textarea and edits in a contenteditable div next to it.
        page.locator(".jodit-wysiwyg").click();
        page.keyboard().type("Freiwilligenarbeit in Bern");

        page.locator(".form-actions button[type='submit']").click();
        page.waitForURL("**/volontariat");

        assertThat(page.getByText(organization).isVisible(), is(true));

        VoluntaryService saved = VoluntaryService.find("organization", organization).firstResult();
        assertThat(saved, notNullValue());
        assertThat(saved.description, containsString("Freiwilligenarbeit in Bern"));
    }

    /**
     * Phase 3 gave the editor a container that matches the other controls. Its frame is drawn by
     * jodit.min.css, which is linked after the site stylesheet — so the rule that overrides it is
     * one specificity step away from silently losing, and nothing in the markup would show that.
     */
    @Test
    public void the_editor_initialises_inside_the_restyled_container() {
        loginAsAdmin();

        page.navigate(url("/volontariat/add"));

        // The label still belongs to the textarea the editor took over
        assertThat(page.locator("label[for='description']").count(), is(1));

        String radius = (String) page.locator(".jodit-container")
                .evaluate("element => getComputedStyle(element).borderTopLeftRadius");
        assertThat(radius, is("8px"));
    }

    private void loginAsAdmin() {
        page.navigate(url("/auth/login"));
        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("admin");
        page.locator(".form-actions button[type='submit']").click();
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
