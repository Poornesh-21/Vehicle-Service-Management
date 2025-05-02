package com.albany.mvc.controller;

import com.albany.mvc.service.MechanicAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/serviceAdvisor")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardController {

    private final MechanicAssignmentService assignmentService;

    /**
     * Dashboard page rendering
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        log.info("Accessing service advisor dashboard");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/serviceAdvisor/login?error=session_expired";
        }

        // Add user information to the model
        HttpSession session = request.getSession(false);
        if (session != null) {
            String firstName = (String) session.getAttribute("firstName");
            String lastName = (String) session.getAttribute("lastName");
            if (firstName != null && lastName != null) {
                model.addAttribute("userName", firstName + " " + lastName);
            } else {
                model.addAttribute("userName", "Service Advisor");
            }
        } else {
            model.addAttribute("userName", "Service Advisor");
        }

        return "serviceAdvisor/dashboard";
    }

    /**
     * REST endpoint to get new service requests
     */
    @GetMapping("/api/new-assignments")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getNewAssignments(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<Map<String, Object>> newRequests = assignmentService.getNewServiceRequests(validToken);
            return ResponseEntity.ok(newRequests);
        } catch (Exception e) {
            log.error("Error fetching new service requests: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * REST endpoint to get all mechanics for assignment
     */
    @GetMapping("/api/mechanics-for-assignment")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMechanicsForAssignment(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<Map<String, Object>> mechanics = assignmentService.getAllMechanics(validToken);
            return ResponseEntity.ok(mechanics);
        } catch (Exception e) {
            log.error("Error fetching mechanics: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * REST endpoint to assign mechanics to a service
     */
    @PostMapping("/api/assign-service/{requestId}")
    @ResponseBody
    public ResponseEntity<?> assignService(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> assignmentData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Map<String, Object> result = assignmentService.assignMechanicsToService(requestId, assignmentData, validToken);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error assigning service: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Gets a valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            // Store token in session
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", tokenParam);
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Using token from Authorization header");
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                log.debug("Using token from session");
                return sessionToken;
            }
        }

        log.warn("No valid token found from any source");
        return null;
    }

    /**
     * Gets a valid token from various sources (without Auth header)
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }
}