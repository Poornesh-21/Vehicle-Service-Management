package com.albany.mvc.controller;

import com.albany.mvc.dto.MechanicDto;
import com.albany.mvc.service.MechanicService;
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
@RequestMapping("/serviceAdvisor/mechanics")
@RequiredArgsConstructor
@Slf4j
public class MechanicController {

    private final MechanicService mechanicService;

    // Page rendering method
    @GetMapping
    public String mechanicsPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        log.info("Accessing mechanics page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/serviceAdvisor/login?error=session_expired";
        }

        // Set some model attributes for the view
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

        return "serviceAdvisor/mechanics";
    }

    // REST endpoint to get all mechanics
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<MechanicDto>> getAllMechanics(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<MechanicDto> mechanics = mechanicService.getAllMechanics(validToken);
            log.info("Mechanics loaded: {}", mechanics.size());
            return ResponseEntity.ok(mechanics);
        } catch (Exception e) {
            log.error("Error fetching mechanics: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // REST endpoint to get counts of mechanics by specialization
    @GetMapping("/api/counts")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getMechanicCounts(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyMap());
        }

        try {
            Map<String, Long> counts = mechanicService.getMechanicCounts(validToken);
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            log.error("Error fetching mechanic counts: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyMap());
        }
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<MechanicDto> getMechanic(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            MechanicDto mechanic = mechanicService.getMechanicById(id, validToken);

            if (mechanic == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(mechanic);
        } catch (Exception e) {
            log.error("Error getting mechanic: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> createMechanic(
            @RequestBody MechanicDto mechanicDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Creating mechanic: {}", mechanicDto.getEmail());

            // Ensure the mechanic has a password
            if (mechanicDto.getPassword() == null || mechanicDto.getPassword().isEmpty()) {
                mechanicDto.setPassword(generateRandomPassword());
            }

            MechanicDto createdMechanic = mechanicService.createMechanic(mechanicDto, validToken);

            if (createdMechanic == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Failed to create mechanic");
                return ResponseEntity.badRequest().body(error);
            }

            // Add the password to the response so the client can display it
            Map<String, Object> response = new HashMap<>();
            response.put("mechanic", createdMechanic);
            response.put("tempPassword", mechanicDto.getPassword());

            log.info("Successfully created mechanic with ID: {}", createdMechanic.getMechanicId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating mechanic: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<MechanicDto> updateMechanic(
            @PathVariable Integer id,
            @RequestBody MechanicDto mechanicDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            MechanicDto updatedMechanic = mechanicService.updateMechanic(id, mechanicDto, validToken);

            if (updatedMechanic == null) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(updatedMechanic);
        } catch (Exception e) {
            log.error("Error updating mechanic: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteMechanic(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            boolean deleted = mechanicService.deleteMechanic(id, validToken);

            if (!deleted) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting mechanic: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
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
     * Gets a valid token from various sources (without Auth header)
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }

    /**
     * Generate a random password for new mechanics
     */
    private String generateRandomPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excluded I and O to avoid confusion
        final String numbers = "123456789"; // Excluded 0 to avoid confusion with O

        StringBuilder password = new StringBuilder("MECH2025-");

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