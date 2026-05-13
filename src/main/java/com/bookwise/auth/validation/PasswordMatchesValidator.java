package com.bookwise.auth.validation;

import com.bookwise.auth.dto.RegisterForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterForm> {

    @Override
    public boolean isValid(RegisterForm value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.getPassword() != null && value.getPassword().equals(value.getConfirmPassword());
    }
}
