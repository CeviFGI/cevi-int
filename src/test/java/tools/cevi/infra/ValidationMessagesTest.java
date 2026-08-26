package tools.cevi.infra;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * The lookup a form template does once per control (BR-045). It is worth its own test because the
 * alternative — a loop and a comparison inside the template — is the least testable place in the
 * application, which is exactly why it lives here.
 */
class ValidationMessagesTest {

    @Test
    void a_clean_form_reports_no_fault_at_all() {
        ValidationMessages messages = ValidationMessages.none();

        assertThat(messages.any(), is(false));
        assertThat(messages.messageFor("message"), is(nullValue()));
    }

    @Test
    void a_field_without_a_fault_stays_null_even_when_others_failed() {
        ValidationMessages messages = ValidationMessages.of(List.of(
                ValidationMessage.of("spam", "Bitte trage die Zahl 50 ein.")));

        assertThat(messages.any(), is(true));
        assertThat(messages.messageFor("spam"), is("Bitte trage die Zahl 50 ein."));
        assertThat(messages.messageFor("message"), is(nullValue()));
    }

    /** The summary reads top to bottom, so it has to list the faults the way the form does. */
    @Test
    void the_order_the_resource_collected_them_in_is_kept() {
        ValidationMessages messages = ValidationMessages.of(List.of(
                ValidationMessage.of("title", "fehlt"),
                ValidationMessage.of("slug", "belegt"),
                ValidationMessage.of("location", "fehlt")));

        assertThat(messages.all().stream().map(ValidationMessage::fieldName).toList(),
                contains("title", "slug", "location"));
    }

    /** Two faults on one field would otherwise render twice under the same control. */
    @Test
    void only_the_first_fault_of_a_field_is_shown_at_that_field() {
        ValidationMessages messages = ValidationMessages.of(List.of(
                ValidationMessage.of("slug", "belegt"),
                ValidationMessage.of("slug", "zu lang")));

        assertThat(messages.messageFor("slug"), is("belegt"));
    }

    /** A summary entry has to name the field the way the form labelled it, not internally. */
    @Test
    void a_field_is_named_the_way_the_form_labels_it() {
        assertThat(ValidationMessage.of("organizationLink", "x").label(), is("Link zur Organisation"));
        assertThat(ValidationMessage.of("spam", "x").label(), is("Sicherheitsfrage"));
    }

    /** A field nobody added a label for is still recognisable rather than blank. */
    @Test
    void an_unknown_field_falls_back_to_its_technical_name() {
        assertThat(ValidationMessage.of("somethingNew", "x").label(), is("somethingNew"));
    }
}
