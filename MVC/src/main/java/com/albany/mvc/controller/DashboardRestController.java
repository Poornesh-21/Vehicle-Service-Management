package com.albany.mvc.controller;

import com.albany.mvc.dto.DashboardDTO;
import com.albany.mvc.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dashboard/api")
@RequiredArgsConstructor
@Slf4j
public class DashboardRestController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard data
     */
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<?> getDashboardData(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            DashboardDTO dashboardData = dashboardService.getDashboardData(validToken);
            if (dashboardData == null) {
                return ResponseEntity.status(500).body("Failed to fetch dashboard data");
            }
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error fetching dashboard data: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error fetching dashboard data: " + e.getMessage());
        }
    }

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

        try {
            var updatedRequest = dashboardService.assignServiceAdvisor(requestId, advisorId, validToken);
            if (updatedRequest == null) {
                return ResponseEntity.badRequest().body("Failed to assign service advisor");
            }
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            log.error("Error assigning service advisor: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error assigning service advisor: " + e.getMessage());
        }
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