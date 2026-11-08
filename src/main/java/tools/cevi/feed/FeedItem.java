package tools.cevi.feed;

import tools.cevi.event.Event;
import tools.cevi.voluntary.VoluntaryService;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * One Feed Item of the News Feed (UC-013 BR-003): an Event or a Voluntary Service Offer in the state
 * of its Last Change.
 *
 * @param title      the title of the item, marked when the entry was changed after its creation
 * @param link       the Public Address of the Event Page or the Offer Page
 * @param guid       the identity, new after every change of the entry (UC-013 BR-005)
 * @param category   the kind of entry, {@code Anlass} or {@code Volontariat}
 * @param lastChange the Last Change, published as the moment of publication
 * @param html       the text of the item as HTML: already reduced to the Allow-List where it comes
 *                   from a Description, escaped where it comes from plain text
 */
public record FeedItem(String title, String link, String guid, String category, Instant lastChange, String html) {

    static final String EVENT = "Anlass";
    static final String OFFER = "Volontariat";

    // UC-013 BR-005: the mark of a Feed Item whose entry was changed after its creation
    static final String UPDATED = "Aktualisiert: ";

    private static final DateTimeFormatter RFC_822 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

    // UC-013 BR-003: title; Date Text and location before the complete Description
    static FeedItem of(Event event, String link, String feedAddress) {
        String preface = "<p>" + escape(event.date) + " · " + escape(event.location) + "</p>";
        return new FeedItem(mark(event.title, event.wasChanged()), link,
                guid(feedAddress, "anlass", event.id, event.lastChange), EVENT, event.lastChange,
                preface + event.description);
    }

    // UC-013 BR-003: an offer has no title — Organisation and location name it, as on its Offer Page
    static FeedItem of(VoluntaryService offer, String link, String feedAddress) {
        return new FeedItem(mark(offer.organization + ", " + offer.location, offer.wasChanged()), link,
                guid(feedAddress, "volontariat", offer.id, offer.lastChange), OFFER, offer.lastChange,
                offer.description);
    }

    /** The moment of publication in the form RSS 2.0 asks for (RFC 822). */
    public String pubDate() {
        return RFC_822.format(lastChange);
    }

    private static String mark(String title, boolean changed) {
        return changed ? UPDATED + title : title;
    }

    // UC-013 BR-005: made of the entry and its Last Change — the same between fetches, new after a change
    private static String guid(String feedAddress, String kind, int id, Instant lastChange) {
        return feedAddress + "#" + kind + "-" + id + "-" + lastChange.toEpochMilli();
    }

    /** Plain text as HTML, so that a Date Text or location can never carry markup into a reader. */
    static String escape(String text) {
        StringBuilder html = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<' -> html.append("&lt;");
                case '>' -> html.append("&gt;");
                case '&' -> html.append("&amp;");
                case '"' -> html.append("&quot;");
                case '\'' -> html.append("&#39;");
                default -> html.append(c);
            }
        }
        return html.toString();
    }
}
