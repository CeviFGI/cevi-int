package tools.cevi.infra;

import io.quarkus.qute.TemplateExtension;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Makes the presentation helpers reachable from Qute without putting logic into the templates.
 *
 * <p>The methods hang off the type of the value they read, so a template writes
 * {@code {event.description.excerpt}} and {@code {event.displayDate.countdown}}. A template
 * extension is matched by its method name, so each name here is exactly the property a template
 * asks for — there is no {@code isX} to {@code x} translation as there is for a bean getter.
 */
@TemplateExtension
public final class TemplateExtensions {

    private TemplateExtensions() {
    }

    /**
     * The value as one query parameter of a link, as the search encodes it. An entered Short Name is
     * stored as typed (UC-004 BR-009), so it may hold {@code &}, {@code #}, {@code +} or
     * {@code %}; Qute escapes it for HTML only, which would let such a name break the link.
     */
    // UC-001 BR-003, UC-003 BR-005: the entry leads to the page under exactly its Short Name
    public static String queryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Shortens a stored description for a list (UC-001 BR-004, UC-003 BR-004). Rendered escaped, never raw. */
    public static String excerpt(String description) {
        return Excerpt.from(description);
    }

    /**
     * The description as the detail page renders it: unchanged apart from a scroll container
     * around each table, so a wide one cannot take the page sideways (NFR-013, {@link ProseTables}).
     */
    public static String withScrollableTables(String description) {
        return ProseTables.wrapped(description);
    }

    /** One of four accent treatments, stable for a given key (see {@link AccentVariant}). */
    public static int accentVariant(String key) {
        return AccentVariant.of(key);
    }

    /** The same, for a record identified by a number rather than by a slug. */
    public static int accentVariant(Integer key) {
        return AccentVariant.of(String.valueOf(key));
    }

    /** How much time the event has left, in German, or an empty string (UC-001 BR-005). */
    public static String countdown(LocalDate displayDate) {
        return Countdown.text(displayDate);
    }

    /** Whether that remaining time is short enough to point out rather than merely state. */
    public static boolean countdownUrgent(LocalDate displayDate) {
        return Countdown.isUrgent(displayDate);
    }
}
