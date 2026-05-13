package com.bookwise.order.controller;

import com.bookwise.common.SafeRedirects;
import com.bookwise.order.dto.CheckoutPreview;
import com.bookwise.order.dto.CheckoutRequestForm;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.order.service.CheckoutService;
import com.bookwise.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @GetMapping
    public String preview(@AuthenticationPrincipal AppUserPrincipal principal, @ModelAttribute CheckoutRequestForm form, Model model, RedirectAttributes redirectAttributes) {
        try {
            CheckoutPreview preview = checkoutService.previewCheckout(principal.id(), form.getBookIds());
            model.addAttribute("preview", preview);
            model.addAttribute("checkoutRequestForm", form);
            return "checkout/summary";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/books";
        }
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AppUserPrincipal principal,
                         @Valid @ModelAttribute("checkoutRequestForm") CheckoutRequestForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("preview", new CheckoutPreview(List.of(), 0L, "MAD"));
            return "checkout/summary";
        }
        try {
            PurchaseOrder order = checkoutService.createOrder(principal.id(), form.getBookIds());
            redirectAttributes.addFlashAttribute("flashMessage", "Order created. Please submit your payment proof.");
            return "redirect:/orders/" + order.getId() + "/payment";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/books";
        }
    }
}
