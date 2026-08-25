package tools.cevi.infra;

import io.quarkus.qute.TemplateExtension;

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

    /** Shortens a stored description for a list (BR-040, BR-044). Rendered escaped, never raw. */
    public static String excerpt(String description) {
        return Excerpt.from(description);
    }

    /** Whether the excerpt left something out, so the card can offer to show the rest. */
    public static boolean shortened(String description) {
        return !Excerpt.from(description).equals(Excerpt.from(description, Integer.MAX_VALUE));
    }

    /** One of four accent treatments, stable for a given key (see {@link AccentVariant}). */
    public static int accentVariant(String key) {
        return AccentVariant.of(key);
    }

    /** The same, for a record identified by a number rather than by a slug. */
    public static int accentVariant(Integer key) {
        return AccentVariant.of(String.valueOf(key));
    }

    /** How much time the event has left, in German, or an empty string (BR-041). */
    public static String countdown(LocalDate displayDate) {
        return Countdown.text(displayDate);
    }

    /** Whether that remaining time is short enough to point out rather than merely state. */
    public static boolean countdownUrgent(LocalDate displayDate) {
        return Countdown.isUrgent(displayDate);
    }
}
