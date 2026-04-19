package lk.pharmacy.inventory.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom annotation for validating dd-mm-yyyy date format.
 * Can be applied to String fields that should contain dates in dd-mm-yyyy format.
 */
@Documented
@Constraint(validatedBy = DDMMYYYYDateFormatValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateFormat {
    String message() default "Invalid date format. Expected: dd-mm-yyyy";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

/**
 * Validator for dd-mm-yyyy date format
 */
class DDMMYYYYDateFormatValidator implements ConstraintValidator<ValidDateFormat, String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public void initialize(ValidDateFormat constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // null values are valid, use @NotNull for null checks
        }

        try {
            LocalDate.parse(value, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid date format. Expected: dd-mm-yyyy, got: " + value)
                    .addConstraintViolation();
            return false;
        }
    }
}

