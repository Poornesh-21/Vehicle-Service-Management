package com.albany.restapi.service.admin;

import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling dashboard operations
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    
    /**
     * Get all dashboard data
     * @return Map containing all dashboard statistics and lists
     */
    public Map<String, Object> getDashboardData() {
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
        
        // Convert requests to DTOs for frontend
        dashboardData.put("vehiclesDueList", convertToDueList(dueRequests));
        dashboardData.put("vehiclesInServiceList", convertToInServiceList(inProgressRequests));
        dashboardData.put("completedServicesList", convertToCompletedList(completedRequests));
        
        return dashboardData;
    }
    
    /**
     * Calculate total revenue from invoices
     */
    private BigDecimal calculateTotalRevenue(List<ServiceRequest> completedRequests) {
        return completedRequests.stream()
            .map(request -> invoiceRepository.findByRequestId(request.getRequestId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(invoice -> invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Convert to due list with real data
     */
    private List<Map<String, Object>> convertToDueList(List<ServiceRequest> requests) {
        return requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            
            // Vehicle info
            map.put("vehicleName", getVehicleName(req));
            map.put("vehicleBrand", req.getVehicleBrand());
            map.put("vehicleModel", req.getVehicleModel());
            map.put("registrationNumber", req.getVehicleRegistration());
            map.put("category", req.getVehicleType());
            map.put("status", req.getStatus().name());
            
            // Dates
            map.put("dueDate", req.getDeliveryDate() != null ? req.getDeliveryDate() : LocalDate.now().plusDays(3));
            
            // Customer info
            Map<String, Object> customerInfo = getUserInfo(req.getUserId());
            map.put("customerName", customerInfo.get("name"));
            map.put("customerEmail", customerInfo.get("email"));
            map.put("membershipStatus", customerInfo.get("membershipStatus"));
            
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * Convert to in-service list with real data
     */
    private List<Map<String, Object>> convertToInServiceList(List<ServiceRequest> requests) {
        return requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            
            // Vehicle info
            map.put("vehicleName", getVehicleName(req));
            map.put("vehicleBrand", req.getVehicleBrand());
            map.put("vehicleModel", req.getVehicleModel());
            map.put("registrationNumber", req.getVehicleRegistration());
            map.put("category", req.getVehicleType());
            map.put("status", req.getStatus().name());
            
            // Dates
            map.put("startDate", req.getCreatedAt());
            map.put("estimatedCompletionDate", req.getDeliveryDate() != null ? req.getDeliveryDate() : LocalDate.now().plusDays(5));
            
            // Service Advisor info
            if (req.getServiceAdvisor() != null) {
                map.put("serviceAdvisorId", req.getServiceAdvisor().getAdvisorId());
                
                if (req.getServiceAdvisor().getUser() != null) {
                    User advisorUser = req.getServiceAdvisor().getUser();
                    map.put("serviceAdvisorName", advisorUser.getFirstName() + " " + advisorUser.getLastName());
                } else {
                    map.put("serviceAdvisorName", "Not Available");
                }
            } else {
                map.put("serviceAdvisorId", null);
                map.put("serviceAdvisorName", "Not Assigned");
            }
            
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * Convert to completed list with real data
     */
    private List<Map<String, Object>> convertToCompletedList(List<ServiceRequest> requests) {
        return requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceId", req.getRequestId());
            map.put("requestId", req.getRequestId());
            
            // Vehicle info
            map.put("vehicleName", getVehicleName(req));
            map.put("vehicleBrand", req.getVehicleBrand());
            map.put("vehicleModel", req.getVehicleModel());
            map.put("registrationNumber", req.getVehicleRegistration());
            map.put("category", req.getVehicleType());
            
            // Dates
            map.put("completedDate", req.getUpdatedAt());
            
            // Customer info
            Map<String, Object> customerInfo = getUserInfo(req.getUserId());
            map.put("customerName", customerInfo.get("name"));
            
            // Service Advisor info
            if (req.getServiceAdvisor() != null && req.getServiceAdvisor().getUser() != null) {
                User advisorUser = req.getServiceAdvisor().getUser();
                map.put("serviceAdvisorName", advisorUser.getFirstName() + " " + advisorUser.getLastName());
            } else {
                map.put("serviceAdvisorName", "Not Assigned");
            }
            
            // Invoice and payment info
            invoiceRepository.findByRequestId(req.getRequestId()).ifPresent(invoice -> {
                map.put("hasInvoice", true);
                map.put("invoiceId", invoice.getInvoiceId());
                map.put("totalCost", invoice.getTotalAmount());
            });
            
            if (!map.containsKey("hasInvoice")) {
                map.put("hasInvoice", false);
                map.put("totalCost", BigDecimal.ZERO);
            }
            
            // Payment status
            paymentRepository.findByRequestId(req.getRequestId()).ifPresent(payment -> {
                map.put("isPaid", payment.getStatus() == Payment.Status.Completed);
                map.put("paymentMethod", payment.getPaymentMethod());
                map.put("paymentDate", payment.getPaymentTimestamp());
            });
            
            if (!map.containsKey("isPaid")) {
                map.put("isPaid", false);
            }
            
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * Helper method to get vehicle name
     */
    private String getVehicleName(ServiceRequest req) {
        if (req.getVehicleBrand() != null && req.getVehicleModel() != null) {
            return req.getVehicleBrand() + " " + req.getVehicleModel();
        } else if (req.getVehicle() != null) {
            return req.getVehicle().getBrand() + " " + req.getVehicle().getModel();
        }
        return "Unknown Vehicle";
    }
    
    /**
     * Helper method to get user info
     */
    private Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        
        if (userId != null) {
            try {
                User user = userRepository.findById(userId.intValue()).orElse(null);
                if (user != null) {
                    info.put("name", user.getFirstName() + " " + user.getLastName());
                    info.put("email", user.getEmail());
                    info.put("membershipStatus", user.getMembershipType().name());
                    
                    // Check if customer profile has more specific membership status
                    customerRepository.findByUser_UserId(user.getUserId()).ifPresent(profile -> {
                        if (profile.getMembershipStatus() != null) {
                            info.put("membershipStatus", profile.getMembershipStatus());
                        }
                    });
                } else {
                    info.put("name", "Unknown Customer");
                    info.put("email", "");
                    info.put("membershipStatus", "Unknown");
                }
            } catch (Exception e) {
                logger.warn("Error retrieving user info for ID: {}", userId, e);
                info.put("name", "Unknown Customer");
                info.put("email", "");
                info.put("membershipStatus", "Unknown");
            }
        } else {
            info.put("name", "Unknown Customer");
            info.put("email", "");
            info.put("membershipStatus", "Unknown");
        }
        
        return info;
    }
    
    /**
     * Assign service advisor to request
     * @param requestId The service request ID
     * @param advisorId The service advisor ID
     * @return true if successful, false otherwise
     */
    public boolean assignServiceAdvisorToRequest(Integer requestId, Integer advisorId) {
        try {
            ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Service request not found with id: " + requestId));
                
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                .orElseThrow(() -> new IllegalArgumentException("Service advisor not found with id: " + advisorId));
                
            request.setServiceAdvisor(advisor);
            request.setUpdatedAt(LocalDateTime.now());
            
            serviceRequestRepository.save(request);
            return true;
        } catch (Exception e) {
            logger.error("Error assigning service advisor: {}", e.getMessage(), e);
            return false;
        }
    }
}