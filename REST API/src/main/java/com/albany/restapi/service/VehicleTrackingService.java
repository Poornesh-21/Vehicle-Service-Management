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
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

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

        ServiceRequest.Status oldStatus = request.getStatus();
        request.setStatus(newStatus);

        // Create a service tracking entry to record this status change
        ServiceTracking tracking = new ServiceTracking();
        tracking.setRequestId(requestId);
        tracking.setWorkDescription("Status updated from " + oldStatus + " to " + newStatus);
        tracking.setStatus(newStatus);

        // If status is now completed, update the customer's last service date
        if (newStatus == ServiceRequest.Status.Completed) {
            CustomerProfile customer = request.getVehicle().getCustomer();
            customer.setLastServiceDate(LocalDate.now());
            customer.setTotalServices(customer.getTotalServices() + 1);

            // Calculate final labor cost if not set
            if (tracking.getLaborCost() == null) {
                // Default to at least 1 labor hour (60 minutes) if no tracking exists
                Integer laborMinutes = serviceTrackingRepository.sumLaborMinutesByRequestId(requestId);
                if (laborMinutes == null || laborMinutes == 0) {
                    laborMinutes = 60;
                }

                // Calculate labor cost at $10 per minute (simplified example)
                BigDecimal laborRate = new BigDecimal("10.00");
                tracking.setLaborMinutes(laborMinutes);
                tracking.setLaborCost(laborRate.multiply(new BigDecimal(laborMinutes)));
            }

            // Calculate material cost from actual used materials
            BigDecimal materialCost = calculateMaterialCost(requestId);
            tracking.setTotalMaterialCost(materialCost);
        }

        serviceTrackingRepository.save(tracking);
        return serviceRequestRepository.save(request);
    }

    /**
     * Records a payment for a service request
     */
    @Transactional
    public void recordPayment(Integer requestId, Map<String, Object> paymentDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Create and store actual Payment entity
        Payment payment = new Payment();
        payment.setRequestId(requestId);
        payment.setCustomerId(request.getVehicle().getCustomer().getCustomerId());

        // Set amount from payment details
        if (paymentDetails.containsKey("amount")) {
            try {
                BigDecimal amount = new BigDecimal(paymentDetails.get("amount").toString());
                payment.setAmount(amount);
            } catch (NumberFormatException e) {
                log.error("Invalid amount in payment details", e);
                throw new IllegalArgumentException("Invalid payment amount");
            }
        } else {
            // If amount not provided, calculate from service costs
            payment.setAmount(calculateTotalCost(request));
        }

        // Set payment method
        if (paymentDetails.containsKey("paymentMethod")) {
            String methodStr = paymentDetails.get("paymentMethod").toString();
            try {
                Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(methodStr);
                payment.setPaymentMethod(method);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment method: {}. Defaulting to Card.", methodStr);
                payment.setPaymentMethod(Payment.PaymentMethod.Card);
            }
        } else {
            payment.setPaymentMethod(Payment.PaymentMethod.Card);
        }

        // Set transaction ID
        if (paymentDetails.containsKey("transactionId")) {
            payment.setTransactionId(paymentDetails.get("transactionId").toString());
        } else {
            payment.setTransactionId("TXN" + System.currentTimeMillis());
        }

        payment.setStatus(Payment.PaymentStatus.Completed);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment recorded for service request {}: {}", requestId, savedPayment);
    }

    /**
     * Generates an invoice for a completed service
     */
    @Transactional
    public Invoice generateInvoice(Integer requestId, Map<String, Object> invoiceDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find existing payment or create one if needed
        Payment payment = paymentRepository.findByRequestId(requestId)
                .orElseThrow(() -> new RuntimeException("Payment not found for service request"));

        // Calculate costs
        BigDecimal totalAmount = calculateTotalCost(request);
        BigDecimal taxRate = new BigDecimal("0.18"); // 18% tax
        BigDecimal taxes = totalAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = totalAmount.add(taxes).setScale(2, RoundingMode.HALF_UP);

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setRequestId(requestId);
        invoice.setPaymentId(payment.getPaymentId());
        invoice.setTotalAmount(totalAmount);
        invoice.setTaxes(taxes);
        invoice.setNetAmount(netAmount);
        invoice.setDownloadable(true);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice generated for service request {}: {}", requestId, savedInvoice);

        return savedInvoice;
    }

    /**
     * Dispatches a vehicle (marks as completed and delivered)
     */
    @Transactional
    public void dispatchVehicle(Integer requestId, Map<String, Object> dispatchDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Ensure status is Completed
        if (request.getStatus() != ServiceRequest.Status.Completed) {
            updateServiceStatus(requestId, ServiceRequest.Status.Completed);
        }

        // Check if payment exists
        boolean hasPayment = paymentRepository.existsByRequestId(requestId);
        if (!hasPayment) {
            throw new RuntimeException("Cannot dispatch vehicle without payment");
        }

        // Check if invoice exists, create if not
        boolean hasInvoice = invoiceRepository.existsByRequestId(requestId);
        if (!hasInvoice) {
            generateInvoice(requestId, dispatchDetails);
        }

        // Add a service tracking entry for the dispatch
        ServiceTracking tracking = new ServiceTracking();
        tracking.setRequestId(requestId);
        tracking.setWorkDescription("Vehicle dispatched to customer");
        tracking.setStatus(ServiceRequest.Status.Completed);
        serviceTrackingRepository.save(tracking);

        log.info("Vehicle dispatched for service request {}", requestId);
    }

    /**
     * Filter vehicles based on criteria
     */
    public List<VehicleInServiceDTO> filterVehiclesUnderService(Map<String, Object> filterCriteria) {
        // Start with all vehicles under service
        List<ServiceRequest> filteredRequests = serviceRequestRepository.findByStatusNot(ServiceRequest.Status.Completed);

        // Apply database-level filters where possible
        if (filterCriteria.containsKey("vehicleType") && filterCriteria.get("vehicleType") != null) {
            String vehicleType = filterCriteria.get("vehicleType").toString();
            try {
                Vehicle.Category category = Vehicle.Category.valueOf(vehicleType);
                filteredRequests = filteredRequests.stream()
                        .filter(req -> req.getVehicle().getCategory() == category)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid vehicle category filter: {}", vehicleType);
            }
        }

        if (filterCriteria.containsKey("status") && filterCriteria.get("status") != null) {
            String statusStr = filterCriteria.get("status").toString();
            try {
                ServiceRequest.Status status = ServiceRequest.Status.valueOf(statusStr);
                filteredRequests = filteredRequests.stream()
                        .filter(req -> req.getStatus() == status)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", statusStr);
            }
        }

        if (filterCriteria.containsKey("serviceType") && filterCriteria.get("serviceType") != null) {
            String serviceType = filterCriteria.get("serviceType").toString();
            filteredRequests = filteredRequests.stream()
                    .filter(req -> serviceType.equals(req.getServiceType()))
                    .collect(Collectors.toList());
        }

        if (filterCriteria.containsKey("search") && filterCriteria.get("search") != null) {
            String search = filterCriteria.get("search").toString().toLowerCase();
            filteredRequests = filteredRequests.stream()
                    .filter(req ->
                            (req.getVehicle().getBrand() + " " + req.getVehicle().getModel()).toLowerCase().contains(search) ||
                                    req.getVehicle().getRegistrationNumber().toLowerCase().contains(search) ||
                                    (req.getVehicle().getCustomer().getUser().getFirstName() + " " +
                                            req.getVehicle().getCustomer().getUser().getLastName()).toLowerCase().contains(search)
                    )
                    .collect(Collectors.toList());
        }

        // Map filtered requests to DTOs
        return filteredRequests.stream()
                .map(this::mapToVehicleInServiceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Filter completed services based on criteria
     */
    public List<CompletedServiceDTO> filterCompletedServices(Map<String, Object> filterCriteria) {
        // Start with all completed services
        List<ServiceRequest> filteredRequests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Completed);

        // Apply database-level filters where possible
        if (filterCriteria.containsKey("vehicleType") && filterCriteria.get("vehicleType") != null) {
            String vehicleType = filterCriteria.get("vehicleType").toString();
            try {
                Vehicle.Category category = Vehicle.Category.valueOf(vehicleType);
                filteredRequests = filteredRequests.stream()
                        .filter(req -> req.getVehicle().getCategory() == category)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid vehicle category filter: {}", vehicleType);
            }
        }

        if (filterCriteria.containsKey("search") && filterCriteria.get("search") != null) {
            String search = filterCriteria.get("search").toString().toLowerCase();
            filteredRequests = filteredRequests.stream()
                    .filter(req ->
                            (req.getVehicle().getBrand() + " " + req.getVehicle().getModel()).toLowerCase().contains(search) ||
                                    req.getVehicle().getRegistrationNumber().toLowerCase().contains(search) ||
                                    (req.getVehicle().getCustomer().getUser().getFirstName() + " " +
                                            req.getVehicle().getCustomer().getUser().getLastName()).toLowerCase().contains(search)
                    )
                    .collect(Collectors.toList());
        }

        // Map filtered requests to DTOs
        return filteredRequests.stream()
                .map(this::mapToCompletedServiceDTO)
                .collect(Collectors.toList());
    }

    // Helper methods

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

        // Calculate estimated completion date based on service type and vehicle category
        LocalDate estimatedCompletionDate = calculateEstimatedCompletionDate(request);

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

        // Get completion date from service tracking
        LocalDate completedDate = serviceTrackingRepository.findLastUpdateDateByRequestIdAndStatus(
                        request.getRequestId(), ServiceRequest.Status.Completed)
                .orElse(LocalDate.now());

        // Calculate total cost
        BigDecimal totalCost = calculateTotalCost(request);

        // Check if invoice exists
        boolean hasInvoice = invoiceRepository.existsByRequestId(request.getRequestId());

        return CompletedServiceDTO.builder()
                .serviceId(request.getRequestId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .customerName(customerUser.getFirstName() + " " + customerUser.getLastName())
                .completedDate(completedDate)
                .serviceAdvisorName(serviceAdvisorName)
                .totalCost(totalCost)
                .hasInvoice(hasInvoice)
                .build();
    }

    private LocalDate calculateEstimatedCompletionDate(ServiceRequest request) {
        // If delivery date is set, use that
        if (request.getDeliveryDate() != null) {
            return request.getDeliveryDate();
        }

        // Otherwise calculate based on service type and vehicle category
        LocalDate startDate = request.getCreatedAt().toLocalDate();
        int daysToAdd = 1; // Default to 1 day

        // Adjust based on service type
        String serviceType = request.getServiceType();
        if (serviceType != null) {
            switch (serviceType) {
                case "Oil Change":
                case "Tire Rotation":
                    daysToAdd = 1;
                    break;
                case "Brake Service":
                case "Battery Replacement":
                    daysToAdd = 2;
                    break;
                case "Engine Repair":
                case "Transmission Service":
                    daysToAdd = 5;
                    break;
                case "Regular Maintenance":
                    daysToAdd = 3;
                    break;
                default:
                    daysToAdd = 2;
            }
        }

        // Adjust based on vehicle category
        if (request.getVehicle() != null && request.getVehicle().getCategory() != null) {
            switch (request.getVehicle().getCategory()) {
                case Bike:
                    daysToAdd = Math.max(1, daysToAdd - 1);
                    break;
                case Car:
                    // No adjustment
                    break;
                case Truck:
                    daysToAdd += 1;
                    break;
            }
        }

        return startDate.plusDays(daysToAdd);
    }

    private BigDecimal calculateTotalCost(ServiceRequest request) {
        if (request == null) {
            return BigDecimal.ZERO;
        }

        // Get labor cost from service tracking
        BigDecimal laborCost = serviceTrackingRepository.findTotalLaborCostByRequestId(request.getRequestId())
                .orElse(BigDecimal.ZERO);

        // Get material cost
        BigDecimal materialCost = calculateMaterialCost(request.getRequestId());

        // Calculate base service fee based on service type
        BigDecimal baseServiceFee = getServiceBaseCost(request.getServiceType());

        // Apply vehicle category multiplier
        BigDecimal categoryMultiplier = BigDecimal.ONE; // default
        if (request.getVehicle() != null && request.getVehicle().getCategory() != null) {
            switch (request.getVehicle().getCategory()) {
                case Car:
                    categoryMultiplier = new BigDecimal("1.2");
                    break;
                case Truck:
                    categoryMultiplier = new BigDecimal("1.5");
                    break;
                case Bike:
                    categoryMultiplier = new BigDecimal("0.8");
                    break;
            }
        }

        // Apply premium customer discount if applicable
        BigDecimal membershipMultiplier = BigDecimal.ONE;
        if (request.getVehicle() != null &&
                request.getVehicle().getCustomer() != null &&
                "Premium".equals(request.getVehicle().getCustomer().getMembershipStatus())) {
            membershipMultiplier = new BigDecimal("0.9"); // 10% discount for premium members
        }

        // Calculate total cost
        BigDecimal adjustedServiceFee = baseServiceFee.multiply(categoryMultiplier).multiply(membershipMultiplier);
        BigDecimal totalCost = adjustedServiceFee.add(laborCost).add(materialCost);

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMaterialCost(Integer requestId) {
        List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(requestId);

        BigDecimal totalCost = materials.stream()
                .map(material -> {
                    BigDecimal quantity = material.getQuantity();
                    BigDecimal unitPrice = material.getInventoryItem().getUnitPrice();
                    return quantity.multiply(unitPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getServiceBaseCost(String serviceType) {
        if (serviceType == null) return new BigDecimal("5000.00");

        // Map common service types to base costs
        switch (serviceType) {
            case "Oil Change":
                return new BigDecimal("2000.00");
            case "Brake Service":
                return new BigDecimal("5000.00");
            case "Tire Rotation":
                return new BigDecimal("1500.00");
            case "Engine Repair":
                return new BigDecimal("15000.00");
            case "Transmission Service":
                return new BigDecimal("10000.00");
            case "Regular Maintenance":
                return new BigDecimal("3500.00");
            case "Battery Replacement":
                return new BigDecimal("4000.00");
            case "Diagnostics":
                return new BigDecimal("2500.00");
            default:
                return new BigDecimal("5000.00");
        }
    }
}