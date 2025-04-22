package com.albany.restapi.service;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
     * Get service requests by advisor ID
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
                    return new RuntimeException("Service request not found with ID: " + requestId);
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
            if (requestDTO.getVehicleId() == null) {
                throw new IllegalArgumentException("Vehicle ID is required");
            }

            if (requestDTO.getServiceType() == null || requestDTO.getServiceType().trim().isEmpty()) {
                throw new IllegalArgumentException("Service type is required");
            }

            if (requestDTO.getDeliveryDate() == null) {
                throw new IllegalArgumentException("Delivery date is required");
            }

            // Get the vehicle
            Vehicle vehicle = vehicleRepository.findById(requestDTO.getVehicleId())
                    .orElseThrow(() -> {
                        log.warn("Vehicle not found with ID: {}", requestDTO.getVehicleId());
                        return new RuntimeException("Vehicle not found with ID: " + requestDTO.getVehicleId());
                    });

            log.debug("Found vehicle: {} {}, Registration: {}",
                    vehicle.getBrand(), vehicle.getModel(), vehicle.getRegistrationNumber());

            // Create new service request
            ServiceRequest serviceRequest = new ServiceRequest();
            serviceRequest.setVehicle(vehicle);
            serviceRequest.setServiceType(requestDTO.getServiceType());
            serviceRequest.setDeliveryDate(requestDTO.getDeliveryDate());
            serviceRequest.setAdditionalDescription(requestDTO.getAdditionalDescription());
            serviceRequest.setStatus(ServiceRequest.Status.Received);
            serviceRequest.setCreatedAt(LocalDateTime.now());

            // If admin ID is provided, set admin
            if (requestDTO.getAdminId() != null) {
                adminProfileRepository.findById(requestDTO.getAdminId()).ifPresent(serviceRequest::setAdmin);
            }

            // Save the service request
            log.debug("Saving service request");
            ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
            log.info("Service request created successfully with ID: {}", savedRequest.getRequestId());

            // Update customer's last service date if applicable
            if (vehicle.getCustomer() != null) {
                CustomerProfile customer = vehicle.getCustomer();
                customer.setLastServiceDate(LocalDate.now());
                customer.setTotalServices(customer.getTotalServices() + 1);
                customerProfileRepository.save(customer);
                log.debug("Updated customer service information");
            }

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
                        return new RuntimeException("Service request not found with ID: " + requestId);
                    });

            // Get the service advisor
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                    .orElseThrow(() -> {
                        log.warn("Service advisor not found with ID: {}", advisorId);
                        return new RuntimeException("Service advisor not found with ID: " + advisorId);
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
                        return new RuntimeException("Service request not found with ID: " + requestId);
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
     * Convert service request entity to DTO
     */
    private ServiceRequestDTO convertToDTO(ServiceRequest serviceRequest) {
        ServiceRequestDTO dto = new ServiceRequestDTO();

        dto.setRequestId(serviceRequest.getRequestId());
        dto.setServiceType(serviceRequest.getServiceType());
        dto.setDeliveryDate(serviceRequest.getDeliveryDate());
        dto.setAdditionalDescription(serviceRequest.getAdditionalDescription());
        dto.setStatus(serviceRequest.getStatus());

        // Set vehicle info
        if (serviceRequest.getVehicle() != null) {
            Vehicle vehicle = serviceRequest.getVehicle();
            dto.setVehicleId(vehicle.getVehicleId());
            dto.setVehicleBrand(vehicle.getBrand());
            dto.setVehicleModel(vehicle.getModel());
            dto.setRegistrationNumber(vehicle.getRegistrationNumber());

            // Set customer info
            if (vehicle.getCustomer() != null && vehicle.getCustomer().getUser() != null) {
                User user = vehicle.getCustomer().getUser();
                dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                dto.setCustomerId(vehicle.getCustomer().getCustomerId());
            }
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

        return dto;
    }
}