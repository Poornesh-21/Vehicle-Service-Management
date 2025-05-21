package com.albany.restapi.controller.serviceAdvisor;

import com.albany.restapi.dto.AuthRequest;
import com.albany.restapi.dto.AuthResponse;
import com.albany.restapi.dto.ChangePasswordRequest;
import com.albany.restapi.model.User;
import com.albany.restapi.security.JwtUtil;
import com.albany.restapi.service.serviceAdvisor.ServiceAdvisorAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/serviceAdvisor/api")
@RequiredArgsConstructor
public class ServiceAdvisorAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorAuthController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ServiceAdvisorAuthService serviceAdvisorAuthService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            logger.info("Login attempt for service advisor: {}", authRequest.getEmail());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
            
            User user = (User) authentication.getPrincipal();

            if (user.getRole() != User.Role.serviceAdvisor) {
                logger.warn("Access denied for user {}: Not a service advisor", authRequest.getEmail());
                return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to access the Service Advisor portal"));
            }
            
            String token = jwtUtil.generateToken(user);
            
            logger.info("Login successful for service advisor: {}", authRequest.getEmail());
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .role(user.getRole().name())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .build());
                    
        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for service advisor login: {}", authRequest.getEmail());
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email/password combination"));
        } catch (UsernameNotFoundException e) {
            logger.warn("User not found for service advisor login: {}", authRequest.getEmail());
            return ResponseEntity.status(401).body(Map.of("message", "User not found"));
        } catch (Exception e) {
            logger.error("Authentication failed for service advisor: {}", authRequest.getEmail(), e);
            return ResponseEntity.status(500).body(Map.of("message", "Authentication failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            User user = serviceAdvisorAuthService.validateServiceAdvisorToken(username, token);
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "user", Map.of(
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole().name()
                )
            ));
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Invalid or expired token"));
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            @RequestParam(required = false) String token) {
        
        try {
            String username = null;
            
            // If token is provided, validate and extract username
            if (token != null && !token.isEmpty()) {
                username = jwtUtil.extractUsername(token);
                if (username == null) {
                    return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
                }
            }
            
            // Change password using service
            AuthResponse response = serviceAdvisorAuthService.changePassword(
                username,
                request.getCurrentPassword(), 
                request.getNewPassword(), 
                request.getConfirmPassword(),
                request.isTemporaryPassword()
            );
            
            logger.info("Password changed successfully for user: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Password change validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Password change failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to change password: " + e.getMessage()));
        }
    }
}