package com.albany.restapi.service;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ServiceRequestRepository serviceRequestRepository;

    public DashboardDTO getDashboardData() {
        // Get counts
        long vehiclesDueCount = serviceRequestRepository.countByStatus(ServiceRequest.Status.Received);
        long vehiclesInProgressCount = serviceRequestRepository.countByStatus(ServiceRequest.Status.Diagnosis) +
                serviceRequestRepository.countByStatus(ServiceRequest.Status.Repair);
        long vehiclesCompletedCount = serviceRequestRepository.countByStatus(ServiceRequest.Status.Completed);

        // Get revenue from completed services
        BigDecimal totalRevenue = calculateTotalRevenue();

        // Get lists of vehicles
        List<VehicleDueDTO> vehiclesDueList = getVehiclesDueList();
        List<VehicleInServiceDTO> vehiclesInServiceList = getVehiclesInServiceList();
        List<CompletedServiceDTO> completedServicesList = getCompletedServicesList();

        // Build the response
        return DashboardDTO.builder()
                .vehiclesDue((int) vehiclesDueCount)
                .vehiclesInProgress((int) vehiclesInProgressCount)
                .vehiclesCompleted((int) vehiclesCompletedCount)
                .totalRevenue(totalRevenue)
                .vehiclesDueList(vehiclesDueList)
                .vehiclesInServiceList(vehiclesInServiceList)
                .completedServicesList(completedServicesList)
                .build();
    }

    private BigDecimal calculateTotalRevenue() {
        // Get all completed service requests
        List<ServiceRequest> completedRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Completed);

        // Initialize revenue
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // For each completed request, calculate the total based on service type and vehicle category
        for (ServiceRequest request : completedRequests) {
            // Calculate cost based on service type and vehicle attributes
            BigDecimal serviceCost = calculateServiceCost(request);
            totalRevenue = totalRevenue.add(serviceCost);
        }

        return totalRevenue;
    }

    private BigDecimal calculateServiceCost(ServiceRequest request) {
        // Get base cost based on service type
        String serviceType = request.getServiceType();
        BigDecimal baseServiceCost = getServiceBaseCost(serviceType);

        // Apply multiplier based on vehicle category (premium for cars and trucks)
        BigDecimal categoryMultiplier = BigDecimal.ONE; // default
        if (request.getVehicle() != null && request.getVehicle().getCategory() != null) {
            switch (request.getVehicle().getCategory()) {
                case Car:
                    categoryMultiplier = BigDecimal.valueOf(1.2);
                    break;
                case Truck:
                    categoryMultiplier = BigDecimal.valueOf(1.5);
                    break;
                case Bike:
                    categoryMultiplier = BigDecimal.valueOf(0.8);
                    break;
            }
        }

        // Apply premium customer discount if applicable
        BigDecimal membershipMultiplier = BigDecimal.ONE;
        if (request.getVehicle() != null &&
                request.getVehicle().getCustomer() != null &&
                "Premium".equals(request.getVehicle().getCustomer().getMembershipStatus())) {
            membershipMultiplier = BigDecimal.valueOf(0.9); // 10% discount for premium members
        }

        // Calculate final cost
        BigDecimal finalCost = baseServiceCost
                .multiply(categoryMultiplier)
                .multiply(membershipMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        return finalCost;
    }

    private BigDecimal getServiceBaseCost(String serviceType) {
        if (serviceType == null) return BigDecimal.valueOf(5000.00);

        // Map common service types to base costs
        Map<String, BigDecimal> serviceCosts = new HashMap<>();
        serviceCosts.put("Oil Change", BigDecimal.valueOf(2000.00));
        serviceCosts.put("Brake Service", BigDecimal.valueOf(5000.00));
        serviceCosts.put("Tire Rotation", BigDecimal.valueOf(1500.00));
        serviceCosts.put("Engine Repair", BigDecimal.valueOf(15000.00));
        serviceCosts.put("Transmission Service", BigDecimal.valueOf(10000.00));
        serviceCosts.put("Regular Maintenance", BigDecimal.valueOf(3500.00));
        serviceCosts.put("Battery Replacement", BigDecimal.valueOf(4000.00));
        serviceCosts.put("Diagnostics", BigDecimal.valueOf(2500.00));

        return serviceCosts.getOrDefault(serviceType, BigDecimal.valueOf(5000.00));
    }

    private List<VehicleDueDTO> getVehiclesDueList() {
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Received);

        return requests.stream().map(request -> {
            String vehicleName = request.getVehicle().getBrand() + " " + request.getVehicle().getModel();
            String customerName = request.getVehicle().getCustomer().getUser().getFirstName() + " " +
                    request.getVehicle().getCustomer().getUser().getLastName();

            // Get the membership status directly from the database without modification
            String membershipStatus = request.getVehicle().getCustomer().getMembershipStatus();

            // Default to Standard only if completely null
            if (membershipStatus == null) {
                membershipStatus = "Standard";
            }

            return VehicleDueDTO.builder()
                    .requestId(request.getRequestId())
                    .vehicleName(vehicleName)
                    .registrationNumber(request.getVehicle().getRegistrationNumber())
                    .customerName(customerName)
                    .customerEmail(request.getVehicle().getCustomer().getUser().getEmail())
                    .status(request.getStatus().name())
                    .dueDate(request.getDeliveryDate())
                    .category(request.getVehicle().getCategory().name())
                    .membershipStatus(membershipStatus)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<VehicleInServiceDTO> getVehiclesInServiceList() {
        List<ServiceRequest> diagnosisRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Diagnosis);
        List<ServiceRequest> repairRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Repair);

        List<ServiceRequest> inServiceRequests = new ArrayList<>();
        inServiceRequests.addAll(diagnosisRequests);
        inServiceRequests.addAll(repairRequests);

        return inServiceRequests.stream().map(request -> {
            String vehicleName = request.getVehicle().getBrand() + " " + request.getVehicle().getModel();
            String advisorName = request.getServiceAdvisor() != null ?
                    request.getServiceAdvisor().getUser().getFirstName() + " " +
                            request.getServiceAdvisor().getUser().getLastName() :
                    "Not Assigned";

            String advisorId = request.getServiceAdvisor() != null ?
                    request.getServiceAdvisor().getFormattedId() :
                    "N/A";

            // For estimatedCompletionDate, we'll use delivery date
            LocalDate startDate = request.getCreatedAt().toLocalDate();

            // Get customer information
            String customerName = "N/A";
            String customerEmail = "N/A";
            String membershipStatus = "Standard";

            if (request.getVehicle() != null && request.getVehicle().getCustomer() != null) {
                if (request.getVehicle().getCustomer().getUser() != null) {
                    customerName = request.getVehicle().getCustomer().getUser().getFirstName() + " " +
                            request.getVehicle().getCustomer().getUser().getLastName();
                    customerEmail = request.getVehicle().getCustomer().getUser().getEmail();
                }

                if (request.getVehicle().getCustomer().getMembershipStatus() != null) {
                    membershipStatus = request.getVehicle().getCustomer().getMembershipStatus();
                }
            }

            return VehicleInServiceDTO.builder()
                    .requestId(request.getRequestId())
                    .vehicleName(vehicleName)
                    .registrationNumber(request.getVehicle().getRegistrationNumber())
                    .serviceAdvisorName(advisorName)
                    .serviceAdvisorId(advisorId)
                    .status(request.getStatus().name())
                    .startDate(startDate)
                    .estimatedCompletionDate(request.getDeliveryDate())
                    .category(request.getVehicle().getCategory().name())
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .membershipStatus(membershipStatus)
                    .serviceType(request.getServiceType())
                    .additionalDescription(request.getAdditionalDescription())
                    .build();
        }).collect(Collectors.toList());
    }

    private List<CompletedServiceDTO> getCompletedServicesList() {
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Completed);

        return requests.stream().map(request -> {
            String vehicleName = request.getVehicle().getBrand() + " " + request.getVehicle().getModel();
            String customerName = request.getVehicle().getCustomer().getUser().getFirstName() + " " +
                    request.getVehicle().getCustomer().getUser().getLastName();
            String advisorName = request.getServiceAdvisor() != null ?
                    request.getServiceAdvisor().getUser().getFirstName() + " " +
                            request.getServiceAdvisor().getUser().getLastName() :
                    "Not Assigned";

            // Calculate actual service cost
            BigDecimal totalCost = calculateServiceCost(request);

            // Check if there's an invoice (mock implementation)
            boolean hasInvoice = Math.random() > 0.3; // 70% chance of having an invoice

            return CompletedServiceDTO.builder()
                    .serviceId(request.getRequestId())
                    .vehicleName(vehicleName)
                    .registrationNumber(request.getVehicle().getRegistrationNumber())
                    .customerName(customerName)
                    .completedDate(LocalDate.now().minusDays((long)(Math.random() * 30)))
                    .serviceAdvisorName(advisorName)
                    .totalCost(totalCost)
                    .hasInvoice(hasInvoice)
                    .build();
        }).collect(Collectors.toList());
    }
}