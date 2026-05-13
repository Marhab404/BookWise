package com.bookwise.auth.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface PasswordMatches {

    String message() default "Password confirmation does not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
