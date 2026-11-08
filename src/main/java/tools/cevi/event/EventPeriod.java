package tools.cevi.event;

import tools.cevi.infra.ValidationMessage;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The First Day and Last Day as the event form submits them, read by the rules of UC-002 BR-012.
 *
 * <p>Unlike the display date (UC-002 BR-008), a value that cannot be read is not ignored: keeping
 * the stored day would let the Calendar Entry show a period the administrator just tried to
 * change. So every fault becomes a {@link ValidationMessage} on the field it concerns, and the
 * days are only taken over when there is none.
 *
 * @param firstDay   the First Day, {@code null} when the event has no period or the entry is faulty
 * @param lastDay    the Last Day, {@code null} when the event has no period or the entry is faulty
 * @param violations what the administrator has to correct; empty when the entry is valid
 */
public record EventPeriod(LocalDate firstDay, LocalDate lastDay, Set<ValidationMessage> violations) {

    static final String FIRST_DAY = "firstDay";
    static final String LAST_DAY = "lastDay";

    static final String UNREADABLE = "Kein gültiges Datum";
    static final String MISSING_PARTNER = "Erster und Letzter Tag nur gemeinsam angeben";
    static final String WRONG_ORDER = "Der Letzte Tag darf nicht vor dem Ersten Tag liegen";

    public EventPeriod {
        violations = Collections.unmodifiableSet(new LinkedHashSet<>(violations));
    }

    /**
     * @param firstDay the submitted First Day as {@code yyyy-MM-dd}, blank or {@code null} for none
     * @param lastDay  the submitted Last Day as {@code yyyy-MM-dd}, blank or {@code null} for none
     */
    // UC-002 BR-012: both or neither, readable, the Last Day not before the First Day
    public static EventPeriod read(String firstDay, String lastDay) {
        boolean firstGiven = isGiven(firstDay);
        boolean lastGiven = isGiven(lastDay);
        if (!firstGiven && !lastGiven) {
            // UC-002 BR-012: emptying both removes the period, and with it the Calendar Entry
            return new EventPeriod(null, null, Set.of());
        }

        Set<ValidationMessage> violations = new LinkedHashSet<>();
        LocalDate first = parse(firstDay, FIRST_DAY, firstGiven, violations);
        LocalDate last = parse(lastDay, LAST_DAY, lastGiven, violations);
        if (first != null && last != null && last.isBefore(first)) {
            violations.add(ValidationMessage.of(LAST_DAY, WRONG_ORDER));
        }
        return violations.isEmpty()
                ? new EventPeriod(first, last, Set.of())
                : new EventPeriod(null, null, violations);
    }

    public boolean valid() {
        return violations.isEmpty();
    }

    private static LocalDate parse(String value, String field, boolean given, Set<ValidationMessage> violations) {
        if (!given) {
            violations.add(ValidationMessage.of(field, MISSING_PARTNER));
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            violations.add(ValidationMessage.of(field, UNREADABLE));
            return null;
        }
    }

    private static boolean isGiven(String value) {
        return value != null && !value.isBlank();
    }
}
