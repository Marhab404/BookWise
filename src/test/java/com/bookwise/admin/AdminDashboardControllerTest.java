package com.bookwise.admin;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bookwise.admin.dto.BarSeries;
import com.bookwise.admin.dto.KpiCard;
import com.bookwise.admin.dto.PieSlice;
import com.bookwise.admin.dto.TopBook;
import com.bookwise.security.AppUserPrincipal;
import com.bookwise.user.entity.UserRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService dashboardService;

    private AppUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new AppUserPrincipal(1L, "Admin User", "admin@bookwise.com", UserRole.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities())
        );

        when(dashboardService.prepareKpiCards()).thenReturn(List.of(
                new KpiCard("Users", 5L, ""),
                new KpiCard("Books", 10L, "")
        ));
        when(dashboardService.prepareUsersByRole()).thenReturn(List.of(
                new PieSlice("Admins", 1L, "#14b8a6"),
                new PieSlice("Readers", 4L, "#0f766e")
        ));
        when(dashboardService.prepareOrdersByStatus()).thenReturn(List.of());
        when(dashboardService.preparePaymentsByStatus()).thenReturn(List.of());
        when(dashboardService.prepareMonthlyRevenue()).thenReturn(
                new BarSeries("Revenue", List.of("Jan", "Feb"), List.of(100L, 200L)));
        when(dashboardService.prepareMonthlyOrderCount()).thenReturn(
                new BarSeries("Orders", List.of("Jan", "Feb"), List.of(1L, 2L)));
        when(dashboardService.prepareTopSellingBooks()).thenReturn(List.of(
                new TopBook("Book A", 5L)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboard_shouldIncludeAllModelAttributes() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("kpiCards"))
                .andExpect(model().attributeExists("usersByRole"))
                .andExpect(model().attributeExists("ordersByStatus"))
                .andExpect(model().attributeExists("paymentsByStatus"))
                .andExpect(model().attributeExists("monthlyRevenue"))
                .andExpect(model().attributeExists("monthlyOrders"))
                .andExpect(model().attributeExists("topBooks"));
    }

    @Test
    void unauthenticated_shouldRedirectToLogin() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }
}
