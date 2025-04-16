package com.albany.mvc.controller;

import com.albany.mvc.dto.ServiceRequestDto;
import com.albany.mvc.service.ServiceRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/service-requests")
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    
    @GetMapping
    public String serviceRequestsPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        
        log.info("Accessing service requests page");
        
        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }
        
        // Set admin's name for the page
        model.addAttribute("userName", "Arthur Morgan");
        
        return "admin/serviceRequests";
    }
    
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<ServiceRequestDto>> getAllServiceRequests(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<ServiceRequestDto> serviceRequests = serviceRequestService.getAllServiceRequests(validToken);
        return ResponseEntity.ok(serviceRequests);
    }
    
    @GetMapping("/api/status/{status}")
    @ResponseBody
    public ResponseEntity<List<ServiceRequestDto>> getServiceRequestsByStatus(
            @PathVariable String status,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<ServiceRequestDto> serviceRequests = serviceRequestService.getServiceRequestsByStatus(status, validToken);
        return ResponseEntity.ok(serviceRequests);
    }
    
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ServiceRequestDto> getServiceRequestById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        ServiceRequestDto serviceRequest = serviceRequestService.getServiceRequestById(id, validToken);
        if (serviceRequest == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(serviceRequest);
    }
    
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<ServiceRequestDto> createServiceRequest(
            @RequestBody ServiceRequestDto requestDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        ServiceRequestDto createdRequest = serviceRequestService.createServiceRequest(requestDto, validToken);
        if (createdRequest == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(createdRequest);
    }
    
    @PutMapping("/api/{id}/assign")
    @ResponseBody
    public ResponseEntity<ServiceRequestDto> assignServiceAdvisor(
            @PathVariable Integer id,
            @RequestParam Integer advisorId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        ServiceRequestDto updatedRequest = serviceRequestService.assignServiceAdvisor(id, advisorId, validToken);
        if (updatedRequest == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(updatedRequest);
    }
    
    @PutMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<ServiceRequestDto> updateServiceRequestStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        ServiceRequestDto updatedRequest = serviceRequestService.updateServiceRequestStatus(id, status, validToken);
        if (updatedRequest == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(updatedRequest);
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
}