package com.albany.mvc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // List of paths that should be accessible without authentication
    private final List<String> publicPaths = Arrays.asList(
            "/admin/login",
            "/admin/api/login",
            "/serviceAdvisor/login",
            "/serviceAdvisor/api/login",
            "/css/",
            "/js/",
            "/images/",
            "/favicon.ico",
            "/error",
            "/test-auth"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Get the request URI
        String requestURI = request.getRequestURI();
        boolean isPublicPath = publicPaths.stream().anyMatch(requestURI::startsWith);
        boolean isAjaxRequest = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        log.debug("Filtering request: {} (Public: {}, Ajax: {})", requestURI, isPublicPath, isAjaxRequest);

        // For public paths, just continue
        if (isPublicPath) {
            log.debug("Public path detected, proceeding without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        // Check for token in request parameter
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Found token parameter");

            if (jwtUtil.validateToken(tokenParam)) {
                Authentication auth = jwtUtil.getAuthentication(tokenParam);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Store token in session for future requests
                HttpSession session = request.getSession();
                session.setAttribute("jwt-token", tokenParam);

                // Store user info in session
                String email = jwtUtil.extractUsername(tokenParam);
                String firstName = jwtUtil.extractClaim(tokenParam, claims -> claims.get("firstName", String.class));
                String lastName = jwtUtil.extractClaim(tokenParam, claims -> claims.get("lastName", String.class));
                if (firstName != null) session.setAttribute("firstName", firstName);
                if (lastName != null) session.setAttribute("lastName", lastName);
                if (email != null) session.setAttribute("email", email);

                log.debug("Valid token parameter, set authentication for user: {}", auth.getName());
                filterChain.doFilter(request, response);
                return;
            } else {
                log.warn("Invalid token parameter provided");
            }
        }

        // Check for token in session
        HttpSession session = request.getSession(false);
        String sessionToken = session != null ? (String) session.getAttribute("jwt-token") : null;

        if (sessionToken != null && jwtUtil.validateToken(sessionToken)) {
            Authentication auth = jwtUtil.getAuthentication(sessionToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Valid session token, set authentication for user: {}", auth.getName());
            filterChain.doFilter(request, response);
            return;
        }

        // Check for token in Authorization header
        String authHeader = request.getHeader("Authorization");
        String headerToken = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headerToken = authHeader.substring(7);
            log.debug("Found token in Authorization header");

            if (jwtUtil.validateToken(headerToken)) {
                Authentication auth = jwtUtil.getAuthentication(headerToken);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Store in session for future requests
                if (session != null) {
                    session.setAttribute("jwt-token", headerToken);

                    // Store user info in session
                    String email = jwtUtil.extractUsername(headerToken);
                    String firstName = jwtUtil.extractClaim(headerToken, claims -> claims.get("firstName", String.class));
                    String lastName = jwtUtil.extractClaim(headerToken, claims -> claims.get("lastName", String.class));
                    if (firstName != null) session.setAttribute("firstName", firstName);
                    if (lastName != null) session.setAttribute("lastName", lastName);
                    if (email != null) session.setAttribute("email", email);
                }

                log.debug("Valid Authorization header token, set authentication for user: {}", auth.getName());
                filterChain.doFilter(request, response);
                return;
            }
        }

        // If no valid token was found, handle accordingly
        log.debug("No valid token found, clearing security context");
        SecurityContextHolder.clearContext();

        // Determine which login page to redirect to based on the URL
        String loginRedirectPath = requestURI.startsWith("/serviceAdvisor") ?
                "/serviceAdvisor/login?error=session_expired" :
                "/admin/login?error=session_expired";

        if (isAjaxRequest) {
            log.debug("AJAX request without valid token, returning 401");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Session expired\",\"status\":401}");
        } else {
            log.debug("Redirecting to login page due to missing/invalid token");
            response.sendRedirect(loginRedirectPath);
        }
    }
}