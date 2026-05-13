package com.bookwise;

import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.payment.entity.Payment;
import com.bookwise.support.IntegrationTestSupport;
import com.bookwise.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthAndCheckoutFlowTest extends IntegrationTestSupport {

    @Test
    void registrationSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Reader One")
                        .param("email", "reader1@example.com")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/auth/login?next=%2F"));

        assertThat(userRepository.findByEmailIgnoreCase("reader1@example.com")).isPresent();
    }

    @Test
    void loginSuccessCreatesJwtCookie() throws Exception {
        createUser("Reader One", "reader-login@example.com", "Password123!", UserRole.READER);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("email", "reader-login@example.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("BOOKWISE_JWT=")));
    }

    @Test
    void protectedRouteRequiresJwt() throws Exception {
        mockMvc.perform(get("/my-library"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/auth/login?next=%2Fmy-library"));
    }

    @Test
    void adminRouteBlockedForReader() throws Exception {
        createUser("Reader One", "reader-admin-block@example.com", "Password123!", UserRole.READER);
        var cookie = jwtCookieFromLogin("reader-admin-block@example.com", "Password123!");

        mockMvc.perform(get("/admin").cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkoutCreatesOrderAndPaymentForValidBook() throws Exception {
        createUser("Reader One", "reader-checkout@example.com", "Password123!", UserRole.READER);
        var cookie = jwtCookieFromLogin("reader-checkout@example.com", "Password123!");
        var book = createBook("Checkout Book", 2990L, true);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(cookie)
                        .param("bookIds", book.getId().toString()))
                .andExpect(status().is3xxRedirection());

        PurchaseOrder order = orderRepository.findAll().getFirst();
        Payment payment = paymentRepository.findAll().getFirst();
        assertThat(order.getTotalAmount()).isEqualTo(2990L);
        assertThat(order.getStatus()).isEqualTo(com.bookwise.order.entity.OrderStatus.PENDING_PAYMENT);
        assertThat(payment.getStatus()).isEqualTo(com.bookwise.payment.entity.PaymentStatus.WAITING_FOR_TRANSFER);
    }

    @Test
    void checkoutRejectsUnpublishedBook() throws Exception {
        createUser("Reader One", "reader-unpublished@example.com", "Password123!", UserRole.READER);
        var cookie = jwtCookieFromLogin("reader-unpublished@example.com", "Password123!");
        var book = createBook("Hidden Book", 2990L, false);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(cookie)
                        .param("bookIds", book.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/books"));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void checkoutRejectsAlreadyOwnedBook() throws Exception {
        var user = createUser("Reader One", "reader-owned@example.com", "Password123!", UserRole.READER);
        var cookie = jwtCookieFromLogin("reader-owned@example.com", "Password123!");
        var book = createBook("Owned Book", 2990L, true);
        createPaidOwnership(user, book);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(cookie)
                        .param("bookIds", book.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/books"));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void checkoutCalculatesTotalOnServer() throws Exception {
        createUser("Reader One", "reader-total@example.com", "Password123!", UserRole.READER);
        var cookie = jwtCookieFromLogin("reader-total@example.com", "Password123!");
        var book1 = createBook("Book One", 1990L, true);
        var book2 = createBook("Book Two", 3490L, true);

        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .cookie(cookie)
                        .param("bookIds", book1.getId().toString())
                        .param("bookIds", book2.getId().toString()))
                .andExpect(status().is3xxRedirection());

        PurchaseOrder order = orderRepository.findAll().getFirst();
        assertThat(order.getTotalAmount()).isEqualTo(5480L);
    }
}
