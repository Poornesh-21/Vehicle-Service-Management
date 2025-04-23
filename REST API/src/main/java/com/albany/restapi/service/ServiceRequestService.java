package com.albany.restapi.service;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.exception.ResourceNotFoundException;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ServiceAdvisorProfileRepository serviceAdvisorRepository;
    private final CustomerProfileRepository customerProfileRepository;

    /**
     * Get all service requests
     */
    public List<ServiceRequestDTO> getAllServiceRequests() {
        log.info("Fetching all service requests");
        List<ServiceRequest> requests = serviceRequestRepository.findAll();
        log.debug("Found {} service requests", requests.size());
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get service requests by customer ID
     */
    public List<ServiceRequestDTO> getServiceRequestsByCustomer(Integer customerId) {
        log.info("Fetching service requests for customer: {}", customerId);
        List<ServiceRequest> requests = serviceRequestRepository.findByVehicle_Customer_User_UserId(customerId);
        log.debug("Found {} service requests for customer {}", requests.size(), customerId);
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get service requests by service advisor ID
     */
    public List<ServiceRequestDTO> getServiceRequestsByAdvisor(Integer advisorId) {
        log.info("Fetching service requests for advisor: {}", advisorId);
        List<ServiceRequest> requests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisorId);
        log.debug("Found {} service requests for advisor {}", requests.size(), advisorId);
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get service requests by status
     */
    public List<ServiceRequestDTO> getServiceRequestsByStatus(ServiceRequest.Status status) {
        log.info("Fetching service requests with status: {}", status);
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(status);
        log.debug("Found {} service requests with status {}", requests.size(), status);
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get service request by ID
     */
    public ServiceRequestDTO getServiceRequestById(Integer requestId) {
        log.info("Fetching service request with ID: {}", requestId);
        return serviceRequestRepository.findById(requestId)
                .map(this::convertToDTO)
                .orElseThrow(() -> {
                    log.warn("Service request not found with ID: {}", requestId);
                    return new ResourceNotFoundException("Service request not found with ID: " + requestId);
                });
    }

    /**
     * Create a new service request
     */
    @Transactional
    public ServiceRequestDTO createServiceRequest(ServiceRequestDTO requestDTO) {
        try {
            log.info("Creating new service request: {}", requestDTO);

            // Validate required fields
            validateServiceRequestDTO(requestDTO);

            // Get the vehicle
            Vehicle vehicle = vehicleRepository.findById(requestDTO.getVehicleId())
                    .orElseThrow(() -> {
                        log.warn("Vehicle not found with ID: {}", requestDTO.getVehicleId());
                        return new ResourceNotFoundException("Vehicle not found with ID: " + requestDTO.getVehicleId());
                    });

            // Create new service request
            ServiceRequest serviceRequest = new ServiceRequest();

            // Populate service request with DTO data
            populateServiceRequestFromDTO(serviceRequest, requestDTO, vehicle);

            // Save the service request
            log.debug("Saving service request");
            ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
            log.info("Service request created successfully with ID: {}", savedRequest.getRequestId());

            // Update customer's service information
            updateCustomerServiceInfo(vehicle);

            return convertToDTO(savedRequest);
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create service request: " + e.getMessage(), e);
        }
    }

    /**
     * Assign a service advisor to a service request
     */
    @Transactional
    public ServiceRequestDTO assignServiceAdvisor(Integer requestId, Integer advisorId) {
        try {
            log.info("Assigning service advisor {} to request {}", advisorId, requestId);

            // Get the service request
            ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> {
                        log.warn("Service request not found with ID: {}", requestId);
                        return new ResourceNotFoundException("Service request not found with ID: " + requestId);
                    });

            // Get the service advisor
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                    .orElseThrow(() -> {
                        log.warn("Service advisor not found with ID: {}", advisorId);
                        return new ResourceNotFoundException("Service advisor not found with ID: " + advisorId);
                    });

            // Assign advisor
            serviceRequest.setServiceAdvisor(advisor);

            // Change status to Diagnosis when advisor is assigned if the status is still Received
            if (serviceRequest.getStatus() == ServiceRequest.Status.Received) {
                serviceRequest.setStatus(ServiceRequest.Status.Diagnosis);
                log.debug("Updated status to Diagnosis");
            }

            // Save and return
            ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
            log.info("Service advisor assigned successfully");

            return convertToDTO(updatedRequest);
        } catch (Exception e) {
            log.error("Error assigning service advisor: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign service advisor: " + e.getMessage(), e);
        }
    }

    /**
     * Update the status of a service request
     */
    @Transactional
    public ServiceRequestDTO updateServiceRequestStatus(Integer requestId, ServiceRequest.Status newStatus) {
        try {
            log.info("Updating service request {} status to {}", requestId, newStatus);

            // Get the service request
            ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                    .orElseThrow(() -> {
                        log.warn("Service request not found with ID: {}", requestId);
                        return new ResourceNotFoundException("Service request not found with ID: " + requestId);
                    });

            // Update status
            serviceRequest.setStatus(newStatus);

            // Save and return
            ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
            log.info("Service request status updated successfully");

            return convertToDTO(updatedRequest);
        } catch (Exception e) {
            log.error("Error updating service request status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update service request status: " + e.getMessage(), e);
        }
    }

    /**
     * Validate service request DTO
     */
    private void validateServiceRequestDTO(ServiceRequestDTO requestDTO) {
        if (requestDTO.getVehicleId() == null) {
            throw new IllegalArgumentException("Vehicle ID is required");
        }

        if (requestDTO.getServiceType() == null || requestDTO.getServiceType().trim().isEmpty()) {
            throw new IllegalArgumentException("Service type is required");
        }

        if (requestDTO.getDeliveryDate() == null) {
            throw new IllegalArgumentException("Delivery date is required");
        }
    }

    /**
     * Populate service request from DTO
     */
    private void populateServiceRequestFromDTO(ServiceRequest serviceRequest,
                                               ServiceRequestDTO requestDTO,
                                               Vehicle vehicle) {
        serviceRequest.setVehicle(vehicle);
        serviceRequest.setServiceType(requestDTO.getServiceType());
        serviceRequest.setDeliveryDate(requestDTO.getDeliveryDate());
        serviceRequest.setAdditionalDescription(requestDTO.getAdditionalDescription());
        serviceRequest.setServiceDescription(requestDTO.getServiceDescription());

        // Populate vehicle-specific details
        serviceRequest.setVehicleModel(vehicle.getModel());
        serviceRequest.setVehicleRegistration(vehicle.getRegistrationNumber());
        serviceRequest.setVehicleType(vehicle.getCategory() != null ? vehicle.getCategory().name() : null);
        serviceRequest.setVehicleYear(vehicle.getYear());

        // Use the flexible status conversion method from DTO
        ServiceRequest.Status status = requestDTO.getStatusEnum();
        serviceRequest.setStatus(status);

        // Set admin if provided
        if (requestDTO.getAdminId() != null) {
            AdminProfile admin = adminProfileRepository.findById(requestDTO.getAdminId())
                    .orElse(null);
            serviceRequest.setAdmin(admin);
        }

        // Set service advisor if provided
        if (requestDTO.getServiceAdvisorId() != null) {
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(requestDTO.getServiceAdvisorId())
                    .orElse(null);
            serviceRequest.setServiceAdvisor(advisor);
        }

        // Set timestamps
        serviceRequest.setCreatedAt(LocalDateTime.now());
    }

    /**
     * Update customer's service information
     */
    private void updateCustomerServiceInfo(Vehicle vehicle) {
        if (vehicle.getCustomer() != null) {
            CustomerProfile customer = vehicle.getCustomer();
            customer.setLastServiceDate(LocalDate.now());

            // Increment total services count
            Integer totalServices = customer.getTotalServices();
            customer.setTotalServices(totalServices != null ? totalServices + 1 : 1);

            customerProfileRepository.save(customer);
            log.debug("Updated customer service information");
        }
    }

    /**
     * Convert service request entity to DTO
     */
    // Add this method to the ServiceRequestService class in the REST API
// This ensures the membership status is correctly set in the ServiceRequestDTO

    private ServiceRequestDTO convertToDTO(ServiceRequest serviceRequest) {
        ServiceRequestDTO dto = new ServiceRequestDTO();

        dto.setRequestId(serviceRequest.getRequestId());
        dto.setServiceType(serviceRequest.getServiceType());
        dto.setDeliveryDate(serviceRequest.getDeliveryDate());
        dto.setAdditionalDescription(serviceRequest.getAdditionalDescription());
        dto.setServiceDescription(serviceRequest.getServiceDescription());

        // Ensure status is always set properly, even if null in entity
        if (serviceRequest.getStatus() != null) {
            dto.setStatus(serviceRequest.getStatus().name());
            log.debug("Setting status for request {}: {}", serviceRequest.getRequestId(), serviceRequest.getStatus().name());
        } else {
            dto.setStatus("Received"); // Default to Received if null
            log.debug("Status was null for request {}, defaulting to Received", serviceRequest.getRequestId());
        }

        // Set vehicle details from entity's vehicle-specific fields
        dto.setVehicleModel(serviceRequest.getVehicleModel());
        dto.setRegistrationNumber(serviceRequest.getVehicleRegistration());
        dto.setVehicleType(serviceRequest.getVehicleType());
        dto.setVehicleYear(serviceRequest.getVehicleYear());

        // Set vehicle ID if available
        if (serviceRequest.getVehicle() != null) {
            dto.setVehicleId(serviceRequest.getVehicle().getVehicleId());
            dto.setVehicleBrand(serviceRequest.getVehicle().getBrand());
        }

        // Set admin info
        if (serviceRequest.getAdmin() != null) {
            dto.setAdminId(serviceRequest.getAdmin().getAdminId());
        }

        // Set service advisor info
        if (serviceRequest.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = serviceRequest.getServiceAdvisor();
            dto.setServiceAdvisorId(advisor.getAdvisorId());

            if (advisor.getUser() != null) {
                User user = advisor.getUser();
                dto.setServiceAdvisorName(user.getFirstName() + " " + user.getLastName());
            }
        }

        // Set customer info
        if (serviceRequest.getVehicle() != null &&
                serviceRequest.getVehicle().getCustomer() != null) {
            CustomerProfile customer = serviceRequest.getVehicle().getCustomer();

            // Explicit handling of membership status to ensure it's set correctly
            String membershipStatus = customer.getMembershipStatus();
            // Log the raw value from the database
            log.debug("Raw membership status from database for request {}: '{}'",
                    serviceRequest.getRequestId(), membershipStatus);

            // Use the exact value from database, only default if completely null
            if (membershipStatus == null) {
                membershipStatus = "Standard";
                log.debug("Membership status was null, defaulting to Standard");
            }

            dto.setMembershipStatus(membershipStatus);

            if (customer.getUser() != null) {
                User user = customer.getUser();
                dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                dto.setCustomerId(customer.getCustomerId());
                dto.setCustomerEmail(user.getEmail());
            }
        } else {
            // Default membership status if no customer is associated
            dto.setMembershipStatus("Standard");
        }

        return dto;
    }
}