package com.bookwise.admin;

import com.bookwise.book.service.BookService;
import com.bookwise.payment.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final BookService bookService;
    private final PaymentService paymentService;

    public AdminDashboardController(BookService bookService, PaymentService paymentService) {
        this.bookService = bookService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("bookCount", bookService.listAllBooks().size());
        model.addAttribute("pendingPaymentsCount", paymentService.listPendingReviews().size());
        return "admin/dashboard";
    }
}
