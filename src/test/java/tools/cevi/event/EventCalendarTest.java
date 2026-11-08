package tools.cevi.event;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.EventFixture;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * UC-012 Add an Event to the Calendar — the offer on the Event Page and the Calendar Entry the
 * platform delivers. The file's line format is {@code CalendarEntryTest}; the download in a real
 * browser is {@code CalendarEntryE2ETest}.
 */
@QuarkusTest
class EventCalendarTest {

    private static final String CALENDAR_ENTRY = "/anlaesse/kalender.ics";
    private static final LocalDate FIRST = LocalDate.of(2099, 7, 1);
    private static final LocalDate LAST = LocalDate.of(2099, 7, 5);

    @TestHTTPResource("/")
    URL root;

    private String origin() {
        return root.getProtocol() + "://" + root.getHost() + ":" + root.getPort();
    }

    private static Event reload(long id) {
        return QuarkusTransaction.requiringNew().call(() -> Event.findById((int) id));
    }

    private static String eventPage(String slug) {
        return given().queryParam("slug", slug).when().get("/anlaesse/detail").then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    private static String calendarEntry(String slug) {
        return given().queryParam("slug", slug).when().get(CALENDAR_ENTRY).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    private static String unfolded(String file) {
        return file.replace("\r\n ", "");
    }

    /** The number of rows in every table of the database, by table name. */
    @SuppressWarnings("unchecked")
    private static Map<String, Long> rowsPerTable() {
        return QuarkusTransaction.requiringNew().call(() -> {
            var em = Event.getEntityManager();
            List<String> tables = em.createNativeQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").getResultList();
            Map<String, Long> rows = new TreeMap<>();
            for (String table : tables) {
                Number count = (Number) em.createNativeQuery("SELECT COUNT(*) FROM \"" + table + "\"").getSingleResult();
                rows.put(table, count.longValue());
            }
            return rows;
        });
    }

    private static String uidOf(String file) {
        Matcher uid = Pattern.compile("\r\nUID:([^\r]+)\r\n").matcher(unfolded(file));
        assertThat("the entry carries a UID", uid.find(), is(true));
        return uid.group(1);
    }

    /** Main success scenario, BR-001, BR-006: an event with a period offers a plain link to its entry. */
    @Test
    void an_event_with_a_period_offers_its_calendar_entry() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 offer", "desc", FIRST, LAST);
        String slug = reload(id).slug;

        String body = eventPage(slug);

        assertThat(body, containsString("href=\"/anlaesse/kalender.ics?slug=" + slug + "\""));
        assertThat(body, containsString("In den Kalender"));
    }

    /** A1, BR-001: without a period there is no offer, and the rest of the page is unchanged. */
    @Test
    void an_event_without_a_period_offers_no_calendar_entry() {
        long id = EventFixture.createEvent("CLEANUP UC-012 no period");
        String slug = reload(id).slug;

        String body = eventPage(slug);

        assertThat(body, not(containsString("kalender.ics")));
        assertThat(body, not(containsString("In den Kalender")));
        assertThat(body, containsString("id=\"share-address\""));
    }

    /** BR-001: whether the Display Date has passed does not matter. */
    @Test
    void an_event_whose_display_date_has_passed_still_offers_its_entry() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 past display date", "desc", FIRST, LAST);
        QuarkusTransaction.requiringNew().run(() -> ((Event) Event.findById((int) id)).displayDate = LocalDate.now().minusDays(1));
        String slug = reload(id).slug;

        assertThat(eventPage(slug), containsString("kalender.ics"));
        assertThat(calendarEntry(slug), containsString("\r\nBEGIN:VEVENT\r\n"));
    }

    /** The link reaches the event under exactly its Short Name, also when that holds &, # or +. */
    @Test
    void the_link_encodes_an_entered_short_name() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 encoded", "desc", FIRST, LAST);
        String slug = "Lager & Kurs #1+2";
        QuarkusTransaction.requiringNew().run(() -> ((Event) Event.findById((int) id)).slug = slug);

        Matcher href = Pattern.compile("href=\"(/anlaesse/kalender\\.ics\\?[^\"]+)\"").matcher(eventPage(slug));
        assertThat("the page links to the calendar entry", href.find(), is(true));
        String link = href.group(1).replace("&amp;", "&");

        assertThat(link, not(containsString(" ")));
        assertThat(link, not(containsString("#")));
        String file = given().urlEncodingEnabled(false).when().get(link).then()
                .statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertThat(file, containsString("SUMMARY:CLEANUP UC-012 encoded"));
    }

    /** NFR-048: text/calendar in UTF-8, a file name ending in .ics; A3: the device may keep it as a file. */
    @Test
    void the_entry_is_served_as_an_ics_file() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 media type", "desc", FIRST, LAST);
        String slug = reload(id).slug;

        given().queryParam("slug", slug).when().get(CALENDAR_ENTRY).then()
                .statusCode(HttpStatus.SC_OK)
                .header("Content-Type", startsWith("text/calendar"))
                .header("Content-Type", containsString("charset=UTF-8"))
                .header("Content-Disposition", equalTo("attachment; filename=\"" + slug + ".ics\""));
    }

    /**
     * Main success scenario step 2, BR-002, BR-003: title, location, Date Text, the visible text of
     * the Description and the Public Address — built from the request's origin with the Short Name
     * as its only parameter.
     */
    @Test
    void the_entry_holds_the_event_and_its_public_address() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 content",
                "<p>Ein <strong>Lager</strong> am See.</p>", FIRST, LAST);
        String slug = reload(id).slug;

        String file = unfolded(given().queryParam("slug", slug).queryParam("utm_source", "newsletter")
                .when().get(CALENDAR_ENTRY).then()
                .statusCode(HttpStatus.SC_OK).extract().body().asString());

        assertThat(file, startsWith("BEGIN:VCALENDAR\r\n"));
        assertThat(file, containsString("\r\nSUMMARY:CLEANUP UC-012 content\r\n"));
        assertThat(file, containsString("\r\nLOCATION:some location\r\n"));
        assertThat(file, containsString("\r\nDTSTART;VALUE=DATE:20990701\r\n"));
        assertThat(file, containsString("\r\nDTEND;VALUE=DATE:20990706\r\n"));
        assertThat(file, containsString("\r\nDESCRIPTION:some date\\n\\nEin Lager am See.\\n\\n"
                + origin() + "/anlaesse/detail?slug=" + slug + "\r\n"));
        assertThat(file, not(containsString("utm_source")));
        assertThat(file, not(containsString("<strong>")));
    }

    /** BR-004, BR-005: the entry is current when taken in again, under the same identity. */
    @Test
    void the_identity_stays_when_title_period_and_short_name_change() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 identity", "desc", FIRST, LAST);
        long other = EventFixture.createEventWithPeriod("CLEANUP UC-012 identity other", "desc", FIRST, LAST);
        String before = calendarEntry(reload(id).slug);

        QuarkusTransaction.requiringNew().run(() -> {
            Event event = Event.findById((int) id);
            event.title = "CLEANUP UC-012 identity renamed";
            event.slug = "cleanup-uc-012-identity-renamed";
            event.firstDay = FIRST.plusDays(10);
            event.lastDay = LAST.plusDays(10);
        });
        String after = calendarEntry("cleanup-uc-012-identity-renamed");

        assertThat(uidOf(after), equalTo(uidOf(before)));
        assertThat(after, containsString("SUMMARY:CLEANUP UC-012 identity renamed"));
        assertThat(after, containsString("DTSTART;VALUE=DATE:20990711"));
        assertThat(uidOf(calendarEntry(reload(other).slug)), not(equalTo(uidOf(before))));
    }

    /**
     * Success postcondition "No data is stored or changed on the platform", BR-006: delivering the
     * Calendar Entry adds no row to any table — no counter, no record of its own — and leaves the
     * event exactly as it was.
     */
    @Test
    void taking_the_event_into_the_calendar_stores_and_changes_nothing() {
        long id = EventFixture.createEventWithPeriod("CLEANUP UC-012 stores nothing",
                "<p>Ein Lager.</p>", FIRST, LAST);
        Event original = reload(id);
        Map<String, Long> rowsBefore = rowsPerTable();
        assertThat(rowsBefore, hasKey("events"));

        calendarEntry(original.slug);
        calendarEntry(original.slug);

        assertThat(rowsPerTable(), equalTo(rowsBefore));
        Event after = reload(id);
        assertThat(after.title, equalTo(original.title));
        assertThat(after.slug, equalTo(original.slug));
        assertThat(after.date, equalTo(original.date));
        assertThat(after.location, equalTo(original.location));
        assertThat(after.description, equalTo(original.description));
        assertThat(after.displayDate, equalTo(original.displayDate));
        assertThat(after.firstDay, equalTo(original.firstDay));
        assertThat(after.lastDay, equalTo(original.lastDay));
    }

    /** A2: an unknown Short Name — removed or renamed event — leads to the Not-Found Page. */
    @Test
    void an_unknown_short_name_delivers_no_entry() {
        String body = given().queryParam("slug", "cleanup-uc-012-does-not-exist").when().get(CALENDAR_ENTRY).then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().body().asString();

        assertThat(body, not(containsString("BEGIN:VCALENDAR")));
        assertThat(body, containsString("href=\"/anlaesse\""));
    }

    /** A2: an event whose period has been removed delivers no entry either. */
    @Test
    void an_event_without_a_period_delivers_no_entry() {
        long id = EventFixture.createEvent("CLEANUP UC-012 period removed");

        given().queryParam("slug", reload(id).slug).when().get(CALENDAR_ENTRY).then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /** A2: a request without any Short Name delivers no entry. */
    @Test
    void a_request_without_short_name_delivers_no_entry() {
        given().when().get(CALENDAR_ENTRY).then().statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
