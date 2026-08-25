package tools.cevi.infra;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * States how much time an event has left before it drops off the list (BR-041).
 *
 * <p>It reads the same {@code displayDate} that decides whether the event is listed at all, so it
 * needs no new field and cannot disagree with the list it appears in.
 */
public final class Countdown {

    /** Below this, the remaining time is worth pointing out rather than merely stating. */
    private static final long URGENT_DAYS = 14;

    /** Above this, a day count stops being useful and turns into months. */
    private static final long DAYS_SHOWN_EXACTLY = 60;

    private static final double DAYS_PER_MONTH = 30.44;

    private Countdown() {
    }

    public static String text(LocalDate displayDate) {
        return text(displayDate, LocalDate.now());
    }

    /**
     * @param displayDate the date until which the event stays listed
     * @param today       the day to count from
     * @return German wording for the remaining time, or an empty string when the date has passed —
     *         such an event is not listed, so there is nothing to say about it
     */
    public static String text(LocalDate displayDate, LocalDate today) {
        if (displayDate == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(today, displayDate);

        if (days < 0) {
            return "";
        }
        if (days == 0) {
            return "Heute letzter Tag";
        }
        if (days == 1) {
            return "Noch 1 Tag";
        }
        if (days <= DAYS_SHOWN_EXACTLY) {
            return "Noch " + days + " Tage";
        }

        long months = Math.round(days / DAYS_PER_MONTH);
        return months <= 1 ? "Noch 1 Monat" : "Noch " + months + " Monate";
    }

    public static boolean isUrgent(LocalDate displayDate) {
        return isUrgent(displayDate, LocalDate.now());
    }

    /** @return whether the remaining time should be pointed out rather than merely stated */
    public static boolean isUrgent(LocalDate displayDate, LocalDate today) {
        if (displayDate == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(today, displayDate);
        return days >= 0 && days <= URGENT_DAYS;
    }
}
