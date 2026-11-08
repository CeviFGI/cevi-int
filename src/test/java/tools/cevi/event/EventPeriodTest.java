package tools.cevi.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.cevi.infra.ValidationMessage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-002 BR-012: how the First Day and Last Day of the event form are read. */
class EventPeriodTest {

    @Test
    void no_period_at_all_is_valid() {
        for (String empty : new String[]{null, "", "   "}) {
            EventPeriod period = EventPeriod.read(empty, empty);
            assertTrue(period.valid());
            assertNull(period.firstDay());
            assertNull(period.lastDay());
        }
    }

    @Test
    void a_period_of_several_days_is_taken_over() {
        EventPeriod period = EventPeriod.read("2099-07-01", "2099-07-05");

        assertTrue(period.valid());
        assertEquals(LocalDate.of(2099, 7, 1), period.firstDay());
        assertEquals(LocalDate.of(2099, 7, 5), period.lastDay());
    }

    /** An event of a single day has the same date in both. */
    @Test
    void a_single_day_is_valid() {
        EventPeriod period = EventPeriod.read("2099-07-01", "2099-07-01");

        assertTrue(period.valid());
        assertEquals(period.firstDay(), period.lastDay());
    }

    /** Neither day is compared with today: a period may lie in the past. */
    @Test
    void a_period_in_the_past_is_valid() {
        assertTrue(EventPeriod.read("2001-01-01", "2001-01-02").valid());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "2099-07-01, '',         lastDay,  " + EventPeriod.MISSING_PARTNER,
            "'',         2099-07-01, firstDay, " + EventPeriod.MISSING_PARTNER,
            "01.07.2099, 2099-07-05, firstDay, " + EventPeriod.UNREADABLE,
            "2099-07-01, Juli,       lastDay,  " + EventPeriod.UNREADABLE,
            "2099-02-30, 2099-03-01, firstDay, " + EventPeriod.UNREADABLE,
            "2099-07-05, 2099-07-04, lastDay,  " + EventPeriod.WRONG_ORDER,
    })
    void a_faulty_period_is_rejected_on_the_field_it_concerns(String first, String last, String field, String message) {
        EventPeriod period = EventPeriod.read(first, last);

        assertFalse(period.valid());
        assertEquals(List.of(ValidationMessage.of(field, message)), List.copyOf(period.violations()));
        assertNull(period.firstDay());
        assertNull(period.lastDay());
    }

    @Test
    void both_unreadable_marks_both_fields_in_form_order() {
        EventPeriod period = EventPeriod.read("x", "y");

        assertEquals(List.of(ValidationMessage.of("firstDay", EventPeriod.UNREADABLE),
                ValidationMessage.of("lastDay", EventPeriod.UNREADABLE)), List.copyOf(period.violations()));
    }
}
