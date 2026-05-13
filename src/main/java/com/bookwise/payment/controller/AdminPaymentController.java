package com.bookwise.payment.controller;

import com.bookwise.payment.dto.PaymentReviewForm;
import com.bookwise.payment.entity.Payment;
import com.bookwise.payment.service.PaymentService;
import com.bookwise.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("payments", paymentService.listPendingReviews());
        return "admin/payments/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Payment payment = paymentService.getForAdmin(id);
        model.addAttribute("payment", payment);
        model.addAttribute("paymentReviewForm", new PaymentReviewForm());
        return "admin/payments/detail";
    }

    @GetMapping("/{id}/proof")
    public ResponseEntity<Resource> proof(@PathVariable Long id) {
        Payment payment = paymentService.getForAdmin(id);
        if (payment.getReceiptFilePath() == null || payment.getReceiptFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = paymentService.getProofResource(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (payment.getReceiptContentType() != null && !payment.getReceiptContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(payment.getReceiptContentType());
        }
        String fileName = payment.getReceiptFileName() != null ? payment.getReceiptFileName() : "payment-proof";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/approve")
    public String approve(@AuthenticationPrincipal AppUserPrincipal principal,
                          @PathVariable Long id,
                          @Valid @ModelAttribute("paymentReviewForm") PaymentReviewForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        try {
            paymentService.approve(id, principal.id(), form.getAdminReviewNote());
            redirectAttributes.addFlashAttribute("flashMessage", "Payment approved");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@AuthenticationPrincipal AppUserPrincipal principal,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("paymentReviewForm") PaymentReviewForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        try {
            paymentService.reject(id, principal.id(), form.getAdminReviewNote());
            redirectAttributes.addFlashAttribute("flashMessage", "Payment rejected");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }
}
