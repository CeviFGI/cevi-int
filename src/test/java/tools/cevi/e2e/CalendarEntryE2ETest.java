package tools.cevi.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.event.Event;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * UC-012 Add an Event to the Calendar together with UC-002 BR-012, in a real browser: the
 * administrator records the First Day and Last Day in the event form, a visitor takes the event into
 * the calendar from its Event Page, and the administrator removes the period again.
 *
 * <p>Whether Google Calendar, Apple Calendar and Outlook accept the file is checked once at
 * acceptance (NFR-048, UC-012 BR-004); here the browser receives the file as a download, which is
 * what a device without a calendar app does with it (A3).
 */
@QuarkusTest
public class CalendarEntryE2ETest extends PlaywrightTestBase {

    private final String title = "CLEANUP calendar-" + UUID.randomUUID();

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> Event.delete("title", title));
    }

    private Locator calendarLink(Page visitor) {
        return visitor.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("In den Kalender"));
    }

    private static Event stored(String title) {
        return QuarkusTransaction.requiringNew().call(() -> Event.<Event>find("title", title).firstResult());
    }

    private void submitEventForm() {
        page.locator(".form-actions button[type='submit']").click();
    }

    /** UC-002 step 3, BR-012 and UC-012 main success scenario, BR-001, A1. */
    @Test
    public void the_administrator_records_a_period_and_a_visitor_takes_the_event_into_the_calendar() throws IOException {
        signInAsAdministrator();

        // UC-002 step 3: the period is entered with the browser's own date fields
        page.navigate(url("/anlaesse/add"));
        page.locator("#title").fill(title);
        page.locator("#date").fill("1. bis 5. Juli 2099");
        page.locator("#displayDate").fill("2099-06-15");
        page.getByLabel("Erster Tag").fill("2099-07-01");
        page.getByLabel("Letzter Tag").fill("2099-07-05");
        page.locator("#location").fill("Genf");
        page.locator(".jodit-wysiwyg").click();
        page.keyboard().type("Ein Lager am See");
        submitEventForm();
        page.waitForURL("**/anlaesse");
        Event event = stored(title);
        assertThat(event.firstDay, is(LocalDate.of(2099, 7, 1)));
        assertThat(event.lastDay, is(LocalDate.of(2099, 7, 5)));

        // UC-012: a visitor of their own — no session, and a browser that runs no script (C-021)
        try (BrowserContext visitorContext = page.context().browser()
                .newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false).setAcceptDownloads(true))) {
            Page visitor = visitorContext.newPage();
            List<String> foreign = foreignRequests(visitor);
            visitor.navigate(url("/anlaesse/detail?slug=" + event.slug));

            // Steps 1-2: the plain link delivers the Calendar Entry as a file (A3 on a device without a calendar app)
            Download download = visitor.waitForDownload(() -> calendarLink(visitor).click());
            assertThat(download.suggestedFilename(), endsWith(".ics"));
            String file = new String(Files.readAllBytes(download.path()), StandardCharsets.UTF_8).replace("\r\n ", "");
            assertThat(file, containsString("\r\nSUMMARY:" + title + "\r\n"));
            assertThat(file, containsString("\r\nDTSTART;VALUE=DATE:20990701\r\n"));
            assertThat(file, containsString("\r\nDTEND;VALUE=DATE:20990706\r\n"));
            assertThat(file, containsString("\r\nLOCATION:Genf\r\n"));
            assertThat(file, containsString("Ein Lager am See"));
            assertThat(file, containsString("/anlaesse/detail?slug=" + event.slug));

            // BR-006: the Calendar Entry comes from the platform itself, no other host is asked
            assertThat(foreign, is(empty()));
        }

        // UC-002 BR-012, UC-012 A1: emptying both days removes the period, and with it the offer
        page.navigate(url("/anlaesse/edit?id=" + event.id));
        page.getByLabel("Erster Tag").fill("");
        page.getByLabel("Letzter Tag").fill("");
        submitEventForm();
        page.waitForURL("**/anlaesse");
        page.navigate(url("/anlaesse/detail?slug=" + event.slug));
        assertThat(calendarLink(page).count(), is(0));
        assertThat(page.getByLabel("Link zu diesem Anlass").count(), is(1));
    }

    /** UC-002 A2, BR-012: a Last Day before the First Day is marked, and the typed days stay in the form. */
    @Test
    public void a_last_day_before_the_first_day_is_marked_in_the_form() {
        signInAsAdministrator();

        page.navigate(url("/anlaesse/add"));
        page.locator("#title").fill(title);
        page.locator("#date").fill("Juli 2099");
        page.locator("#location").fill("Genf");
        page.locator(".jodit-wysiwyg").click();
        page.keyboard().type("Beschreibung");
        page.getByLabel("Erster Tag").fill("2099-07-05");
        page.getByLabel("Letzter Tag").fill("2099-07-01");
        submitEventForm();

        Locator lastDay = page.getByLabel("Letzter Tag");
        assertThat(lastDay.getAttribute("aria-invalid"), is("true"));
        assertThat(page.getByText("Der Letzte Tag darf nicht vor dem Ersten Tag liegen").first().isVisible(), is(true));
        assertThat(page.getByLabel("Erster Tag").inputValue(), is("2099-07-05"));
        assertThat(lastDay.inputValue(), is("2099-07-01"));
        assertThat(stored(title), is((Event) null));
    }

    /** NFR-013, NFR-039: on a phone the offer is a full tap target and takes the page nowhere sideways. */
    @Test
    public void the_offer_is_a_full_tap_target_on_a_phone() {
        QuarkusTransaction.requiringNew().run(() -> {
            Event event = new Event();
            event.title = title;
            event.slug = "cleanup-calendar-phone-" + UUID.randomUUID();
            event.date = "1. bis 5. Juli 2099";
            event.location = "Genf";
            event.displayDate = LocalDate.of(2099, 6, 15);
            event.firstDay = LocalDate.of(2099, 7, 1);
            event.lastDay = LocalDate.of(2099, 7, 5);
            event.description = "<p>Beschreibung</p>";
            event.persist();
        });
        page.setViewportSize(320, 640);

        page.navigate(url("/anlaesse/detail?slug=" + stored(title).slug));

        BoundingBox box = calendarLink(page).boundingBox();
        assertThat(box.width, greaterThanOrEqualTo(44.0));
        assertThat(box.height, greaterThanOrEqualTo(44.0));
        Number scrollWidth = (Number) page.evaluate("() => document.documentElement.scrollWidth");
        assertThat(scrollWidth.intValue(), lessThanOrEqualTo(320));
    }

    private List<String> foreignRequests(Page visitor) {
        List<String> foreign = new ArrayList<>();
        String ownHost = URI.create(url("/")).getHost();
        visitor.onRequest(request -> {
            String host = URI.create(request.url()).getHost();
            if (host != null && !host.equals(ownHost)) {
                foreign.add(request.url());
            }
        });
        return foreign;
    }
}
