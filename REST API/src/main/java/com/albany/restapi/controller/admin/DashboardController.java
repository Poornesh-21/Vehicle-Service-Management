package com.albany.restapi.controller.admin;

import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.repository.InvoiceRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/api/data")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Get all service requests
        List<ServiceRequest> allRequests = serviceRequestRepository.findAll();
        
        // Filter requests by status
        List<ServiceRequest> dueRequests = allRequests.stream()
                .filter(req -> req.getStatus() == ServiceRequest.Status.Received)
                .collect(Collectors.toList());
                
        List<ServiceRequest> inProgressRequests = allRequests.stream()
                .filter(req -> req.getStatus() == ServiceRequest.Status.Diagnosis || 
                               req.getStatus() == ServiceRequest.Status.Repair)
                .collect(Collectors.toList());
                
        List<ServiceRequest> completedRequests = allRequests.stream()
                .filter(req -> req.getStatus() == ServiceRequest.Status.Completed)
                .collect(Collectors.toList());
        
        // Calculate counts
        dashboardData.put("vehiclesDue", dueRequests.size());
        dashboardData.put("vehiclesInProgress", inProgressRequests.size());
        dashboardData.put("vehiclesCompleted", completedRequests.size());
        
        // Calculate total revenue from completed services with invoices
        BigDecimal totalRevenue = calculateTotalRevenue(completedRequests);
        dashboardData.put("totalRevenue", totalRevenue);
        
        // Convert requests to simplified DTOs for frontend
        dashboardData.put("vehiclesDueList", convertToDueList(dueRequests));
        dashboardData.put("vehiclesInServiceList", convertToInServiceList(inProgressRequests));
        dashboardData.put("completedServicesList", convertToCompletedList(completedRequests));
        
        return ResponseEntity.ok(dashboardData);
    }
    
    @PutMapping("/api/assign/{requestId}")
    public ResponseEntity<?> assignAdvisor(
            @PathVariable Integer requestId,
            @RequestParam Integer advisorId) {
        
        // Simplified for example - in a real app, you'd implement this fully
        return ResponseEntity.ok(Map.of("success", true, "message", "Advisor assigned successfully"));
    }
    
    private BigDecimal calculateTotalRevenue(List<ServiceRequest> completedRequests) {
        BigDecimal total = BigDecimal.ZERO;
        
        // In a real application, you would sum the actual invoice amounts
        // This is a simplified example
        for (ServiceRequest request : completedRequests) {
            invoiceRepository.findByRequestId(request.getRequestId())
                .ifPresent(invoice -> {
                    // Use actual invoice amount if available
                });
            
            // For now, just add a dummy amount for each completed request
            total = total.add(BigDecimal.valueOf(12000 + Math.random() * 8000));
        }
        
        return total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    private List<Map<String, Object>> convertToDueList(List<ServiceRequest> requests) {
        return requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            map.put("vehicleName", req.getVehicleBrand() + " " + req.getVehicleModel());
            map.put("vehicleBrand", req.getVehicleBrand());
            map.put("vehicleModel", req.getVehicleModel());
            map.put("registrationNumber", req.getVehicleRegistration());
            map.put("category", req.getVehicleType());
            map.put("status", req.getStatus().name());
            map.put("dueDate", LocalDate.now().plusDays(3)); // Example due date
            map.put("customerName", "Customer " + req.getRequestId()); // Placeholder
            map.put("customerEmail", "customer" + req.getRequestId() + "@example.com"); // Placeholder
            map.put("membershipStatus", "Standard"); // Placeholder
            return map;
        }).collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> convertToInServiceList(List<ServiceRequest> requests) {
        return requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            map.put("vehicleName", req.getVehicleBrand() + " " + req.getVehicleModel());
            map.put("vehicleBrand", req.getVehicleBrand());
            map.put("vehicleModel", req.getVehicleModel());
            map.put("registrationNumber", req.getVehicleRegistration());
            map.put("category", req.getVehicleType());
            map.put("status", req.getStatus().name());
            map.put("startDate", req.getCreatedAt());
            map.put("estimatedCompletionDate", LocalDate.now().plusDays(5)); // Example completion date
            map.put("serviceAdvisorName", "Advisor Name"); // Placeholder
            map.put("serviceAdvisorId", 1); // Placeholder
            return map;
        }).collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> convertToCompletedList(List<ServiceRequest> requests) {
        return requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceId", req.getRequestId());
            map.put("requestId", req.getRequestId());
            map.put("vehicleName", req.getVehicleBrand() + " " + req.getVehicleModel());
            map.put("vehicleBrand", req.getVehicleBrand());
            map.put("vehicleModel", req.getVehicleModel());
            map.put("registrationNumber", req.getVehicleRegistration());
            map.put("category", req.getVehicleType());
            map.put("completedDate", req.getUpdatedAt());
            map.put("customerName", "Customer " + req.getRequestId()); // Placeholder
            map.put("serviceAdvisorName", "Advisor Name"); // Placeholder
            map.put("totalCost", BigDecimal.valueOf(10000 + Math.random() * 5000).setScale(2, BigDecimal.ROUND_HALF_UP));
            map.put("hasInvoice", Math.random() > 0.5); // Random for example
            return map;
        }).collect(Collectors.toList());
    }
}