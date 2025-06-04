package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.User;
import com.albany.restapi.security.JwtUtil;
import com.albany.restapi.service.customer.CustomerAuthService;
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
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthRestController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthRestController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomerAuthService customerAuthService;
    
    @PostMapping("/login/send-otp")
    public ResponseEntity<?> sendLoginOtp(@RequestBody OtpRequestDTO request) {
        try {
            logger.info("OTP request for login: {}", request.getEmail());
            CustomerAuthResponse response = customerAuthService.sendLoginOtp(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send OTP for login: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                CustomerAuthResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> verifyLoginOtp(@RequestBody OtpVerificationDTO request) {
        try {
            logger.info("OTP verification for login: {}", request.getEmail());
            CustomerAuthResponse response = customerAuthService.verifyLoginOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to verify OTP for login: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                CustomerAuthResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/register/send-otp")
    public ResponseEntity<?> sendRegistrationOtp(@RequestBody CustomerRegistrationDTO request) {
        try {
            logger.info("OTP request for registration: {}", request.getEmail());
            CustomerAuthResponse response = customerAuthService.sendRegistrationOtp(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send OTP for registration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                CustomerAuthResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody CustomerRegistrationDTO request, 
                                                  @RequestParam String otp) {
        try {
            logger.info("OTP verification for registration: {}", request.getEmail());
            CustomerAuthResponse response = customerAuthService.verifyRegistrationOtp(request, otp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to verify OTP for registration: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                CustomerAuthResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
    
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            User user = customerAuthService.validateCustomerToken(username, token);
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "user", Map.of(
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole().name(),
                    "membershipType", user.getMembershipType().name()
                )
            ));
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Invalid or expired token"));
        }
    }
}