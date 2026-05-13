package com.bookwise.payment.validation;

import com.bookwise.config.StorageProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ValidPaymentProofFileValidator implements ConstraintValidator<ValidPaymentProofFile, MultipartFile> {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private final StorageProperties storageProperties;

    public ValidPaymentProofFileValidator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.getSize() > storageProperties.maxPaymentProofSizeBytes()) {
            return false;
        }
        String contentType = value.getContentType();
        if (contentType == null) {
            return false;
        }
        return ALLOWED_TYPES.contains(contentType);
    }
}
