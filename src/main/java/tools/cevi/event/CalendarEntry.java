package tools.cevi.event;

import tools.cevi.infra.PlainText;
import tools.cevi.infra.Slug;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * The Calendar Entry of an event: an iCalendar file (RFC 5545) holding the event as one all-day
 * entry (FR-047, NFR-048, ADR-007).
 *
 * <p>Written by hand rather than with a library: the file is one fixed {@code VEVENT} with six text
 * properties, and the three things RFC 5545 is strict about — escaping, CRLF and folding — fit in
 * this class.
 */
final class CalendarEntry {

    static final String MEDIA_TYPE = "text/calendar; charset=UTF-8";

    /**
     * The host part of every {@code UID}. Fixed rather than taken from the request: the identity of an
     * event must not change with the host name it was downloaded under (UC-012 BR-004).
     */
    static final String UID_DOMAIN = "international.cevi.tools";

    private static final String CRLF = "\r\n";

    /** RFC 5545 3.1: a content line is at most 75 octets long, CRLF excluded. */
    private static final int MAX_LINE_OCTETS = 75;

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter UTC_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /** RFC 5545 3.1: control characters other than horizontal tab are not allowed in a value. */
    private static final Pattern CONTROL = Pattern.compile("[\\x00-\\x08\\x0B-\\x1F\\x7F]");

    private CalendarEntry() {
    }

    /**
     * @param event         an event with a period ({@link Event#hasPeriod()})
     * @param publicAddress the Public Address of its Event Page (UC-012 BR-003)
     * @param now           the moment of delivery, for {@code DTSTAMP}
     * @return the complete iCalendar file
     */
    static String of(Event event, String publicAddress, Instant now) {
        if (!event.hasPeriod()) {
            throw new IllegalArgumentException("Event " + event.id + " has no period");
        }
        StringBuilder file = new StringBuilder();
        line(file, "BEGIN:VCALENDAR");
        line(file, "VERSION:2.0");
        line(file, "PRODID:-//Cevi Schweiz//Cevi International//DE");
        line(file, "CALSCALE:GREGORIAN");
        line(file, "METHOD:PUBLISH");
        // UC-012 BR-002: exactly one event
        line(file, "BEGIN:VEVENT");
        // UC-012 BR-004: the identity hangs on the id only, so title, period and Short Name may change
        line(file, "UID:" + uid(event));
        line(file, "DTSTAMP:" + UTC_TIMESTAMP.format(now));
        // UC-012 BR-002: all day, First Day through Last Day both included, no time and no time zone;
        // NFR-048: DTEND is exclusive, so it is the day after the Last Day
        line(file, "DTSTART;VALUE=DATE:" + DATE.format(event.firstDay));
        line(file, "DTEND;VALUE=DATE:" + DATE.format(event.lastDay.plusDays(1)));
        line(file, "SUMMARY:" + text(event.title));
        line(file, "LOCATION:" + text(event.location));
        line(file, "DESCRIPTION:" + text(notes(event, publicAddress)));
        // UC-012 BR-002: no VALARM and no ATTENDEE — no reminder, no invitation
        line(file, "END:VEVENT");
        line(file, "END:VCALENDAR");
        return file.toString();
    }

    // UC-012 BR-004: one identity per event, never shared by two events
    static String uid(Event event) {
        return "event-" + event.id + "@" + UID_DOMAIN;
    }

    /**
     * NFR-048: a file name ending in {@code .ics}. An entered Short Name is stored as typed and may hold
     * quotes, slashes or line breaks, none of which belong into a header; the name is therefore reduced
     * to the letters, digits and hyphens a derived Short Name consists of.
     */
    static String fileName(Event event) {
        String name = Slug.of(event.slug).toString();
        return (name.isEmpty() ? "anlass" : name) + ".ics";
    }

    // UC-012 BR-002: the notes hold the Date Text, the visible Description and the Public Address
    private static String notes(Event event, String publicAddress) {
        StringBuilder notes = new StringBuilder(event.date);
        String description = PlainText.of(event.description);
        if (!description.isEmpty()) {
            notes.append("\n\n").append(description);
        }
        return notes.append("\n\n").append(publicAddress).toString();
    }

    /** RFC 5545 3.3.11: a TEXT value escapes backslash, semicolon, comma and line breaks. */
    static String text(String value) {
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        return CONTROL.matcher(normalized.replace("\\", "\\\\")
                        .replace(";", "\\;")
                        .replace(",", "\\,")
                        .replace("\n", "\\n"))
                .replaceAll("");
    }

    /**
     * RFC 5545 3.1: appends one content line, folded so that no line exceeds 75 octets; a
     * continuation starts with a space, which counts towards its 75. A line is only ever split between
     * two characters, never inside the UTF-8 bytes of one.
     */
    static void line(StringBuilder file, String contentLine) {
        int octets = 0;
        int index = 0;
        while (index < contentLine.length()) {
            int codePoint = contentLine.codePointAt(index);
            int width = Character.toString(codePoint).getBytes(StandardCharsets.UTF_8).length;
            if (octets + width > MAX_LINE_OCTETS) {
                file.append(CRLF).append(' ');
                octets = 1;
            }
            file.appendCodePoint(codePoint);
            octets += width;
            index += Character.charCount(codePoint);
        }
        file.append(CRLF);
    }
}
