package com.bookwise.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.bank-transfer")
public record BankTransferProperties(
        @NotBlank String bankName,
        @NotBlank String accountHolderName,
        @NotBlank String iban,
        @NotBlank String referenceInstruction
) {
}
