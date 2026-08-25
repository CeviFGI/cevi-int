package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountdownTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 25);

    @Test
    void an_event_without_a_date_says_nothing() {
        assertEquals("", Countdown.text(null, TODAY));
        assertFalse(Countdown.isUrgent(null, TODAY));
    }

    /** Such an event is not listed at all, so there is nothing to say about it. */
    @Test
    void a_date_that_has_passed_says_nothing() {
        assertEquals("", Countdown.text(TODAY.minusDays(1), TODAY));
        assertFalse(Countdown.isUrgent(TODAY.minusDays(1), TODAY));
    }

    @Test
    void the_last_day_is_named_as_such() {
        assertEquals("Heute letzter Tag", Countdown.text(TODAY, TODAY));
        assertTrue(Countdown.isUrgent(TODAY, TODAY));
    }

    @Test
    void a_single_day_is_singular() {
        assertEquals("Noch 1 Tag", Countdown.text(TODAY.plusDays(1), TODAY));
    }

    @Test
    void a_few_days_are_counted_in_days() {
        assertEquals("Noch 2 Tage", Countdown.text(TODAY.plusDays(2), TODAY));
        assertEquals("Noch 42 Tage", Countdown.text(TODAY.plusDays(42), TODAY));
        assertEquals("Noch 60 Tage", Countdown.text(TODAY.plusDays(60), TODAY));
    }

    @Test
    void beyond_two_months_the_count_turns_into_months() {
        assertEquals("Noch 2 Monate", Countdown.text(TODAY.plusDays(61), TODAY));
        assertEquals("Noch 12 Monate", Countdown.text(TODAY.plusDays(365), TODAY));
    }

    @Test
    void only_a_short_remaining_time_is_pointed_out() {
        assertTrue(Countdown.isUrgent(TODAY.plusDays(14), TODAY));
        assertFalse(Countdown.isUrgent(TODAY.plusDays(15), TODAY));
    }

    /** The default overload reads today's date; it must agree with the explicit one. */
    @Test
    void the_convenience_overload_counts_from_today() {
        LocalDate today = LocalDate.now();

        assertEquals(Countdown.text(today.plusDays(3), today), Countdown.text(today.plusDays(3)));
        assertEquals(Countdown.isUrgent(today.plusDays(3), today), Countdown.isUrgent(today.plusDays(3)));
    }
}
