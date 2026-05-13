package com.bookwise.payment.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidPaymentProofFileValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface ValidPaymentProofFile {

    String message() default "Receipt file must be a PDF, PNG, JPG, or JPEG file";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
