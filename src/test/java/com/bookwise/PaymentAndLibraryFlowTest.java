package com.bookwise;

import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.payment.entity.PaymentStatus;
import com.bookwise.support.IntegrationTestSupport;
import com.bookwise.user.entity.UserRole;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentAndLibraryFlowTest extends IntegrationTestSupport {

    @Test
    void proofSubmissionAndApprovalUnlockLibraryAccess() throws Exception {
        var reader = createUser("Reader One", "reader-payment@example.com", "Password123!", UserRole.READER);
        var admin = createUser("Admin One", "admin-payment@example.com", "Password123!", UserRole.ADMIN);
        var readerCookie = jwtCookieFromLogin("reader-payment@example.com", "Password123!");
        var adminCookie = jwtCookieFromLogin("admin-payment@example.com", "Password123!");
        var book = createBook("Payment Book", 4990L, true);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(readerCookie)
                        .param("bookIds", book.getId().toString()))
                .andExpect(status().is3xxRedirection());

        PurchaseOrder order = orderRepository.findAll().getFirst();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/orders/" + order.getId() + "/payment")
                        .file(new MockMultipartFile("receiptFile", "receipt.pdf", "application/pdf", "proof".getBytes(StandardCharsets.UTF_8)))
                        .param("transferReference", "REF-123")
                        .with(csrf())
                        .cookie(readerCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-orders/" + order.getId()));

        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PROOF_SUBMITTED);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PROOF_SUBMITTED);

        var payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        mockMvc.perform(post("/admin/payments/" + payment.getId() + "/approve")
                        .with(csrf())
                        .cookie(adminCookie)
                        .param("adminReviewNote", "Looks good"))
                .andExpect(status().is3xxRedirection());

        order = orderRepository.findById(order.getId()).orElseThrow();
        payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        mockMvc.perform(get("/my-library").cookie(readerCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment Book")));

        mockMvc.perform(get("/my-library/books/" + book.getId() + "/read").cookie(readerCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BookWise demo file")));
    }

    @Test
    void proofSubmissionAndRejectionUpdateStatuses() throws Exception {
        createUser("Reader One", "reader-reject@example.com", "Password123!", UserRole.READER);
        createUser("Admin One", "admin-reject@example.com", "Password123!", UserRole.ADMIN);
        var readerCookie = jwtCookieFromLogin("reader-reject@example.com", "Password123!");
        var adminCookie = jwtCookieFromLogin("admin-reject@example.com", "Password123!");
        var book = createBook("Rejected Book", 3990L, true);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(readerCookie)
                        .param("bookIds", book.getId().toString()))
                .andExpect(status().is3xxRedirection());

        PurchaseOrder order = orderRepository.findAll().getFirst();
        mockMvc.perform(MockMvcRequestBuilders.multipart("/orders/" + order.getId() + "/payment")
                        .file(new MockMultipartFile("receiptFile", "receipt.pdf", "application/pdf", "proof".getBytes(StandardCharsets.UTF_8)))
                        .with(csrf())
                        .cookie(readerCookie))
                .andExpect(status().is3xxRedirection());

        var payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        mockMvc.perform(post("/admin/payments/" + payment.getId() + "/reject")
                        .with(csrf())
                        .cookie(adminCookie)
                        .param("adminReviewNote", "Mismatch"))
                .andExpect(status().is3xxRedirection());

        order = orderRepository.findById(order.getId()).orElseThrow();
        payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    void fileAccessDeniedBeforeApprovalAndForOtherUser() throws Exception {
        var owner = createUser("Reader One", "reader-owner@example.com", "Password123!", UserRole.READER);
        var other = createUser("Reader Two", "reader-other@example.com", "Password123!", UserRole.READER);
        var ownerCookie = jwtCookieFromLogin("reader-owner@example.com", "Password123!");
        var otherCookie = jwtCookieFromLogin("reader-other@example.com", "Password123!");
        createUser("Admin One", "admin-other@example.com", "Password123!", UserRole.ADMIN);
        var adminCookie = jwtCookieFromLogin("admin-other@example.com", "Password123!");
        var book = createBook("Private Book", 2990L, true);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(ownerCookie)
                        .param("bookIds", book.getId().toString()))
                .andExpect(status().is3xxRedirection());

        PurchaseOrder order = orderRepository.findAll().getFirst();
        mockMvc.perform(MockMvcRequestBuilders.multipart("/orders/" + order.getId() + "/payment")
                        .file(new MockMultipartFile("receiptFile", "receipt.pdf", "application/pdf", "proof".getBytes(StandardCharsets.UTF_8)))
                        .with(csrf())
                        .cookie(ownerCookie))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/my-library/books/" + book.getId() + "/read").cookie(ownerCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/my-library").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private Book"))));

        var payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        mockMvc.perform(post("/admin/payments/" + payment.getId() + "/approve")
                        .with(csrf())
                        .cookie(adminCookie))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/my-library/books/" + book.getId() + "/read").cookie(otherCookie))
                .andExpect(status().isForbidden());
    }
}
