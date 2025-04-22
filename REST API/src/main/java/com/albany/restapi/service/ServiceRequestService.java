package com.albany.restapi.service;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ServiceRequestDTO> getAllServiceRequests() {
        log.info("Getting all service requests");
        return serviceRequestRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceRequestDTO> getServiceRequestsByCustomer(Integer customerId) {
        log.info("Getting service requests for customer ID: {}", customerId);
        // Try both with customer ID and user ID
        List<ServiceRequest> requests = serviceRequestRepository.findByVehicle_Customer_CustomerId(customerId);

        if (requests.isEmpty()) {
            // Try finding by user ID
            CustomerProfile customerProfile = customerProfileRepository.findByUserId(customerId);
            if (customerProfile != null) {
                requests = serviceRequestRepository.findByVehicle_Customer_CustomerId(customerProfile.getCustomerId());
            }
        }

        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceRequestDTO> getServiceRequestsByAdvisor(Integer advisorId) {
        log.info("Getting service requests for advisor ID: {}", advisorId);
        return serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceRequestDTO> getServiceRequestsByStatus(ServiceRequest.Status status) {
        log.info("Getting service requests with status: {}", status);
        return serviceRequestRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServiceRequestDTO getServiceRequestById(Integer requestId) {
        log.info("Getting service request by ID: {}", requestId);
        return serviceRequestRepository.findById(requestId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));
    }

    @Transactional
    public ServiceRequestDTO createServiceRequest(ServiceRequestDTO requestDTO) {
        log.info("Creating new service request for vehicle ID: {}", requestDTO.getVehicleId());

        try {
            // Get the vehicle
            Vehicle vehicle = vehicleRepository.findById(requestDTO.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + requestDTO.getVehicleId()));

            log.debug("Found vehicle: {}", vehicle);

            // Create service request
            ServiceRequest serviceRequest = new ServiceRequest();
            serviceRequest.setVehicle(vehicle);
            serviceRequest.setServiceType(requestDTO.getServiceType());
            serviceRequest.setDeliveryDate(requestDTO.getDeliveryDate());
            serviceRequest.setAdditionalDescription(requestDTO.getAdditionalDescription());
            serviceRequest.setStatus(ServiceRequest.Status.Received);

            // If admin ID is provided, set it
            if (requestDTO.getAdminId() != null) {
                AdminProfile admin = adminProfileRepository.findById(requestDTO.getAdminId())
                        .orElse(null);
                serviceRequest.setAdmin(admin);
            }

            // If advisor ID is provided, set it
            if (requestDTO.getServiceAdvisorId() != null) {
                ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(requestDTO.getServiceAdvisorId())
                        .orElse(null);
                serviceRequest.setServiceAdvisor(advisor);
            }

            // Save service request
            ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
            log.info("Successfully created service request with ID: {}", savedRequest.getRequestId());

            return convertToDTO(savedRequest);
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create service request: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceRequestDTO assignServiceAdvisor(Integer requestId, Integer advisorId) {
        log.info("Assigning advisor ID: {} to service request ID: {}", advisorId, requestId);

        // Get the service request
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));

        // Get the service advisor
        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                .orElseThrow(() -> new RuntimeException("Service advisor not found with ID: " + advisorId));

        // Assign advisor
        serviceRequest.setServiceAdvisor(advisor);

        // Changed status to Diagnosis when advisor is assigned if the status is still Received
        if (serviceRequest.getStatus() == ServiceRequest.Status.Received) {
            serviceRequest.setStatus(ServiceRequest.Status.Diagnosis);
        }

        // Save and return
        ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
        log.info("Successfully assigned advisor to service request");

        return convertToDTO(updatedRequest);
    }

    @Transactional
    public ServiceRequestDTO updateServiceRequestStatus(Integer requestId, ServiceRequest.Status newStatus) {
        log.info("Updating service request ID: {} to status: {}", requestId, newStatus);

        // Get the service request
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));

        // Update status
        serviceRequest.setStatus(newStatus);

        // Save and return
        ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
        log.info("Successfully updated service request status");

        return convertToDTO(updatedRequest);
    }

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