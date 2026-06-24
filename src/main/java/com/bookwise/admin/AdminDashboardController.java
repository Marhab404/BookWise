package com.bookwise.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("kpiCards", dashboardService.prepareKpiCards());
        model.addAttribute("usersByRole", dashboardService.prepareUsersByRole());
        model.addAttribute("ordersByStatus", dashboardService.prepareOrdersByStatus());
        model.addAttribute("paymentsByStatus", dashboardService.preparePaymentsByStatus());
        model.addAttribute("monthlyRevenue", dashboardService.prepareMonthlyRevenue());
        model.addAttribute("monthlyOrders", dashboardService.prepareMonthlyOrderCount());
        model.addAttribute("topBooks", dashboardService.prepareTopSellingBooks());
        return "admin/dashboard";
    }
}
