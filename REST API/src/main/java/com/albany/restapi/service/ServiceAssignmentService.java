package com.albany.restapi.service;

import com.albany.restapi.dto.ServiceAssignmentDTO;
import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.dto.VehicleInServiceDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAssignmentService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final ServiceRequestService serviceRequestService;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final ServiceAdvisorProfileRepository serviceAdvisorRepository;

    /**
     * Get new service requests that need assignment
     */
    public List<ServiceRequestDTO> getNewServiceRequests() {
        // Get service requests with status "Received" that have no advisors assigned
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Received);

        // For this demo, we'll consider all "Received" requests as needing assignment
        return requests.stream()
                .map(this::mapToServiceRequestDTO)
                .collect(Collectors.toList());
    }

    /**
     * Assign a service request to an advisor
     */
    @Transactional
    public ServiceRequestDTO assignServiceRequest(
            ServiceAssignmentDTO assignmentDTO,
            String serviceAdvisorEmail) {

        try {
            // Find the service request
            ServiceRequest request = serviceRequestRepository.findById(assignmentDTO.getServiceRequestId())
                    .orElseThrow(() -> new RuntimeException("Service request not found with ID: " +
                            assignmentDTO.getServiceRequestId()));

            // Find the service advisor from email
            User advisorUser = userRepository.findByEmail(serviceAdvisorEmail)
                    .orElseThrow(() -> new RuntimeException("Service advisor not found with email: " +
                            serviceAdvisorEmail));

            // Get service advisor profile
            ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(advisorUser.getUserId())
                    .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " +
                            advisorUser.getUserId()));

            // Assign service advisor if not already assigned
            if (request.getServiceAdvisor() == null) {
                request.setServiceAdvisor(advisor);
            }

            // Update service request status to Diagnosis
            request.setStatus(ServiceRequest.Status.Diagnosis);

            // Update estimated completion date
            if (assignmentDTO.getEstimatedCompletionDate() != null) {
                request.setDeliveryDate(assignmentDTO.getEstimatedCompletionDate());
            }

            // Save updated service request
            request = serviceRequestRepository.save(request);

            // Create service tracking entry for this assignment
            ServiceTracking tracking = new ServiceTracking();
            tracking.setRequestId(request.getRequestId());
            tracking.setWorkDescription("Service assigned to advisor. " +
                    (assignmentDTO.getServiceNotes() != null ? "Notes: " + assignmentDTO.getServiceNotes() : ""));
            tracking.setStatus(ServiceRequest.Status.Diagnosis);
            tracking.setServiceAdvisor(advisor);

            // Save tracking entry
            serviceTrackingRepository.save(tracking);

            // Return updated service request
            return mapToServiceRequestDTO(request);

        } catch (Exception e) {
            log.error("Error assigning service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign service: " + e.getMessage(), e);
        }
    }

    /**
     * Get all service requests that have been assigned to service advisors (in progress)
     */
    public List<VehicleInServiceDTO> getAssignedRequests() {
        // Get service requests with status "Diagnosis" or "Repair"
        List<ServiceRequest> diagnosisRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Diagnosis);
        List<ServiceRequest> repairRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Repair);

        List<ServiceRequest> inProgressRequests = new ArrayList<>();
        inProgressRequests.addAll(diagnosisRequests);
        inProgressRequests.addAll(repairRequests);

        // Convert to DTOs
        return inProgressRequests.stream()
                .map(this::mapToVehicleInServiceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map a ServiceRequest entity to ServiceRequestDTO
     */
    private ServiceRequestDTO mapToServiceRequestDTO(ServiceRequest serviceRequest) {
        ServiceRequestDTO dto = new ServiceRequestDTO();

        dto.setRequestId(serviceRequest.getRequestId());
        dto.setServiceType(serviceRequest.getServiceType());
        dto.setDeliveryDate(serviceRequest.getDeliveryDate());
        dto.setAdditionalDescription(serviceRequest.getAdditionalDescription());
        dto.setServiceDescription(serviceRequest.getServiceDescription());

        // Set status
        if (serviceRequest.getStatus() != null) {
            dto.setStatus(serviceRequest.getStatus().name());
        } else {
            dto.setStatus("Received");
        }

        // Set vehicle details
        if (serviceRequest.getVehicle() != null) {
            Vehicle vehicle = serviceRequest.getVehicle();
            dto.setVehicleId(vehicle.getVehicleId());
            dto.setVehicleBrand(vehicle.getBrand());
            dto.setVehicleModel(vehicle.getModel());
            dto.setRegistrationNumber(vehicle.getRegistrationNumber());

            if (vehicle.getCategory() != null) {
                dto.setVehicleType(vehicle.getCategory().name());
            }

            dto.setVehicleYear(vehicle.getYear());

            // Set customer info
            if (vehicle.getCustomer() != null) {
                CustomerProfile customer = vehicle.getCustomer();
                dto.setCustomerId(customer.getCustomerId());

                // Set membership status with proper handling
                String membershipStatus = customer.getMembershipStatus();
                if (membershipStatus == null) {
                    membershipStatus = "Standard";
                }
                dto.setMembershipStatus(membershipStatus);

                if (customer.getUser() != null) {
                    User user = customer.getUser();
                    dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                    dto.setCustomerEmail(user.getEmail());
                }
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

    /**
     * Helper method to map a ServiceRequest to VehicleInServiceDTO
     */
    private VehicleInServiceDTO mapToVehicleInServiceDTO(ServiceRequest request) {
        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();

        // Get service advisor name if assigned
        String serviceAdvisorName = "Not Assigned";
        String serviceAdvisorId = "N/A";

        if (request.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = request.getServiceAdvisor();
            User advisorUser = advisor.getUser();
            serviceAdvisorName = advisorUser.getFirstName() + " " + advisorUser.getLastName();
            serviceAdvisorId = advisor.getFormattedId();
        }

        // Get start date and estimated completion date
        LocalDateTime startDate = request.getCreatedAt();

        return VehicleInServiceDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .serviceAdvisorName(serviceAdvisorName)
                .serviceAdvisorId(serviceAdvisorId)
                .status(request.getStatus().name())
                .startDate(startDate.toLocalDate())
                .estimatedCompletionDate(request.getDeliveryDate())
                .category(vehicle.getCategory().name())
                .customerName(customerUser.getFirstName() + " " + customerUser.getLastName())
                .customerEmail(customerUser.getEmail())
                .membershipStatus(customer.getMembershipStatus())
                .serviceType(request.getServiceType())
                .additionalDescription(request.getAdditionalDescription())
                .build();
    }
}