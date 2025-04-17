package com.albany.mvc.controller;

import com.albany.mvc.dto.ServiceRequestDto;
import com.albany.mvc.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard/api")
@RequiredArgsConstructor
@Slf4j
public class DashboardRestController {

    private final DashboardService dashboardService;

    @PutMapping("/assign/{requestId}")
    public ResponseEntity<?> assignServiceAdvisor(
            @PathVariable Integer requestId,
            @RequestParam Integer advisorId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        ServiceRequestDto updatedRequest = dashboardService.assignServiceAdvisor(requestId, advisorId, validToken);

        if (updatedRequest == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to assign service advisor");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        return ResponseEntity.ok(updatedRequest);
    }

    @PutMapping("/status/{requestId}")
    public ResponseEntity<?> updateServiceRequestStatus(
            @PathVariable Integer requestId,
            @RequestParam String status,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        ServiceRequestDto updatedRequest = dashboardService.updateServiceRequestStatus(requestId, status, validToken);

        if (updatedRequest == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update service request status");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Gets a valid token from various sources with Auth header
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