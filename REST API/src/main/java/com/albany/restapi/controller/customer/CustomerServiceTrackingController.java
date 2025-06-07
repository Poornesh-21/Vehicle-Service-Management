package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.ServiceTrackingDTO;
import com.albany.restapi.dto.VehicleTrackingDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import com.albany.restapi.repository.ServiceTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/service-tracking")
public class CustomerServiceTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceTrackingController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final CustomerRepository customerRepository;

    public CustomerServiceTrackingController(ServiceRequestRepository serviceRequestRepository,
                                           ServiceTrackingRepository serviceTrackingRepository,
                                           CustomerRepository customerRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceTrackingRepository = serviceTrackingRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Get active service requests for the current customer
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveServices(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Find all service requests that are not completed for this user
            List<ServiceRequest> activeRequests = serviceRequestRepository.findAll().stream()
                    .filter(req -> req.getUserId().equals(user.getUserId().longValue()))
                    .filter(req -> req.getStatus() != ServiceRequest.Status.Completed)
                    .collect(Collectors.toList());
            
            List<VehicleTrackingDTO> trackingDTOs = activeRequests.stream()
                    .map(this::convertToTrackingDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(trackingDTOs);
        } catch (Exception e) {
            logger.error("Error fetching active services: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Get service tracking details for a specific request
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getServiceTracking(@PathVariable Integer requestId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Find the service request
            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request not found"
                ));
            }
            
            ServiceRequest request = requestOpt.get();
            
            // Verify that the request belongs to this user
            if (!request.getUserId().equals(user.getUserId().longValue())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request does not belong to this customer"
                ));
            }
            
            // Get tracking records for this request
            List<ServiceTracking> trackingRecords = serviceTrackingRepository
                    .findByServiceRequest_RequestIdOrderByUpdatedAtDesc(requestId);
            
            List<ServiceTrackingDTO> trackingDTOs = trackingRecords.stream()
                    .map(this::convertToTrackingDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", request.getRequestId());
            response.put("status", request.getStatus().name());
            response.put("vehicleBrand", request.getVehicleBrand());
            response.put("vehicleModel", request.getVehicleModel());
            response.put("vehicleRegistration", request.getVehicleRegistration());
            response.put("serviceType", request.getServiceType());
            response.put("createdAt", request.getCreatedAt());
            response.put("updatedAt", request.getUpdatedAt());
            response.put("trackingRecords", trackingDTOs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching service tracking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Convert ServiceRequest to VehicleTrackingDTO
     */
    private VehicleTrackingDTO convertToTrackingDTO(ServiceRequest request) {
        VehicleTrackingDTO dto = VehicleTrackingDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(request.getVehicleBrand() + " " + request.getVehicleModel())
                .vehicleBrand(request.getVehicleBrand())
                .vehicleModel(request.getVehicleModel())
                .registrationNumber(request.getVehicleRegistration())
                .category(request.getVehicleType())
                .serviceType(request.getServiceType())
                .status(request.getStatus().name())
                .additionalDescription(request.getAdditionalDescription())
                .startDate(request.getCreatedAt())
                .estimatedCompletionDate(request.getDeliveryDate())
                .updatedAt(request.getUpdatedAt())
                .build();
        
        // Add service advisor info if available
        if (request.getServiceAdvisor() != null) {
            dto.setServiceAdvisorName(request.getServiceAdvisor().getUser().getFirstName() + " " + 
                                     request.getServiceAdvisor().getUser().getLastName());
            dto.setServiceAdvisorId(request.getServiceAdvisor().getAdvisorId());
        }
        
        return dto;
    }

    /**
     * Convert ServiceTracking to ServiceTrackingDTO
     */
    private ServiceTrackingDTO convertToTrackingDTO(ServiceTracking tracking) {
        return ServiceTrackingDTO.builder()
                .trackingId(tracking.getTrackingId())
                .requestId(tracking.getServiceRequest().getRequestId())
                .status(tracking.getStatus().name())
                .workDescription(tracking.getWorkDescription())
                .laborMinutes(tracking.getLaborMinutes())
                .laborCost(tracking.getLaborCost())
                .totalMaterialCost(tracking.getTotalMaterialCost())
                .updatedAt(tracking.getUpdatedAt())
                .updatedBy(tracking.getServiceAdvisor() != null ? 
                        tracking.getServiceAdvisor().getUser().getFirstName() + " " + 
                        tracking.getServiceAdvisor().getUser().getLastName() : null)
                .build();
    }
}