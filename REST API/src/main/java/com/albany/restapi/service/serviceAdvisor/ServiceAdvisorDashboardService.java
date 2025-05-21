package com.albany.restapi.service.serviceAdvisor;

import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.ServiceAdvisorProfile;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.MaterialUsageRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import com.albany.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAdvisorDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorDashboardService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final UserRepository userRepository;
    private final ServiceAdvisorAuthService serviceAdvisorAuthService;
    
    /**
     * Get all service requests assigned to the service advisor
     */
    public List<ServiceRequestDTO> getAllServiceRequests(String username) {
        ServiceAdvisorProfile advisor = serviceAdvisorAuthService.getServiceAdvisorProfile(username);
        
        return serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get service requests by status
     */
    public List<ServiceRequestDTO> getServiceRequestsByStatus(String username, ServiceRequest.Status status) {
        ServiceAdvisorProfile advisor = serviceAdvisorAuthService.getServiceAdvisorProfile(username);
        
        return serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId()).stream()
                .filter(request -> request.getStatus() == status)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get service request details by ID
     */
    public ServiceRequestDTO getServiceRequestDetails(String username, Integer requestId) {
        ServiceAdvisorProfile advisor = serviceAdvisorAuthService.getServiceAdvisorProfile(username);
        
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Service request not found with ID: " + requestId));
        
        // Verify the request is assigned to this advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new IllegalArgumentException("Service request is not assigned to this advisor");
        }
        
        return mapToDTO(request);
    }
    
    /**
     * Update service request status
     */
    @Transactional
    public ServiceRequestDTO updateServiceRequestStatus(String username, Integer requestId, ServiceRequest.Status status) {
        ServiceAdvisorProfile advisor = serviceAdvisorAuthService.getServiceAdvisorProfile(username);
        
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Service request not found with ID: " + requestId));
        
        // Verify the request is assigned to this advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new IllegalArgumentException("Service request is not assigned to this advisor");
        }
        
        // Update the status
        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now());
        
        ServiceRequest updatedRequest = serviceRequestRepository.save(request);
        return mapToDTO(updatedRequest);
    }
    
    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats(String username) {
        ServiceAdvisorProfile advisor = serviceAdvisorAuthService.getServiceAdvisorProfile(username);
        
        List<ServiceRequest> allRequests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId());
        
        // Count requests by status
        long receivedCount = allRequests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Received).count();
        long diagnosisCount = allRequests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Diagnosis).count();
        long repairCount = allRequests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Repair).count();
        long completedCount = allRequests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Completed).count();
        
        // Get total active requests (not completed)
        long activeCount = receivedCount + diagnosisCount + repairCount;
        
        // Create stats map
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", allRequests.size());
        stats.put("activeRequests", activeCount);
        stats.put("completedRequests", completedCount);
        
        // Status breakdown
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("received", receivedCount);
        statusCounts.put("diagnosis", diagnosisCount);
        statusCounts.put("repair", repairCount);
        statusCounts.put("completed", completedCount);
        stats.put("statusCounts", statusCounts);
        
        // Recent activity (last 5 updated requests)
        List<ServiceRequestDTO> recentActivity = allRequests.stream()
                .sorted((r1, r2) -> r2.getUpdatedAt().compareTo(r1.getUpdatedAt()))
                .limit(5)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        stats.put("recentActivity", recentActivity);
        
        return stats;
    }
    
    /**
     * Map ServiceRequest entity to DTO
     */
    private ServiceRequestDTO mapToDTO(ServiceRequest request) {
        ServiceRequestDTO dto = ServiceRequestDTO.builder()
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .vehicleId(request.getVehicle() != null ? request.getVehicle().getVehicleId() : null)
                .vehicleType(request.getVehicleType())
                .vehicleBrand(request.getVehicleBrand())
                .vehicleModel(request.getVehicleModel())
                .vehicleRegistration(request.getVehicleRegistration())
                .vehicleYear(request.getVehicleYear())
                .serviceType(request.getServiceType())
                .serviceDescription(request.getServiceDescription())
                .additionalDescription(request.getAdditionalDescription())
                .deliveryDate(request.getDeliveryDate())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();

        // Format dates for UI display
        if (request.getDeliveryDate() != null) {
            dto.setFormattedDeliveryDate(request.getDeliveryDate().format(DATE_FORMATTER));
        }

        if (request.getCreatedAt() != null) {
            dto.setFormattedCreatedDate(request.getCreatedAt().format(DATE_FORMATTER));
        }

        // Add service advisor info if assigned
        if (request.getServiceAdvisor() != null) {
            dto.setServiceAdvisorId(request.getServiceAdvisor().getAdvisorId());

            // Get service advisor name
            User advisorUser = request.getServiceAdvisor().getUser();
            if (advisorUser != null) {
                dto.setServiceAdvisorName(
                        (advisorUser.getFirstName() != null ? advisorUser.getFirstName() : "") + " " +
                                (advisorUser.getLastName() != null ? advisorUser.getLastName() : "").trim()
                );
            }
        }

        // Get customer info if we have a userId
        try {
            if (request.getUserId() != null) {
                User user = userRepository.findById(request.getUserId().intValue()).orElse(null);
                if (user != null) {
                    dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                    dto.setCustomerEmail(user.getEmail());
                    dto.setCustomerPhone(user.getPhoneNumber());
                    dto.setMembershipStatus(user.getMembershipType().toString());
                }
            }
        } catch (Exception e) {
            // Silently ignore exceptions when getting customer info
            logger.warn("Error retrieving customer info for user ID: {}", request.getUserId(), e);
        }

        // Get materials used for this service request if available
        try {
            List<MaterialUsageDTO> materials = materialUsageRepository
                    .findByServiceRequest_RequestIdOrderByUsedAtDesc(request.getRequestId())
                    .stream()
                    .map(material -> {
                        String itemName = material.getInventoryItem() != null ?
                                material.getInventoryItem().getName() : "Unknown Item";

                        BigDecimal unitPrice = material.getInventoryItem() != null ?
                                material.getInventoryItem().getUnitPrice() : BigDecimal.ZERO;

                        return MaterialUsageDTO.builder()
                                .materialUsageId(material.getMaterialUsageId())
                                .name(itemName)
                                .quantity(material.getQuantity())
                                .unitPrice(unitPrice)
                                .usedAt(material.getUsedAt())
                                .totalCost(unitPrice.multiply(material.getQuantity()))
                                .build();
                    })
                    .collect(Collectors.toList());

            dto.setMaterials(materials);
        } catch (Exception e) {
            // Silently ignore exceptions when getting materials
            logger.warn("Error retrieving materials for request ID: {}", request.getRequestId(), e);
        }

        return dto;
    }
}