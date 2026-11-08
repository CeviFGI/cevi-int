package tools.cevi.event;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * NFR-048 and UC-012 BR-002, BR-004: the iCalendar file of a Calendar Entry, checked line by line.
 * What the endpoint delivers around it (media type, file name, A2) is {@code EventCalendarTest}.
 */
class CalendarEntryTest {

    private static final String ADDRESS = "https://international.cevi.tools/anlaesse/detail?slug=sommerlager";
    private static final Instant NOW = Instant.parse("2026-11-08T17:05:09Z");

    private static Event event() {
        Event event = new Event();
        event.id = 42;
        event.title = "Sommerlager";
        event.slug = "sommerlager";
        event.date = "1. bis 5. Juli 2099";
        event.location = "Genf";
        event.description = "<p>Ein Lager am See.</p>";
        event.displayDate = LocalDate.of(2099, 6, 1);
        event.firstDay = LocalDate.of(2099, 7, 1);
        event.lastDay = LocalDate.of(2099, 7, 5);
        return event;
    }

    /** The content lines with folding undone (RFC 5545 3.1). */
    private static List<String> unfolded(String file) {
        return Arrays.asList(file.replace("\r\n ", "").split("\r\n"));
    }

    /** Main success scenario step 2, BR-002, NFR-048: exactly one all-day event with title, location and notes, from the First Day through the Last Day. */
    @Test
    void the_file_holds_exactly_one_all_day_event() {
        List<String> lines = unfolded(CalendarEntry.of(event(), ADDRESS, NOW));

        assertEquals(List.of(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Cevi Schweiz//Cevi International//DE",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                "BEGIN:VEVENT",
                "UID:event-42@international.cevi.tools",
                "DTSTAMP:20261108T170509Z",
                "DTSTART;VALUE=DATE:20990701",
                "DTEND;VALUE=DATE:20990706",
                "SUMMARY:Sommerlager",
                "LOCATION:Genf",
                "DESCRIPTION:1. bis 5. Juli 2099\\n\\nEin Lager am See.\\n\\n" + ADDRESS,
                "END:VEVENT",
                "END:VCALENDAR"), lines);
    }

    /** BR-002: an event of a single day ends, exclusively, on the following day. */
    @Test
    void a_single_day_ends_on_the_day_after() {
        Event event = event();
        event.lastDay = event.firstDay;

        List<String> lines = unfolded(CalendarEntry.of(event, ADDRESS, NOW));

        assertThat(lines, hasItem("DTSTART;VALUE=DATE:20990701"));
        assertThat(lines, hasItem("DTEND;VALUE=DATE:20990702"));
    }

    /** NFR-048: DTEND is the day after the Last Day across a month and a year boundary. */
    @Test
    void the_end_crosses_a_year_boundary() {
        Event event = event();
        event.firstDay = LocalDate.of(2099, 12, 27);
        event.lastDay = LocalDate.of(2099, 12, 31);

        assertThat(unfolded(CalendarEntry.of(event, ADDRESS, NOW)), hasItem("DTEND;VALUE=DATE:21000101"));
    }

    /** BR-002: no time of day, no time zone, no reminder, no invitation. */
    @Test
    void the_entry_names_no_time_zone_reminder_or_invitation() {
        String file = CalendarEntry.of(event(), ADDRESS, NOW);

        assertThat(file, not(containsString("VTIMEZONE")));
        assertThat(file, not(containsString("TZID")));
        assertThat(file, not(containsString("VALARM")));
        assertThat(file, not(containsString("ATTENDEE")));
        assertThat(file, not(containsString("ORGANIZER")));
    }

    /** BR-004: the identity hangs on the event alone, not on its title, period or Short Name. */
    @Test
    void the_identity_survives_a_change_of_title_period_and_short_name() {
        Event changed = event();
        changed.title = "Herbstlager";
        changed.slug = "herbstlager";
        changed.firstDay = LocalDate.of(2099, 10, 1);
        changed.lastDay = LocalDate.of(2099, 10, 3);
        Event other = event();
        other.id = 43;

        assertEquals(CalendarEntry.uid(event()), CalendarEntry.uid(changed));
        assertThat(CalendarEntry.uid(other), not(is(CalendarEntry.uid(event()))));
    }

    /** NFR-048: backslash, semicolon, comma and line breaks are escaped in every text value. */
    @Test
    void text_values_are_escaped() {
        assertEquals("a\\\\b\\;c\\,d\\ne\\nf\\ng", CalendarEntry.text("a\\b;c,d\ne\r\nf\rg"));

        Event event = event();
        event.title = "Lager; Kurs, Fest";
        event.location = "Bern\\Thun";
        List<String> lines = unfolded(CalendarEntry.of(event, ADDRESS, NOW));

        assertThat(lines, hasItem("SUMMARY:Lager\\; Kurs\\, Fest"));
        assertThat(lines, hasItem("LOCATION:Bern\\\\Thun"));
    }

    /** RFC 5545 3.1: control characters do not reach the file, a tab does. */
    @Test
    void control_characters_are_dropped() {
        assertEquals("ab\tc", CalendarEntry.text("a\u0000b\tc\u001B"));
    }

    /** NFR-048: every line ends with CRLF, there is no bare LF or CR. */
    @Test
    void every_line_ends_with_crlf() {
        Event event = event();
        event.description = "<p>Erste Zeile</p><p>Zweite Zeile</p>";
        String file = CalendarEntry.of(event, ADDRESS, NOW);

        assertThat(file, endsWith("END:VCALENDAR\r\n"));
        assertThat(file.replace("\r\n", ""), not(containsString("\n")));
        assertThat(file.replace("\r\n", ""), not(containsString("\r")));
    }

    /**
     * NFR-048: lines are folded at 75 octets — counted in UTF-8, so a line of umlauts folds earlier
     * than one of ASCII, and never inside the bytes of one character.
     */
    @Test
    void long_lines_are_folded_at_75_octets_without_splitting_a_character() {
        Event event = event();
        event.title = "Ä".repeat(100) + "x".repeat(100) + "😀".repeat(30);
        String file = CalendarEntry.of(event, ADDRESS, NOW);

        List<String> physical = Arrays.asList(file.split("\r\n"));
        List<Integer> octets = physical.stream().map(line -> line.getBytes(StandardCharsets.UTF_8).length).toList();
        assertThat(octets, everyItem(lessThanOrEqualTo(75)));
        assertThat(physical.stream().noneMatch(line -> line.contains("�")), is(true));
        assertThat(unfolded(file), hasItem("SUMMARY:" + event.title));
    }

    /** NFR-048: a line of exactly 75 octets stays whole, one of 76 folds after the 75th. */
    @Test
    void a_line_of_exactly_75_octets_is_not_folded() {
        StringBuilder file = new StringBuilder();
        CalendarEntry.line(file, "X".repeat(75));
        assertEquals("X".repeat(75) + "\r\n", file.toString());

        StringBuilder longer = new StringBuilder();
        CalendarEntry.line(longer, "X".repeat(76));
        assertEquals("X".repeat(75) + "\r\n X\r\n", longer.toString());
    }

    /** BR-002: a Description without visible text adds no empty paragraph to the notes. */
    @Test
    void an_empty_description_leaves_date_text_and_address() {
        Event event = event();
        event.description = "<p><br></p>";

        assertThat(unfolded(CalendarEntry.of(event, ADDRESS, NOW)),
                hasItem("DESCRIPTION:1. bis 5. Juli 2099\\n\\n" + ADDRESS));
    }

    /** BR-001: there is no Calendar Entry without a period. */
    @Test
    void an_event_without_period_has_no_entry() {
        Event event = event();
        event.lastDay = null;

        assertThrows(IllegalArgumentException.class, () -> CalendarEntry.of(event, ADDRESS, NOW));
    }

    /** NFR-048: the file name ends in .ics and carries nothing that could break the header. */
    @Test
    void the_file_name_is_safe_for_a_header() {
        Event event = event();
        assertEquals("sommerlager.ics", CalendarEntry.fileName(event));

        event.slug = "Lager \"2099\"/\r\nX-Evil: 1";
        assertEquals("lager-2099-x-evil-1.ics", CalendarEntry.fileName(event));

        event.slug = "!!!";
        assertEquals("anlass.ics", CalendarEntry.fileName(event));
    }
}
