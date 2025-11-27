package com.campusnest.housingservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDate;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {
    
    private String availableFromField;
    private String availableToField;
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.availableFromField = constraintAnnotation.availableFromField();
        this.availableToField = constraintAnnotation.availableToField();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        try {
            Field availableFromFieldObj = value.getClass().getDeclaredField(availableFromField);
            Field availableToFieldObj = value.getClass().getDeclaredField(availableToField);
            
            availableFromFieldObj.setAccessible(true);
            availableToFieldObj.setAccessible(true);
            
            LocalDate availableFrom = (LocalDate) availableFromFieldObj.get(value);
            LocalDate availableTo = (LocalDate) availableToFieldObj.get(value);
            
            if (availableFrom == null || availableTo == null) {
                return true; // Let @NotNull handle null validation
            }
            
            return availableTo.isAfter(availableFrom);
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }
}