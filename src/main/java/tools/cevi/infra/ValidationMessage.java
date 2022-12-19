package tools.cevi.infra;

import javax.validation.ConstraintViolation;

public record ValidationMessage(String fieldName, String message) {
    public static <T> ValidationMessage of(ConstraintViolation<T> violation) {
        return new ValidationMessage(violation.getPropertyPath().toString(), violation.getMessage());
    }
}
