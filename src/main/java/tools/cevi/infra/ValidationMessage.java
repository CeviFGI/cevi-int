package tools.cevi.infra;

import java.util.Map;

import jakarta.validation.ConstraintViolation;

/**
 * One rejected field of one submission.
 *
 * <p>The record carries the technical name of the field because that is what a constraint
 * violation knows; that name is also the id of the control, so the error summary can link
 * straight at it. {@link #label()} turns it into what the visitor read above that control
 * (BR-045).
 */
public record ValidationMessage(String fieldName, String message) {

    /**
     * The label of each form control, so that a summary entry names the field the way the form
     * does. Anything not listed falls back to the technical name — a new field shows up readably
     * enough to be noticed and added here.
     */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("message", "Deine Nachricht"),
            Map.entry("spam", "Sicherheitsfrage"),
            Map.entry("title", "Titel"),
            Map.entry("slug", "Slug"),
            Map.entry("date", "Datum"),
            Map.entry("displayDate", "Anzeigedatum"),
            Map.entry("location", "Ort"),
            Map.entry("description", "Beschreibung"),
            Map.entry("organization", "Organisation"),
            Map.entry("organizationLink", "Link zur Organisation"));

    public static <T> ValidationMessage of(ConstraintViolation<T> violation) {
        return new ValidationMessage(violation.getPropertyPath().toString(), violation.getMessage());
    }

    public static ValidationMessage of(String fieldname, String message) {
        return new ValidationMessage(fieldname, message);
    }

    /** How the form names this field. */
    public String label() {
        return LABELS.getOrDefault(fieldName, fieldName);
    }
}
