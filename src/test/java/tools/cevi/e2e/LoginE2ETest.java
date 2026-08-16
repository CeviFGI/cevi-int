package tools.cevi.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

/** Quarkus only indexes {@code @QuarkusTest} on the concrete class, not on an inherited base class. */
@QuarkusTest
public class LoginE2ETest extends PlaywrightTestBase {

    @Test
    public void login_with_demo_credentials_shows_logout_button() {
        page.navigate(url("/auth/login"));

        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("admin");
        page.locator("input[type='submit']").click();
        // The login POST is followed by a server-side redirect chain (/ -> /anlaesse);
        // wait for it to fully settle before inspecting the rendered page.
        page.waitForURL("**/anlaesse");

        assertThat(page.url(), endsWith("/anlaesse"));
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ausloggen")).isVisible(), is(true));
    }

    /**
     * Signing out changes state, so it is a form submission carrying a CSRF token rather than a
     * link. This walks the whole round trip through a real browser (BR-028).
     */
    @Test
    public void logout_ends_the_session() {
        page.navigate(url("/auth/login"));
        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("admin");
        page.locator("input[type='submit']").click();
        page.waitForURL("**/anlaesse");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ausloggen")).click();
        page.waitForURL("**/auth/loggedOut");

        assertThat(page.url(), containsString("/auth/loggedOut"));

        page.navigate(url("/anlaesse"));
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ausloggen")).isVisible(), is(false));
    }

    @Test
    public void failed_login_shows_error_page() {
        page.navigate(url("/auth/login"));

        page.locator("input[name='j_username']").fill("admin");
        page.locator("input[name='j_password']").fill("wrong-password");
        page.locator("input[type='submit']").click();
        page.waitForURL("**/auth/error");

        assertThat(page.url(), containsString("/auth/error"));
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ausloggen")).isVisible(), is(false));
    }
}
