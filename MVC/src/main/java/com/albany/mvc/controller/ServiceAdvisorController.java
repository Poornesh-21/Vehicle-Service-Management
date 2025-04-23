package com.albany.mvc.controller;

import com.albany.mvc.dto.ServiceAdvisorDto;
import com.albany.mvc.service.ServiceAdvisorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/service-advisors")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorController {

    private final ServiceAdvisorService serviceAdvisorService;

    // Page rendering method
    @GetMapping
    public String serviceAdvisorsPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        log.info("Accessing service advisors page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }

        // Set some model attributes for the view
        model.addAttribute("userName", "Arthur Morgan");

        return "admin/serviceAdvisor";
    }

    // REST endpoint to get all advisors
    @GetMapping("/api/advisors")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDto>> getServiceAdvisorsJson(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<ServiceAdvisorDto> serviceAdvisors = serviceAdvisorService.getAllServiceAdvisors(validToken);

            if (serviceAdvisors == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            log.info("Successfully fetched {} service advisors", serviceAdvisors.size());
            return ResponseEntity.ok(serviceAdvisors);
        } catch (Exception e) {
            log.error("Error fetching service advisors: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // Additional endpoint for all advisors (alternative endpoint)
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDto>> getAllServiceAdvisors(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Getting all service advisors via alternative endpoint");
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<ServiceAdvisorDto> serviceAdvisors = serviceAdvisorService.getAllServiceAdvisors(validToken);
            log.info("Service advisors loaded: {}", serviceAdvisors.size());
            return ResponseEntity.ok(serviceAdvisors);
        } catch (Exception e) {
            log.error("Error fetching service advisors: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // Another alternative endpoint with "all" parameter
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDto>> getAllServiceAdvisorsAlternate(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Getting all service advisors via 'all' endpoint");
        return getServiceAdvisorsJson(token, authHeader, request);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ServiceAdvisorDto> getServiceAdvisor(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceAdvisorDto advisor = serviceAdvisorService.getServiceAdvisorById(id, validToken);

            if (advisor == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(advisor);
        } catch (Exception e) {
            log.error("Error getting service advisor: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> createServiceAdvisor(
            @RequestBody ServiceAdvisorDto advisorDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Creating service advisor: {}", advisorDto.getEmail());

            // Ensure the advisor has a password
            if (advisorDto.getPassword() == null || advisorDto.getPassword().isEmpty()) {
                advisorDto.setPassword(generateRandomPassword());
            }

            ServiceAdvisorDto createdAdvisor = serviceAdvisorService.createServiceAdvisor(advisorDto, validToken);

            if (createdAdvisor == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Failed to create service advisor");
                return ResponseEntity.badRequest().body(error);
            }

            // Add the password to the response so the client can display it
            Map<String, Object> response = new HashMap<>();
            response.put("advisor", createdAdvisor);
            response.put("tempPassword", advisorDto.getPassword());

            log.info("Successfully created service advisor with ID: {}", createdAdvisor.getAdvisorId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating service advisor: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ServiceAdvisorDto> updateServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody ServiceAdvisorDto advisorDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceAdvisorDto updatedAdvisor = serviceAdvisorService.updateServiceAdvisor(id, advisorDto, validToken);

            if (updatedAdvisor == null) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(updatedAdvisor);
        } catch (Exception e) {
            log.error("Error updating service advisor: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteServiceAdvisor(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            boolean deleted = serviceAdvisorService.deleteServiceAdvisor(id, validToken);

            if (!deleted) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting service advisor: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Gets a valid token from various sources
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }

    /**
     * Gets a valid token from various sources with Auth header
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
     * Generate a random password for new service advisors
     */
    private String generateRandomPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excluded I and O to avoid confusion
        final String numbers = "123456789"; // Excluded 0 to avoid confusion with O

        StringBuilder password = new StringBuilder("SA2025-");

        // Add 3 random letters
        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * letters.length());
            password.append(letters.charAt(index));
        }

        // Add 3 random numbers
        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * numbers.length());
            password.append(numbers.charAt(index));
        }

        return password.toString();
    }
}