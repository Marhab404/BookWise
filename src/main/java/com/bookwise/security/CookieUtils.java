package com.bookwise.security;

import com.bookwise.config.JwtProperties;
import com.bookwise.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class CookieUtils {

    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;

    public CookieUtils(JwtProperties jwtProperties, SecurityProperties securityProperties) {
        this.jwtProperties = jwtProperties;
        this.securityProperties = securityProperties;
    }

    public void writeJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.cookieName(), token)
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(jwtProperties.expirationMinutes()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.cookieName(), "")
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> findJwtCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> jwtProperties.cookieName().equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst();
    }
}
