package com.bookwise.order.controller;

import com.bookwise.config.BankTransferProperties;
import com.bookwise.order.dto.CheckoutRequestForm;
import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.order.service.OrderService;
import com.bookwise.payment.dto.PaymentProofSubmissionForm;
import com.bookwise.payment.entity.Payment;
import com.bookwise.payment.service.PaymentService;
import com.bookwise.security.AppUserPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final BankTransferProperties bankTransferProperties;

    public OrderController(OrderService orderService, PaymentService paymentService, BankTransferProperties bankTransferProperties) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.bankTransferProperties = bankTransferProperties;
    }

    @GetMapping("/orders/{id}/payment")
    public String paymentPage(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id, Model model) {
        PurchaseOrder order = orderService.getForUser(id, principal.id());
        Payment payment = paymentService.getForOrder(id);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("bankTransfer", bankTransferProperties);
        model.addAttribute("paymentProofSubmissionForm", new PaymentProofSubmissionForm());
        return "orders/payment";
    }

    @PostMapping("/orders/{id}/payment")
    public String submitProof(@AuthenticationPrincipal AppUserPrincipal principal,
                              @PathVariable Long id,
                              @Valid @ModelAttribute("paymentProofSubmissionForm") PaymentProofSubmissionForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        PurchaseOrder order = orderService.getForUser(id, principal.id());
        if (bindingResult.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("payment", paymentService.getForOrder(id));
            model.addAttribute("bankTransfer", bankTransferProperties);
            return "orders/payment";
        }
        try {
            paymentService.submitProof(principal.id(), id, form);
            redirectAttributes.addFlashAttribute("flashMessage", "Your payment proof has been submitted and is awaiting validation.");
            return "redirect:/my-orders/" + id;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/my-orders/" + id;
        }
    }

    @GetMapping("/my-orders")
    public String orders(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("orders", orderService.listForUser(principal.id()));
        return "orders/list";
    }

    @GetMapping("/my-orders/{id}")
    public String orderDetail(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id, Model model) {
        PurchaseOrder order = orderService.getForUser(id, principal.id());
        model.addAttribute("order", order);
        model.addAttribute("bankTransfer", bankTransferProperties);
        model.addAttribute("paymentProofSubmissionForm", new PaymentProofSubmissionForm());
        return "orders/detail";
    }
}
