package com.bookwise.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.user.dto.CreateUserForm;
import com.bookwise.user.dto.EditUserForm;
import com.bookwise.user.entity.User;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import com.bookwise.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, orderRepository, paymentRepository, new BCryptPasswordEncoder());
    }

    @Test
    void create_shouldNormalizeEmailAndEncodePassword() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserForm form = new CreateUserForm();
        form.setFullName("Test User");
        form.setEmail("  Test@Example.COM  ");
        form.setPassword("secret123");
        form.setConfirmPassword("secret123");
        form.setRole("READER");

        User result = userService.create(form);

        assertEquals("test@example.com", result.getEmail());
        assertTrue(new BCryptPasswordEncoder().matches("secret123", result.getPasswordHash()));
        assertEquals(UserRole.READER, result.getRole());
    }

    @Test
    void create_shouldThrowOnDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dupe@example.com")).thenReturn(true);

        CreateUserForm form = new CreateUserForm();
        form.setFullName("Dupe");
        form.setEmail("dupe@example.com");
        form.setPassword("pass");
        form.setConfirmPassword("pass");
        form.setRole("READER");

        assertThrows(IllegalArgumentException.class, () -> userService.create(form));
    }

    @Test
    void create_shouldThrowOnPasswordMismatch() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

        CreateUserForm form = new CreateUserForm();
        form.setFullName("Test");
        form.setEmail("test@example.com");
        form.setPassword("password1");
        form.setConfirmPassword("password2");
        form.setRole("READER");

        assertThrows(IllegalArgumentException.class, () -> userService.create(form));
    }

    @Test
    void update_shouldAllowEmailChange() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("old@example.com");
        existing.setPasswordHash(new BCryptPasswordEncoder().encode("oldpass"));
        existing.setRole(UserRole.READER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EditUserForm form = new EditUserForm();
        form.setFullName("Updated Name");
        form.setEmail("new@example.com");
        form.setRole("ADMIN");
        form.setPassword("");
        form.setConfirmPassword("");

        User result = userService.update(1L, form);

        assertEquals("new@example.com", result.getEmail());
        assertEquals("Updated Name", result.getFullName());
        assertEquals(UserRole.ADMIN, result.getRole());
    }

    @Test
    void update_shouldResetPasswordWhenProvided() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("u@example.com");
        existing.setPasswordHash(new BCryptPasswordEncoder().encode("oldpass"));
        existing.setRole(UserRole.READER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("u@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EditUserForm form = new EditUserForm();
        form.setFullName("User");
        form.setEmail("u@example.com");
        form.setRole("READER");
        form.setPassword("newPass123");
        form.setConfirmPassword("newPass123");

        User result = userService.update(1L, form);

        assertTrue(new BCryptPasswordEncoder().matches("newPass123", result.getPasswordHash()));
    }

    @Test
    void update_shouldThrowOnDuplicateEmail() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("original@example.com");
        existing.setRole(UserRole.READER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 1L)).thenReturn(true);

        EditUserForm form = new EditUserForm();
        form.setFullName("X");
        form.setEmail("taken@example.com");
        form.setRole("READER");

        assertThrows(IllegalArgumentException.class, () -> userService.update(1L, form));
    }

    @Test
    void delete_shouldPreventSelfDeletion() {
        assertThrows(IllegalArgumentException.class, () -> userService.delete(5L, 5L));
    }

    @Test
    void delete_shouldPreventDeletingLastAdmin() {
        User lastAdmin = new User();
        lastAdmin.setId(1L);
        lastAdmin.setRole(UserRole.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> userService.delete(1L, 99L));
    }

    @Test
    void delete_shouldPreventDeletingUserWithOrders() {
        User user = new User();
        user.setId(2L);
        user.setRole(UserRole.READER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(orderRepository.existsByUserId(2L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.delete(2L, 99L));
    }

    @Test
    void delete_shouldPreventDeletingUserWithPaymentReviews() {
        User user = new User();
        user.setId(3L);
        user.setRole(UserRole.READER);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(orderRepository.existsByUserId(3L)).thenReturn(false);
        when(paymentRepository.existsByReviewedByAdminId(3L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.delete(3L, 99L));
    }

    @Test
    void delete_shouldSucceedWhenGuardrailsPass() {
        User user = new User();
        user.setId(2L);
        user.setRole(UserRole.READER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(orderRepository.existsByUserId(2L)).thenReturn(false);
        when(paymentRepository.existsByReviewedByAdminId(2L)).thenReturn(false);

        userService.delete(2L, 99L);
        verify(userRepository).delete(user);
    }
}
