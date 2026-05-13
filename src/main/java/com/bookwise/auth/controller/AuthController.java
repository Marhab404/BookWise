package com.bookwise.auth.controller;

import com.bookwise.auth.dto.LoginForm;
import com.bookwise.auth.dto.RegisterForm;
import com.bookwise.auth.service.AuthService;
import com.bookwise.common.SafeRedirects;
import com.bookwise.security.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    public AuthController(AuthService authService, CookieUtils cookieUtils) {
        this.authService = authService;
        this.cookieUtils = cookieUtils;
    }

    @GetMapping("/register")
    public String registerForm(Model model, String next, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerForm")) {
            RegisterForm form = new RegisterForm();
            form.setNext(next);
            model.addAttribute("registerForm", form);
        }
        model.addAttribute("next", next);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid RegisterForm registerForm, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        String next = SafeRedirects.normalize(registerForm.getNext(), "/");
        if (bindingResult.hasErrors()) {
            model.addAttribute("next", next);
            return "auth/register";
        }
        try {
            authService.register(registerForm);
            redirectAttributes.addFlashAttribute("flashMessage", "Registration completed. Please log in.");
            return "redirect:/auth/login?next=" + urlEncode(next);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("registration.failed", ex.getMessage());
            model.addAttribute("next", next);
            return "auth/register";
        }
    }

    @GetMapping("/login")
    public String loginForm(Model model, String next, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        if (!model.containsAttribute("loginForm")) {
            LoginForm form = new LoginForm();
            form.setNext(next);
            model.addAttribute("loginForm", form);
        }
        model.addAttribute("next", next);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid LoginForm loginForm, BindingResult bindingResult, HttpServletResponse response, Model model) {
        String next = SafeRedirects.normalize(loginForm.getNext(), "/");
        if (bindingResult.hasErrors()) {
            model.addAttribute("next", next);
            return "auth/login";
        }
        try {
            AuthService.AuthenticatedLogin result = authService.authenticate(loginForm);
            cookieUtils.writeJwtCookie(response, result.token());
            return "redirect:" + next;
        } catch (Exception ex) {
            bindingResult.reject("login.failed", "Invalid email or password");
            model.addAttribute("next", next);
            return "auth/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        cookieUtils.clearJwtCookie(response);
        return "redirect:/";
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
