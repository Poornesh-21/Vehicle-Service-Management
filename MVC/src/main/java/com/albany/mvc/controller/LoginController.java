package com.albany.mvc.controller;

import com.albany.mvc.dto.AuthRequest;
import com.albany.mvc.dto.AuthResponse;
import com.albany.mvc.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final AuthenticationService authService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model,
            HttpServletRequest request
    ) {
        log.info("Login page accessed, error param: {}, logout param: {}", error, logout);

        if (error != null) {
            String errorMessage;
            switch (error) {
                case "session_expired":
                    errorMessage = "Your session has expired. Please log in again.";
                    break;
                case "unauthorized":
                    errorMessage = "You do not have permission to access this resource.";
                    break;
                case "invalid_credentials":
                    errorMessage = "Invalid email or password. Please check your credentials.";
                    break;
                case "server_error":
                    errorMessage = "Could not connect to authentication server. Please try again later.";
                    break;
                default:
                    errorMessage = "Authentication failed. Please verify your email and password.";
                    break;
            }
            model.addAttribute("error", errorMessage);
            log.warn("Login page showing error: {}", errorMessage);
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
            log.info("User logged out, showing success message");
        }

        return "admin/login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> apiLogin(@RequestBody AuthRequest request) {
        log.info("Processing API login request for email: {}", request.getEmail());

        try {
            // Call API to authenticate user
            AuthResponse authResponse = authService.authenticate(request);

            log.info("Authentication response received: {}", authResponse);

            // Check if user is an admin
            String role = authResponse.getRole();
            if (role != null) {
                role = role.toUpperCase().replace("\"", "").trim();
            }

            log.info("User role: {}", role);

            if (!"ADMIN".equals(role)) {
                log.warn("Non-admin user attempted to access admin portal: {}, role: {}",
                        request.getEmail(), role);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You do not have permission to access the admin portal"));
            }

            log.info("Admin login successful for: {}", request.getEmail());

            // Return the auth response (token and user details) to the client
            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            log.error("Bad credentials: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password. Please try again."));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("API authentication rejected: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid email or password. Please try again."));
            } else {
                log.error("API error: {} - {}", e.getStatusCode(), e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Authentication service error: " + e.getStatusCode()));
            }
        } catch (RestClientException e) {
            log.error("API connection error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Unable to connect to authentication server. Please try again later."));
        } catch (Exception e) {
            log.error("Unexpected error during authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/logout")
    public String logout() {
        log.info("User logged out successfully");
        return "redirect:/admin/login?logout=true";
    }
}