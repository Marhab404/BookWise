package com.bookwise.security;

import com.bookwise.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, CookieUtils cookieUtils, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.cookieUtils = cookieUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/covers/") ||
               path.startsWith("/images/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cookieUtils.findJwtCookie(request).filter(jwtService::isValid).ifPresent(token -> {
                String email = jwtService.extractEmail(token);
                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                    AppUserPrincipal principal = AppUserPrincipal.from(user);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            });
        }
        filterChain.doFilter(request, response);
    }
}
