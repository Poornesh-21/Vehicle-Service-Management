package com.albany.restapi.service.admin;

import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.exception.ServiceRequestExceptions;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final UserRepository userRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * Get all service requests
     */
    public List<ServiceRequestDTO> getAllServiceRequests() {
        return serviceRequestRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get service request by ID
     */
    public ServiceRequestDTO getServiceRequestById(Integer requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestExceptions.ServiceRequestNotFoundException(requestId));
        return mapToDTO(request);
    }

    /**
     * Create a new service request with improved validation
     */
    @Transactional
    public ServiceRequestDTO createServiceRequest(ServiceRequestDTO requestDTO) {
        logger.debug("Creating service request: {}", requestDTO);

        // Validate vehicle exists if vehicle ID is provided
        Vehicle vehicle = null;
        if (requestDTO.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(requestDTO.getVehicleId())
                    .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with id: " + requestDTO.getVehicleId()));

            // If vehicle registration is null, try to get it from the vehicle entity
            if (requestDTO.getVehicleRegistration() == null && vehicle.getRegistrationNumber() != null) {
                logger.debug("Setting vehicleRegistration from vehicle entity: {}", vehicle.getRegistrationNumber());
                requestDTO.setVehicleRegistration(vehicle.getRegistrationNumber());
            }
        }

        // Check for null registration and provide a default if needed
        if (requestDTO.getVehicleRegistration() == null) {
            logger.warn("Vehicle registration is null, using 'Unknown' as default");
            requestDTO.setVehicleRegistration("Unknown");
        }

        // Ensure vehicle brand is set
        if (requestDTO.getVehicleBrand() == null || requestDTO.getVehicleBrand().isEmpty()) {
            requestDTO.setVehicleBrand("Unknown");
        }

        // Ensure vehicle model is set
        if (requestDTO.getVehicleModel() == null || requestDTO.getVehicleModel().isEmpty()) {
            requestDTO.setVehicleModel("Unknown Model");
        }

        // Ensure vehicle type is set
        if (requestDTO.getVehicleType() == null || requestDTO.getVehicleType().isEmpty()) {
            requestDTO.setVehicleType("Car");
        }

        // Set initial status to Received if not specified
        if (requestDTO.getStatus() == null) {
            requestDTO.setStatus("Received");
        }

        // Determine user ID from vehicle if not provided
        Long userId = requestDTO.getUserId();
        if (userId == null && vehicle != null && vehicle.getCustomer() != null && vehicle.getCustomer().getUser() != null) {
            userId = vehicle.getCustomer().getUser().getUserId().longValue();
        }

        // Build service request entity with all required fields
        ServiceRequest serviceRequest = ServiceRequest.builder()
                .userId(userId)
                .vehicle(vehicle)
                .vehicleType(requestDTO.getVehicleType())
                .vehicleBrand(requestDTO.getVehicleBrand())
                .vehicleModel(requestDTO.getVehicleModel())
                .vehicleRegistration(requestDTO.getVehicleRegistration())
                .vehicleYear(requestDTO.getVehicleYear())
                .serviceType(requestDTO.getServiceType())
                .serviceDescription(requestDTO.getServiceDescription())
                .additionalDescription(requestDTO.getAdditionalDescription())
                .deliveryDate(requestDTO.getDeliveryDate())
                .status(ServiceRequest.Status.valueOf(requestDTO.getStatus()))
                .build();

        logger.debug("Saving service request: {}", serviceRequest);
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
        return mapToDTO(savedRequest);
    }

    /**
     * Update an existing service request
     */
    @Transactional
    public ServiceRequestDTO updateServiceRequest(Integer requestId, ServiceRequestDTO requestDTO) {
        ServiceRequest existingRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestExceptions.ServiceRequestNotFoundException(requestId));

        // Update basic fields
        if (requestDTO.getServiceType() != null) {
            existingRequest.setServiceType(requestDTO.getServiceType());
        }

        if (requestDTO.getServiceDescription() != null) {
            existingRequest.setServiceDescription(requestDTO.getServiceDescription());
        }

        if (requestDTO.getAdditionalDescription() != null) {
            existingRequest.setAdditionalDescription(requestDTO.getAdditionalDescription());
        }

        if (requestDTO.getDeliveryDate() != null) {
            existingRequest.setDeliveryDate(requestDTO.getDeliveryDate());
        }

        if (requestDTO.getStatus() != null) {
            try {
                existingRequest.setStatus(ServiceRequest.Status.valueOf(requestDTO.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status values
                logger.warn("Invalid status value: {}", requestDTO.getStatus());
            }
        }

        // Update vehicle info if provided
        if (requestDTO.getVehicleBrand() != null) {
            existingRequest.setVehicleBrand(requestDTO.getVehicleBrand());
        }

        if (requestDTO.getVehicleModel() != null) {
            existingRequest.setVehicleModel(requestDTO.getVehicleModel());
        }

        if (requestDTO.getVehicleRegistration() != null) {
            existingRequest.setVehicleRegistration(requestDTO.getVehicleRegistration());
        }

        if (requestDTO.getVehicleType() != null) {
            existingRequest.setVehicleType(requestDTO.getVehicleType());
        }

        if (requestDTO.getVehicleYear() != null) {
            existingRequest.setVehicleYear(requestDTO.getVehicleYear());
        }

        // Update vehicle reference if provided
        if (requestDTO.getVehicleId() != null &&
                (existingRequest.getVehicle() == null ||
                        !requestDTO.getVehicleId().equals(existingRequest.getVehicle().getVehicleId()))) {

            Vehicle vehicle = vehicleRepository.findById(requestDTO.getVehicleId())
                    .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with id: " + requestDTO.getVehicleId()));

            existingRequest.setVehicle(vehicle);

            // Update user ID if vehicle's customer changed
            if (vehicle.getCustomer() != null && vehicle.getCustomer().getUser() != null) {
                existingRequest.setUserId(vehicle.getCustomer().getUser().getUserId().longValue());
            }
        }

        existingRequest.setUpdatedAt(LocalDateTime.now());
        ServiceRequest updatedRequest = serviceRequestRepository.save(existingRequest);
        return mapToDTO(updatedRequest);
    }

    /**
     * Assign a service advisor to a service request
     */
    @Transactional
    public ServiceRequestDTO assignServiceAdvisor(Integer requestId, Integer advisorId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestExceptions.ServiceRequestNotFoundException(requestId));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                .orElseThrow(() -> new EntityNotFoundException("Service advisor not found with id: " + advisorId));

        request.setServiceAdvisor(advisor);
        request.setUpdatedAt(LocalDateTime.now());

        ServiceRequest updatedRequest = serviceRequestRepository.save(request);
        return mapToDTO(updatedRequest);
    }

    /**
     * Update the status of a service request
     */
    @Transactional
    public ServiceRequestDTO updateServiceRequestStatus(Integer requestId, ServiceRequest.Status status) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestExceptions.ServiceRequestNotFoundException(requestId));

        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now());

        ServiceRequest updatedRequest = serviceRequestRepository.save(request);
        return mapToDTO(updatedRequest);
    }

    /**
     * Map ServiceRequest entity to DTO with additional information
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