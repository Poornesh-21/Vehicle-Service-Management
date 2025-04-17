package com.albany.restapi.controller;

import com.albany.restapi.dto.DashboardDTO;
import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.DashboardService;
import com.albany.restapi.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ServiceRequestService serviceRequestService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<DashboardDTO> getDashboardData() {
        DashboardDTO dashboardData = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboardData);
    }

    @PutMapping("/assign/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> assignServiceAdvisor(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Integer> request) {
        Integer advisorId = request.get("advisorId");
        return ResponseEntity.ok(serviceRequestService.assignServiceAdvisor(requestId, advisorId));
    }

    @PutMapping("/status/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> updateServiceRequestStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> request) {
        String statusStr = request.get("status");
        ServiceRequest.Status status = ServiceRequest.Status.valueOf(statusStr);
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestStatus(requestId, status));
    }
}