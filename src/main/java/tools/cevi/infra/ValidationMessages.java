package tools.cevi.infra;

import java.util.Collection;
import java.util.List;

import io.quarkus.qute.TemplateData;

/**
 * The faults of one submission, in the shape a form template can bind per field (FR-037, BR-045).
 *
 * <p>The resources collect violations as a flat collection; a form has to ask the opposite
 * question — "is <em>this</em> control faulty, and what does it say?" — once per control. Doing
 * that with a loop and a comparison inside the template would put the logic in the least testable
 * place there is, so it lives here. Nothing about the model changes: the same
 * {@link ValidationMessage} values are carried, only addressable.
 */
@TemplateData
public record ValidationMessages(List<ValidationMessage> all) {

    private static final ValidationMessages NONE = new ValidationMessages(List.of());

    public ValidationMessages {
        all = List.copyOf(all);
    }

    /** A clean form. */
    public static ValidationMessages none() {
        return NONE;
    }

    /** Keeps the order it is given, so the summary lists the faults the way the form reads. */
    public static ValidationMessages of(Collection<ValidationMessage> messages) {
        return messages.isEmpty() ? NONE : new ValidationMessages(List.copyOf(messages));
    }

    /** Whether anything was rejected — the condition the error summary hangs off. */
    public boolean any() {
        return !all.isEmpty();
    }

    /** The fault recorded for a field, or {@code null} when the field is clean. */
    public String messageFor(String fieldName) {
        return all.stream()
                .filter(message -> message.fieldName().equals(fieldName))
                .map(ValidationMessage::message)
                .findFirst()
                .orElse(null);
    }
}
