package com.albany.restapi.controller.serviceAdvisor;

import com.albany.restapi.controller.admin.ServiceAdvisorController;
import com.albany.restapi.dto.*;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.albany.restapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single controller to handle all service advisor functionality
 * All endpoints consolidated under /serviceAdvisor/api prefix
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ServiceAdvisorDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorController.class);

    // Repositories
    private final ServiceRequestRepository serviceRequestRepository;
    private final InventoryRepository inventoryRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final UserRepository userRepository;

    // Utils
    private final JwtUtil jwtUtil;

    /**
     * INVENTORY ENDPOINTS
     */

    /**
     * Get all inventory items
     */
    @GetMapping("/serviceAdvisor/api/dashboard/inventory-items")
    public ResponseEntity<List<InventoryItemDTO>> getInventoryItems(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("Fetching inventory items for service advisor dashboard");

        try {
            // Get all inventory items
            List<InventoryItem> items = inventoryRepository.findAllByOrderByNameAsc();

            // Map to DTOs
            List<InventoryItemDTO> dtos = items.stream()
                    .map(item -> {
                        // Ensure derived fields are calculated
                        item.calculateDerivedFields();

                        return InventoryItemDTO.builder()
                                .itemId(item.getItemId())
                                .name(item.getName())
                                .category(item.getCategory())
                                .currentStock(item.getCurrentStock())
                                .unitPrice(item.getUnitPrice())
                                .reorderLevel(item.getReorderLevel())
                                .totalValue(item.getTotalValue())
                                .stockStatus(item.getStockStatus())
                                .build();
                    })
                    .collect(Collectors.toList());

            logger.info("Returning {} inventory items", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching inventory items", e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    /**
     * SERVICE REQUEST ENDPOINTS
     */

    /**
     * Get all service requests for the logged-in advisor
     */
    @GetMapping("/serviceAdvisor/api/dashboard/service-requests")
    public ResponseEntity<?> getServiceRequests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String status) {

        try {
            logger.info("Fetching service requests for service advisor");

            // Extract username from token
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Service advisor profile not found"));

            List<ServiceRequest> requests;

            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                try {
                    ServiceRequest.Status statusEnum = ServiceRequest.Status.valueOf(status);
                    requests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId())
                            .stream()
                            .filter(req -> req.getStatus() == statusEnum)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid status. Valid values are: Received, Diagnosis, Repair, Completed"));
                }
            } else {
                requests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId());
            }

            // Map to DTOs
            List<ServiceRequestDTO> dtos = requests.stream()
                    .map(this::mapServiceRequestToDTO)
                    .collect(Collectors.toList());

            logger.info("Returning {} service requests", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching service requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch service requests: " + e.getMessage()));
        }
    }

    /**
     * Get service request details by ID
     */
    @GetMapping("/serviceAdvisor/api/dashboard/service-requests/{id}")
    public ResponseEntity<?> getServiceRequestDetails(
            @PathVariable("id") Integer requestId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            logger.info("Fetching details for service request {}", requestId);

            // Extract username from token
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Service advisor profile not found"));

            // Get service request
            ServiceRequest request = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Service request not found"));

            // Verify the request is assigned to this advisor
            if (request.getServiceAdvisor() == null ||
                    !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Service request is not assigned to this advisor"));
            }

            // Map to DTO
            ServiceRequestDTO dto = mapServiceRequestToDTO(request);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching service request details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch service request details: " + e.getMessage()));
        }
    }

    /**
     * Update service request status
     */
    @PutMapping("/serviceAdvisor/api/dashboard/service-requests/{id}/status")
    @Transactional
    public ResponseEntity<?> updateServiceRequestStatus(
            @PathVariable("id") Integer requestId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            logger.info("Updating status for service request {}", requestId);

            // Extract status from request
            String statusStr = request.get("status");
            String notes = request.get("notes");

            if (statusStr == null || statusStr.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }

            // Parse status
            ServiceRequest.Status status;
            try {
                status = ServiceRequest.Status.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Valid values are: Received, Diagnosis, Repair, Completed"));
            }

            // Extract username from token
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Service advisor profile not found"));

            // Get service request
            ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Service request not found"));

            // Verify the request is assigned to this advisor
            if (serviceRequest.getServiceAdvisor() == null ||
                    !serviceRequest.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Service request is not assigned to this advisor"));
            }

            // Update status
            serviceRequest.setStatus(status);
            serviceRequest.setUpdatedAt(LocalDateTime.now());
            serviceRequestRepository.save(serviceRequest);

            // Add tracking record if repository is available
            try {
                ServiceTracking tracking = ServiceTracking.builder()
                        .serviceRequest(serviceRequest)
                        .serviceAdvisor(advisor)
                        .status(status)
                        .workDescription(notes != null ? notes : "Status updated to " + status.name())
                        .updatedAt(LocalDateTime.now())
                        .build();

                serviceTrackingRepository.save(tracking);
            } catch (Exception e) {
                logger.warn("Could not save tracking record: {}", e.getMessage());
                // Continue execution
            }

            // Return updated service request
            ServiceRequestDTO dto = mapServiceRequestToDTO(serviceRequest);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error updating service request status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update service request status: " + e.getMessage()));
        }
    }

    /**
     * Add inventory items to a service request
     */
    @PostMapping("/serviceAdvisor/api/dashboard/service-requests/{id}/inventory-items")
    @Transactional
    public ResponseEntity<?> addInventoryItems(
            @PathVariable("id") Integer requestId,
            @RequestBody MaterialRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            logger.info("Adding {} materials to service request {}",
                    request.getItems() != null ? request.getItems().size() : 0, requestId);

            // Extract username from token
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Service advisor profile not found"));

            // Get service request
            ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Service request not found"));

            // Verify the request is assigned to this advisor
            if (serviceRequest.getServiceAdvisor() == null ||
                    !serviceRequest.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Service request is not assigned to this advisor"));
            }

            // Clear existing materials if requested
            if (request.isReplaceExisting()) {
                List<MaterialUsage> existingMaterials = materialUsageRepository
                        .findByServiceRequest_RequestIdOrderByUsedAtDesc(requestId);
                materialUsageRepository.deleteAll(existingMaterials);
            }

            // Add new materials
            if (request.getItems() != null) {
                for (MaterialItemDTO item : request.getItems()) {
                    InventoryItem inventoryItem = inventoryRepository.findById(item.getItemId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Inventory item not found with ID: " + item.getItemId()));

                    // Check if there's enough stock
                    if (inventoryItem.getCurrentStock().compareTo(item.getQuantity()) < 0) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Not enough stock for item: " + inventoryItem.getName() +
                                        ". Available: " + inventoryItem.getCurrentStock() +
                                        ", Requested: " + item.getQuantity()));
                    }

                    // Update inventory stock
                    inventoryItem.setCurrentStock(inventoryItem.getCurrentStock().subtract(item.getQuantity()));
                    inventoryRepository.save(inventoryItem);

                    // Create material usage record
                    MaterialUsage materialUsage = MaterialUsage.builder()
                            .inventoryItem(inventoryItem)
                            .serviceRequest(serviceRequest)
                            .quantity(item.getQuantity())
                            .usedAt(LocalDateTime.now())
                            .build();

                    materialUsageRepository.save(materialUsage);
                }
            }

            // Update the service request
            serviceRequest.setUpdatedAt(LocalDateTime.now());
            serviceRequestRepository.save(serviceRequest);

            // Return updated service request
            ServiceRequestDTO dto = mapServiceRequestToDTO(serviceRequest);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error adding inventory items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add inventory items: " + e.getMessage()));
        }
    }

    /**
     * Add labor charges to a service request
     */
    @PostMapping("/serviceAdvisor/api/dashboard/service-requests/{id}/labor-charges")
    @Transactional
    public ResponseEntity<?> addLaborCharges(
            @PathVariable("id") Integer requestId,
            @RequestBody List<LaborChargeDTO> charges,
            @RequestHeader("Authorization") String authHeader) {

        try {
            logger.info("Adding {} labor charges to service request {}",
                    charges != null ? charges.size() : 0, requestId);

            // Extract username from token
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Service advisor profile not found"));

            // Get service request
            ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Service request not found"));

            // Verify the request is assigned to this advisor
            if (serviceRequest.getServiceAdvisor() == null ||
                    !serviceRequest.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Service request is not assigned to this advisor"));
            }

            // Add labor charges
            if (charges != null) {
                for (LaborChargeDTO charge : charges) {
                    // Convert hours to minutes for storage
                    int minutes = charge.getHours().multiply(BigDecimal.valueOf(60)).intValue();

                    // Calculate total cost
                    BigDecimal laborCost = charge.getHours().multiply(charge.getRate());

                    // Create tracking record
                    ServiceTracking tracking = ServiceTracking.builder()
                            .serviceRequest(serviceRequest)
                            .serviceAdvisor(advisor)
                            .laborMinutes(minutes)
                            .laborCost(laborCost)
                            .workDescription(charge.getDescription() != null ?
                                    charge.getDescription() : "Labor Charge")
                            .status(serviceRequest.getStatus())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    serviceTrackingRepository.save(tracking);
                }
            }

            // Update the service request
            serviceRequest.setUpdatedAt(LocalDateTime.now());
            serviceRequestRepository.save(serviceRequest);

            // Return updated service request
            ServiceRequestDTO dto = mapServiceRequestToDTO(serviceRequest);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error adding labor charges", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add labor charges: " + e.getMessage()));
        }
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/serviceAdvisor/api/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Extract username from token
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Service advisor profile not found"));

            // Get all service requests for this advisor
            List<ServiceRequest> requests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId());

            // Count requests by status
            long receivedCount = requests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Received).count();
            long diagnosisCount = requests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Diagnosis).count();
            long repairCount = requests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Repair).count();
            long completedCount = requests.stream().filter(r -> r.getStatus() == ServiceRequest.Status.Completed).count();

            // Calculate active count
            long activeCount = receivedCount + diagnosisCount + repairCount;

            // Create stats map
            Map<String, Object> stats = Map.of(
                    "totalRequests", requests.size(),
                    "activeRequests", activeCount,
                    "completedRequests", completedCount,
                    "statusCounts", Map.of(
                            "received", receivedCount,
                            "diagnosis", diagnosisCount,
                            "repair", repairCount,
                            "completed", completedCount
                    ),
                    "recentActivity", requests.stream()
                            .sorted((r1, r2) -> r2.getUpdatedAt().compareTo(r1.getUpdatedAt()))
                            .limit(5)
                            .map(this::mapServiceRequestToDTO)
                            .collect(Collectors.toList())
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching dashboard stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard stats: " + e.getMessage()));
        }
    }

    /**
     * Helper method to map ServiceRequest to DTO
     */
    private ServiceRequestDTO mapServiceRequestToDTO(ServiceRequest request) {
        ServiceRequestDTO.ServiceRequestDTOBuilder builder = ServiceRequestDTO.builder()
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
                .updatedAt(request.getUpdatedAt());

        // Add service advisor info if assigned
        if (request.getServiceAdvisor() != null) {
            builder.serviceAdvisorId(request.getServiceAdvisor().getAdvisorId());

            User advisorUser = request.getServiceAdvisor().getUser();
            if (advisorUser != null) {
                builder.serviceAdvisorName(
                        (advisorUser.getFirstName() != null ? advisorUser.getFirstName() : "") + " " +
                                (advisorUser.getLastName() != null ? advisorUser.getLastName() : "").trim()
                );
            }
        }

        // Add customer info if available
        try {
            if (request.getUserId() != null) {
                User user = userRepository.findById(request.getUserId().intValue()).orElse(null);
                if (user != null) {
                    builder.customerName(user.getFirstName() + " " + user.getLastName())
                            .customerEmail(user.getEmail())
                            .customerPhone(user.getPhoneNumber())
                            .membershipStatus(user.getMembershipType().toString());
                }
            }
        } catch (Exception e) {
            logger.warn("Error retrieving customer info for user ID: {}", request.getUserId(), e);
        }

        // Add materials used if available
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

            builder.materials(materials);
        } catch (Exception e) {
            logger.warn("Error retrieving materials for request ID: {}", request.getRequestId(), e);
        }

        return builder.build();
    }
}