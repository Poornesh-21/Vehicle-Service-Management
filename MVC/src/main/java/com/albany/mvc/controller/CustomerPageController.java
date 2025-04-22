package com.albany.mvc.controller;

import com.albany.mvc.security.JwtUtil;
import com.albany.mvc.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerPageController {

    private final JwtUtil jwtUtil;
    private final CustomerService customerService;

    @GetMapping
    public String customersPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        log.info("Accessing customers page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }

        try {
            // Fetch customers from API
            List<Map<String, Object>> customers = customerService.getAllCustomers(validToken);
            model.addAttribute("customers", customers);
            log.info("Loaded {} customers for display", customers.size());
        } catch (Exception e) {
            log.error("Error loading customers: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load customers: " + e.getMessage());
        }

        // Set the admin's name for the page
        model.addAttribute("userName", "Arthur Morgan");

        return "admin/customers";
    }

    /**
     * Gets a valid token from various sources
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        // If token is provided in parameter, validate and use it
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Token parameter provided");

            if (jwtUtil.validateToken(tokenParam)) {
                // Set the authentication in the security context
                Authentication tokenAuth = jwtUtil.getAuthentication(tokenParam);
                SecurityContextHolder.getContext().setAuthentication(tokenAuth);

                // Store token in session for future requests
                HttpSession session = request.getSession();
                session.setAttribute("jwt-token", tokenParam);

                log.info("Token validated and stored in session");
                return tokenParam;
            } else {
                log.warn("Invalid token provided in URL");
                return null;
            }
        }

        // Check session next
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty() && jwtUtil.validateToken(sessionToken)) {
                // Set the authentication in the security context
                Authentication tokenAuth = jwtUtil.getAuthentication(sessionToken);
                SecurityContextHolder.getContext().setAuthentication(tokenAuth);

                log.info("Using valid token from session");
                return sessionToken;
            }
        }

        // Check header last
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String headerToken = authHeader.substring(7);
            if (jwtUtil.validateToken(headerToken)) {
                // Set the authentication in the security context
                Authentication tokenAuth = jwtUtil.getAuthentication(headerToken);
                SecurityContextHolder.getContext().setAuthentication(tokenAuth);

                // Store in session for future requests
                if (session != null) {
                    session.setAttribute("jwt-token", headerToken);
                }

                log.info("Using valid token from Authorization header");
                return headerToken;
            }
        }

        return null;
    }
}