package com.bookwise.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class LoginRedirectEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        String requestUri = request.getRequestURI();
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            requestUri = requestUri + "?" + request.getQueryString();
        }
        String next = URLEncoder.encode(requestUri, StandardCharsets.UTF_8);
        response.sendRedirect("/auth/login?next=" + next);
    }
}
