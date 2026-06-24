package com.bookwise.user.service;

import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.user.dto.CreateUserForm;
import com.bookwise.user.dto.EditUserForm;
import com.bookwise.user.entity.User;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, OrderRepository orderRepository,
                       PaymentRepository paymentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public User create(CreateUserForm form) {
        String email = form.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(parseRole(form.getRole()));
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, EditUserForm form) {
        User user = getById(id);
        String email = form.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        UserRole newRole = parseRole(form.getRole());

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                throw new IllegalArgumentException("Password confirmation does not match");
            }
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new IllegalArgumentException("You cannot delete yourself");
        }

        User user = getById(id);

        if (user.getRole() == UserRole.ADMIN && countAdmins() <= 1) {
            throw new IllegalArgumentException("Cannot delete the last admin");
        }

        if (orderRepository.existsByUserId(id)) {
            throw new IllegalArgumentException("User cannot be deleted because they have existing orders");
        }

        if (paymentRepository.existsByReviewedByAdminId(id)) {
            throw new IllegalArgumentException("User cannot be deleted because they have reviewed payments");
        }

        userRepository.delete(user);
    }

    public long countAdmins() {
        return userRepository.countByRole(UserRole.ADMIN);
    }

    public long countReaders() {
        return userRepository.countByRole(UserRole.READER);
    }

    public long countUsers() {
        return userRepository.count();
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}
