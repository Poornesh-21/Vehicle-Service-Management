package com.albany.mvc.controller;

import com.albany.mvc.dto.AuthResponse;
import com.albany.mvc.dto.PasswordChangeDto;
import com.albany.mvc.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/serviceAdvisor/api/change-password")
    public ResponseEntity<?> changeServiceAdvisorPassword(
            @RequestBody PasswordChangeDto passwordChangeDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Processing password change request for service advisor");
        
        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authorization required"));
        }

        try {
            // Call service to change password
            AuthResponse response = passwordService.changePassword(passwordChangeDto, validToken);
            
            // Update session with new token
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", response.getToken());
            
            // Return success response with new token
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Password changed successfully");
            successResponse.put("token", response.getToken());
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/admin/api/change-password")
    public ResponseEntity<?> changeAdminPassword(
            @RequestBody PasswordChangeDto passwordChangeDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Processing password change request for admin");
        
        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authorization required"));
        }

        try {
            // Call service to change password
            AuthResponse response = passwordService.changePassword(passwordChangeDto, validToken);
            
            // Update session with new token
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", response.getToken());
            
            // Return success response with new token
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Password changed successfully");
            successResponse.put("token", response.getToken());
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Helper method to get a valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                return sessionToken;
            }
        }

        return null;
    }
}