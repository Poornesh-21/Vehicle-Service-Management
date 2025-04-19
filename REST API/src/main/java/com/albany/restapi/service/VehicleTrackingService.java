package com.albany.restapi.service;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.dto.VehicleInServiceDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceAdvisorProfileRepository serviceAdvisorRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Retrieves all vehicles currently under service (not completed)
     */
    public List<VehicleInServiceDTO> getVehiclesUnderService() {
        // Find service requests that are not completed
        List<ServiceRequest> activeRequests = serviceRequestRepository.findByStatusNot(ServiceRequest.Status.Completed);
        
        return activeRequests.stream()
                .map(this::mapToVehicleInServiceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all completed services
     */
    public List<CompletedServiceDTO> getCompletedServices() {
        // Find completed service requests
        List<ServiceRequest> completedRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Completed);
        
        return completedRequests.stream()
                .map(this::mapToCompletedServiceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific service request by ID
     */
    public Optional<ServiceRequest> getServiceRequestById(Integer requestId) {
        return serviceRequestRepository.findById(requestId);
    }

    /**
     * Updates the status of a service request
     */
    @Transactional
    public ServiceRequest updateServiceStatus(Integer requestId, ServiceRequest.Status newStatus) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        
        request.setStatus(newStatus);
        
        // If status is now completed, update the customer's last service date
        if (newStatus == ServiceRequest.Status.Completed) {
            CustomerProfile customer = request.getVehicle().getCustomer();
            customer.setLastServiceDate(LocalDate.now());
            customer.setTotalServices(customer.getTotalServices() + 1);
            
            // The customer repository will be updated via cascading
        }
        
        return serviceRequestRepository.save(request);
    }

    /**
     * Records a payment for a service request
     */
    @Transactional
    public void recordPayment(Integer requestId, Map<String, Object> paymentDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        
        // In a real application, you would create a Payment entity and save it to the database
        // For simplicity in this example, we'll just log the payment
        log.info("Payment recorded for service request {}: {}", requestId, paymentDetails);
    }

    /**
     * Dispatches a vehicle (marks as completed and delivered)
     */
    @Transactional
    public void dispatchVehicle(Integer requestId, Map<String, Object> dispatchDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        
        // In a real application, you might update the service request status to "Dispatched"
        // or create a Dispatch entity. For simplicity, we'll just log it
        log.info("Vehicle dispatched for service request {}: {}", requestId, dispatchDetails);
    }

    /**
     * Generates an invoice for a completed service
     */
    @Transactional
    public void generateInvoice(Integer requestId, Map<String, Object> invoiceDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        
        // In a real application, you would create an Invoice entity
        // For simplicity, we'll just log the invoice generation
        log.info("Invoice generated for service request {}: {}", requestId, invoiceDetails);
    }

    /**
     * Filter vehicles based on criteria
     */
    public List<VehicleInServiceDTO> filterVehiclesUnderService(Map<String, Object> filterCriteria) {
        List<VehicleInServiceDTO> allVehicles = getVehiclesUnderService();
        
        // Apply filters based on the criteria
        // This is a simple in-memory filtering; in a real application, you would do this with a database query
        return allVehicles.stream()
                .filter(vehicle -> applyFilters(vehicle, filterCriteria))
                .collect(Collectors.toList());
    }

    /**
     * Filter completed services based on criteria
     */
    public List<CompletedServiceDTO> filterCompletedServices(Map<String, Object> filterCriteria) {
        List<CompletedServiceDTO> allServices = getCompletedServices();
        
        // Apply filters
        return allServices.stream()
                .filter(service -> applyFiltersToCompletedService(service, filterCriteria))
                .collect(Collectors.toList());
    }

    // Helper methods to map entities to DTOs
    
    private VehicleInServiceDTO mapToVehicleInServiceDTO(ServiceRequest request) {
        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();
        
        // Calculate the estimated completion date (usually the delivery date)
        LocalDate estimatedCompletionDate = request.getDeliveryDate();
        
        // Get service advisor name if assigned
        String serviceAdvisorName = "Not Assigned";
        String serviceAdvisorId = "N/A";
        
        if (request.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = request.getServiceAdvisor();
            User advisorUser = advisor.getUser();
            serviceAdvisorName = advisorUser.getFirstName() + " " + advisorUser.getLastName();
            serviceAdvisorId = advisor.getFormattedId();
        }
        
        // Create the DTO
        return VehicleInServiceDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .serviceAdvisorName(serviceAdvisorName)
                .serviceAdvisorId(serviceAdvisorId)
                .status(request.getStatus().name())
                .startDate(request.getCreatedAt().toLocalDate())
                .estimatedCompletionDate(estimatedCompletionDate)
                .category(vehicle.getCategory().name())
                .customerName(customerUser.getFirstName() + " " + customerUser.getLastName())
                .customerEmail(customerUser.getEmail())
                .membershipStatus(customer.getMembershipStatus())
                .serviceType(request.getServiceType())
                .additionalDescription(request.getAdditionalDescription())
                .build();
    }
    
    private CompletedServiceDTO mapToCompletedServiceDTO(ServiceRequest request) {
        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();
        
        // Get service advisor name
        String serviceAdvisorName = "Not Assigned";
        if (request.getServiceAdvisor() != null) {
            User advisorUser = request.getServiceAdvisor().getUser();
            serviceAdvisorName = advisorUser.getFirstName() + " " + advisorUser.getLastName();
        }
        
        // Calculate total cost
        BigDecimal totalCost = calculateTotalCost(request);
        
        // Check if has invoice (in a real app, check if there's an invoice record)
        boolean hasInvoice = Math.random() > 0.3; // For demo purposes
        
        return CompletedServiceDTO.builder()
                .serviceId(request.getRequestId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .customerName(customerUser.getFirstName() + " " + customerUser.getLastName())
                .completedDate(LocalDate.now()) // In a real app, use the actual completion date
                .serviceAdvisorName(serviceAdvisorName)
                .totalCost(totalCost)
                .hasInvoice(hasInvoice)
                .build();
    }
    
    // Helper method to calculate total cost
    private BigDecimal calculateTotalCost(ServiceRequest request) {
        // In a real application, you would calculate this based on materials used and labor
        // For simplicity, we'll generate a random cost
        return new BigDecimal(Math.random() * 10000 + 1000).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    // Helper method to apply filters to vehicles under service
    private boolean applyFilters(VehicleInServiceDTO vehicle, Map<String, Object> criteria) {
        // Apply each filter if it exists in the criteria
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                continue; // Skip empty filters
            }
            
            switch (key) {
                case "vehicleType":
                    if (!vehicle.getCategory().equalsIgnoreCase((String) value)) {
                        return false;
                    }
                    break;
                case "serviceType":
                    if (!vehicle.getServiceType().equalsIgnoreCase((String) value)) {
                        return false;
                    }
                    break;
                case "status":
                    if (!vehicle.getStatus().equalsIgnoreCase((String) value)) {
                        return false;
                    }
                    break;
                case "dateFrom":
                    LocalDate fromDate = LocalDate.parse((String) value);
                    if (vehicle.getStartDate().isBefore(fromDate)) {
                        return false;
                    }
                    break;
                case "dateTo":
                    LocalDate toDate = LocalDate.parse((String) value);
                    if (vehicle.getEstimatedCompletionDate().isAfter(toDate)) {
                        return false;
                    }
                    break;
                case "search":
                    String search = ((String) value).toLowerCase();
                    boolean matches = vehicle.getVehicleName().toLowerCase().contains(search) ||
                            vehicle.getRegistrationNumber().toLowerCase().contains(search) ||
                            vehicle.getCustomerName().toLowerCase().contains(search);
                    if (!matches) {
                        return false;
                    }
                    break;
            }
        }
        
        return true; // All filters passed
    }
    
    // Helper method to apply filters to completed services
    private boolean applyFiltersToCompletedService(CompletedServiceDTO service, Map<String, Object> criteria) {
        // Similar to the vehicle filter method
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                continue;
            }
            
            switch (key) {
                case "vehicleType":
                    // Would need to add vehicle type to the DTO for this filter
                    break;
                case "serviceType":
                    // Would need to add service type to the DTO for this filter
                    break;
                case "paymentStatus":
                    // Would need to add payment status to the DTO for this filter
                    break;
                case "dateFrom":
                    LocalDate fromDate = LocalDate.parse((String) value);
                    if (service.getCompletedDate().isBefore(fromDate)) {
                        return false;
                    }
                    break;
                case "dateTo":
                    LocalDate toDate = LocalDate.parse((String) value);
                    if (service.getCompletedDate().isAfter(toDate)) {
                        return false;
                    }
                    break;
                case "search":
                    String search = ((String) value).toLowerCase();
                    boolean matches = service.getVehicleName().toLowerCase().contains(search) ||
                            service.getRegistrationNumber().toLowerCase().contains(search) ||
                            service.getCustomerName().toLowerCase().contains(search);
                    if (!matches) {
                        return false;
                    }
                    break;
            }
        }
        
        return true;
    }
}