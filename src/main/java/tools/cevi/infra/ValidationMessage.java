package tools.cevi.infra;

import jakarta.validation.ConstraintViolation;

public record ValidationMessage(String fieldName, String message) {
    public static <T> ValidationMessage of(ConstraintViolation<T> violation) {
        return new ValidationMessage(violation.getPropertyPath().toString(), violation.getMessage());
    }

    public static ValidationMessage of(String fieldname, String message) {
        return new ValidationMessage(fieldname, message);
    }
}
