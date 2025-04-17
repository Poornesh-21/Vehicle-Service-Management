package com.albany.restapi.service;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

        // Get revenue (this would normally come from payments/invoices)
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
        // In a real implementation, this would query the Invoices or Payments table
        // For now, we'll return a mock value
        return new BigDecimal("384000.00");
    }

    private List<VehicleDueDTO> getVehiclesDueList() {
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Received);

        return requests.stream().map(request -> {
            String vehicleName = request.getVehicle().getBrand() + " " + request.getVehicle().getModel();
            String customerName = request.getVehicle().getCustomer().getUser().getFirstName() + " " +
                    request.getVehicle().getCustomer().getUser().getLastName();

            // Get the membership status directly from the database without modification
            // This ensures "Premium" stays as "Premium" and isn't altered
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
                    .membershipStatus(membershipStatus) // Preserve exact case and value
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

            // In a real implementation, you would get the actual cost from invoices
            // For now, we'll generate a random value
            BigDecimal totalCost = BigDecimal.valueOf(30000 + Math.random() * 50000);

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