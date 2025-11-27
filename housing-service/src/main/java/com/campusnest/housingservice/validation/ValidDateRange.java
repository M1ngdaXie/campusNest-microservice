package com.campusnest.housingservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "Available to date must be after available from date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    String availableFromField() default "availableFrom";
    String availableToField() default "availableTo";
}