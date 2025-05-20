package com.albany.restapi.service.admin;

import com.albany.restapi.dto.VehicleTrackingDTO;
import com.albany.restapi.exception.ServiceRequestExceptions;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.ServiceAdvisorProfile;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.ServiceAdvisorRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import com.albany.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleTrackingService.class);

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;

    /**
     * Get all vehicles currently under service (not completed)
     */
    public List<VehicleTrackingDTO> getVehiclesUnderService() {
        return serviceRequestRepository.findAllUnderService().stream()
                .map(this::mapToTrackingDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get details for a specific service request
     */
    public VehicleTrackingDTO getServiceRequestDetails(Integer requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestExceptions.ServiceRequestNotFoundException(requestId));

        return mapToTrackingDTO(request);
    }

    /**
     * Update the status of a service request
     */
    @Transactional
    public VehicleTrackingDTO updateServiceRequestStatus(Integer requestId, ServiceRequest.Status status) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestExceptions.ServiceRequestNotFoundException(requestId));

        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now());

        ServiceRequest updatedRequest = serviceRequestRepository.save(request);
        return mapToTrackingDTO(updatedRequest);
    }

    /**
     * Filter vehicles under service based on various criteria
     */
    public List<VehicleTrackingDTO> filterVehiclesUnderService(Map<String, Object> filterCriteria) {
        List<ServiceRequest> allRequests = serviceRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() != ServiceRequest.Status.Completed)
                .collect(Collectors.toList());

        return allRequests.stream()
                .filter(request -> matchesFilters(request, filterCriteria))
                .map(this::mapToTrackingDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to check if a service request matches the filter criteria
     */
    private boolean matchesFilters(ServiceRequest request, Map<String, Object> filterCriteria) {
        String vehicleType = (String) filterCriteria.get("vehicleType");
        String serviceType = (String) filterCriteria.get("serviceType");
        String status = (String) filterCriteria.get("status");
        String dateFrom = (String) filterCriteria.get("dateFrom");
        String dateTo = (String) filterCriteria.get("dateTo");
        String serviceAdvisor = (String) filterCriteria.get("serviceAdvisor");

        // If filter is null or empty, consider it a match
        if (vehicleType != null && !vehicleType.isEmpty() &&
                !request.getVehicleType().equalsIgnoreCase(vehicleType)) {
            return false;
        }

        if (serviceType != null && !serviceType.isEmpty() &&
                !request.getServiceType().equalsIgnoreCase(serviceType)) {
            return false;
        }

        if (status != null && !status.isEmpty() &&
                !request.getStatus().name().equalsIgnoreCase(status)) {
            return false;
        }

        // Date filtering
        if (dateFrom != null && !dateFrom.isEmpty()) {
            LocalDate fromDate = LocalDate.parse(dateFrom);
            if (request.getCreatedAt().toLocalDate().isBefore(fromDate)) {
                return false;
            }
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            LocalDate toDate = LocalDate.parse(dateTo);
            if (request.getCreatedAt().toLocalDate().isAfter(toDate)) {
                return false;
            }
        }

        // Service advisor filtering
        if (serviceAdvisor != null && !serviceAdvisor.isEmpty()) {
            if (request.getServiceAdvisor() == null) {
                return false;
            }

            Integer advisorId;
            try {
                advisorId = Integer.parseInt(serviceAdvisor);
            } catch (NumberFormatException e) {
                // If it's not a valid number, consider it a match
                return true;
            }

            return request.getServiceAdvisor().getAdvisorId().equals(advisorId);
        }

        return true;
    }

    /**
     * Map a ServiceRequest entity to VehicleTrackingDTO
     */
    private VehicleTrackingDTO mapToTrackingDTO(ServiceRequest request) {
        String vehicleName = "";
        if (request.getVehicleBrand() != null && request.getVehicleModel() != null) {
            vehicleName = request.getVehicleBrand() + " " + request.getVehicleModel();
        } else if (request.getVehicle() != null) {
            vehicleName = request.getVehicle().getBrand() + " " + request.getVehicle().getModel();
        }

        String category = request.getVehicleType();
        if (request.getVehicle() != null && request.getVehicle().getCategory() != null) {
            category = request.getVehicle().getCategory().name();
        }

        // Get customer information
        String customerName = "Unknown Customer";
        String customerEmail = "";
        String customerPhone = "";
        String membershipStatus = "Standard";

        if (request.getUserId() != null) {
            try {
                User user = userRepository.findById(request.getUserId().intValue()).orElse(null);
                if (user != null) {
                    customerName = user.getFirstName() + " " + user.getLastName();
                    customerEmail = user.getEmail();
                    customerPhone = user.getPhoneNumber();
                    membershipStatus = user.getMembershipType().name();

                    // Try to find customer profile for more details
                    CustomerProfile customerProfile = customerRepository.findByUser_UserId(user.getUserId()).orElse(null);
                    if (customerProfile != null && customerProfile.getMembershipStatus() != null) {
                        membershipStatus = customerProfile.getMembershipStatus();
                    }
                }
            } catch (Exception e) {
                logger.warn("Error getting customer info for user ID: " + request.getUserId(), e);
            }
        }

        // Get service advisor information
        String serviceAdvisorName = "Not Assigned";
        Integer serviceAdvisorId = null;
        if (request.getServiceAdvisor() != null) {
            serviceAdvisorId = request.getServiceAdvisor().getAdvisorId();
            User advisorUser = request.getServiceAdvisor().getUser();
            if (advisorUser != null) {
                serviceAdvisorName = advisorUser.getFirstName() + " " + advisorUser.getLastName();
            }
        }

        return VehicleTrackingDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(vehicleName)
                .vehicleBrand(request.getVehicleBrand())
                .vehicleModel(request.getVehicleModel())
                .registrationNumber(request.getVehicleRegistration())
                .category(category)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .membershipStatus(membershipStatus)
                .serviceType(request.getServiceType())
                .serviceAdvisorName(serviceAdvisorName)
                .serviceAdvisorId(serviceAdvisorId)
                .startDate(request.getCreatedAt())
                .estimatedCompletionDate(request.getDeliveryDate())
                .status(request.getStatus().name())
                .additionalDescription(request.getAdditionalDescription())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}