package com.bookwise.payment.dto;

import com.bookwise.payment.validation.ValidPaymentProofFile;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PaymentProofSubmissionForm {

    @NotNull(message = "Receipt file is required")
    @ValidPaymentProofFile
    private MultipartFile receiptFile;

    private String transferReference;
}
