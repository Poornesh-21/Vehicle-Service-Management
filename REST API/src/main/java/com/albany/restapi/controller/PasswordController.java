package com.albany.restapi.controller;

import com.albany.restapi.dto.AuthenticationResponse;
import com.albany.restapi.dto.PasswordChangeRequest;
import com.albany.restapi.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final AuthenticationService authService;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Processing password change request for user: {}", userEmail);
            
            AuthenticationResponse response = authService.changePassword(request, userEmail);
            
            log.info("Password successfully changed for user: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Password change failed: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error during password change: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}