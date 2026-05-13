package com.bookwise.auth.service;

import com.bookwise.auth.dto.LoginForm;
import com.bookwise.auth.dto.RegisterForm;
import com.bookwise.security.AppUserPrincipal;
import com.bookwise.security.JwtService;
import com.bookwise.user.entity.User;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public User register(RegisterForm form) {
        String normalizedEmail = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(UserRole.READER);
        return userRepository.save(user);
    }

    public AuthenticatedLogin authenticate(LoginForm form) {
        String email = normalizeEmail(form.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(form.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new AuthenticatedLogin(AppUserPrincipal.from(user), token);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public record AuthenticatedLogin(AppUserPrincipal principal, String token) {
    }
}
