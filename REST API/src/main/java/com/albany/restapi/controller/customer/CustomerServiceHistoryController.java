package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/service-history")
public class CustomerServiceHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceHistoryController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final ServiceRequestRepository serviceRequestRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final CustomerRepository customerRepository;

    public CustomerServiceHistoryController(
            ServiceRequestRepository serviceRequestRepository,
            MaterialUsageRepository materialUsageRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            ServiceTrackingRepository serviceTrackingRepository,
            CustomerRepository customerRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.materialUsageRepository = materialUsageRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.serviceTrackingRepository = serviceTrackingRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Get completed service history for the current customer
     */
    @GetMapping
    public ResponseEntity<?> getServiceHistory(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Fetching service history for user: {}", user.getEmail());
            
            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to access customer service history", user.getEmail());
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            // Find all completed service requests for this user
            List<ServiceRequest> completedRequests = serviceRequestRepository.findAll().stream()
                    .filter(req -> req.getUserId().equals(user.getUserId().longValue()))
                    .filter(req -> req.getStatus() == ServiceRequest.Status.Completed)
                    .sorted(Comparator.comparing(ServiceRequest::getUpdatedAt).reversed())
                    .collect(Collectors.toList());
            
            logger.info("Found {} completed service requests", completedRequests.size());
            
            List<CompletedServiceDTO> serviceDTOs = new ArrayList<>();
            for (ServiceRequest request : completedRequests) {
                try {
                    CompletedServiceDTO dto = convertToCompletedServiceDTOWithExtras(request);
                    serviceDTOs.add(dto);
                } catch (Exception e) {
                    logger.error("Error processing service request {}: {}", request.getRequestId(), e.getMessage());
                    // Continue with next request
                }
            }
            
            logger.info("Returning {} service history records", serviceDTOs.size());
            return ResponseEntity.ok(serviceDTOs);
        } catch (Exception e) {
            logger.error("Error fetching service history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Get service details for a specific completed service
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getServiceDetails(@PathVariable Integer requestId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Fetching service details for request: {}, user: {}", requestId, user.getEmail());
            
            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to access customer service details", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }
            
            // Find the service request
            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                logger.warn("Service request not found: {}", requestId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request not found"
                ));
            }
            
            ServiceRequest request = requestOpt.get();
            
            // Verify that the request belongs to this user
            if (!request.getUserId().equals(user.getUserId().longValue())) {
                logger.warn("Service request {} does not belong to user {}", requestId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request does not belong to this customer"
                ));
            }
            
            // Get customer profile - safely with List instead of expecting unique result
            List<CustomerProfile> customerProfiles = customerRepository.findAllByUser_UserId(user.getUserId());
            CustomerProfile customerProfile = customerProfiles.isEmpty() ? null : customerProfiles.get(0);
            
            // Create detailed service DTO
            CompletedServiceDTO serviceDTO = convertToDetailedServiceDTO(request, user, customerProfile);
            
            // Get materials used with full details
            List<MaterialUsage> materials = materialUsageRepository
                    .findByServiceRequest_RequestIdOrderByUsedAtDesc(request.getRequestId());
            
            List<MaterialItemDTO> materialDTOs = materials.stream()
                    .map(this::convertToDetailedMaterialItemDTO)
                    .collect(Collectors.toList());
            
            serviceDTO.setMaterials(materialDTOs);
            
            // Get service tracking records to extract labor charges
            List<ServiceTracking> trackingRecords = serviceTrackingRepository
                    .findByServiceRequest_RequestIdOrderByUpdatedAtDesc(request.getRequestId());
            
            // Extract labor details from tracking records
            List<LaborChargeDTO> laborCharges = extractLaborCharges(trackingRecords);
            serviceDTO.setLaborCharges(laborCharges);
            
            // Calculate financial details
            calculateFinancialDetails(serviceDTO, user);
            
            // Add invoice information if available - using findAllByRequestId instead
            List<Invoice> invoices = invoiceRepository.findAllByRequestId(request.getRequestId());
            if (!invoices.isEmpty()) {
                Invoice invoice = invoices.get(0);
                serviceDTO.setHasInvoice(true);
                serviceDTO.setInvoiceId(invoice.getInvoiceId());
                serviceDTO.setTotalAmount(invoice.getTotalAmount());
                
                // Check if payment exists - using findAllByRequestId instead
                List<Payment> payments = paymentRepository.findAllByRequestId(request.getRequestId());
                boolean isPaid = !payments.isEmpty() && payments.stream()
                    .anyMatch(p -> p.getStatus() == Payment.Status.Completed);
                serviceDTO.setPaid(isPaid);
            }
            
            logger.info("Service details fetched successfully for request: {}", requestId);
            return ResponseEntity.ok(serviceDTO);
        } catch (Exception e) {
            logger.error("Error fetching service details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Convert ServiceRequest to CompletedServiceDTO with materials and invoice info
     */
    private CompletedServiceDTO convertToCompletedServiceDTOWithExtras(ServiceRequest request) {
        CompletedServiceDTO dto = convertToCompletedServiceDTO(request);
        
        // Check if invoice exists - using findAllByRequestId instead
        List<Invoice> invoices = invoiceRepository.findAllByRequestId(request.getRequestId());
        if (!invoices.isEmpty()) {
            Invoice invoice = invoices.get(0);
            dto.setHasInvoice(true);
            dto.setInvoiceId(invoice.getInvoiceId());
            dto.setTotalAmount(invoice.getTotalAmount());
            
            // Check if payment exists - using findAllByRequestId instead
            List<Payment> payments = paymentRepository.findAllByRequestId(request.getRequestId());
            boolean isPaid = !payments.isEmpty() && payments.stream()
                .anyMatch(p -> p.getStatus() == Payment.Status.Completed);
            dto.setPaid(isPaid);
        }
        
        // Get materials used (just basic info for list view)
        List<MaterialUsage> materials = materialUsageRepository
                .findByServiceRequest_RequestIdOrderByUsedAtDesc(request.getRequestId());
        
        List<MaterialItemDTO> materialDTOs = materials.stream()
                .map(this::convertToBasicMaterialItemDTO)
                .collect(Collectors.toList());
        
        dto.setMaterials(materialDTOs);
        
        return dto;
    }

    /**
     * Convert ServiceRequest to basic CompletedServiceDTO (for list view)
     */
    private CompletedServiceDTO convertToCompletedServiceDTO(ServiceRequest request) {
        CompletedServiceDTO dto = CompletedServiceDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(request.getVehicleBrand() + " " + request.getVehicleModel())
                .vehicleBrand(request.getVehicleBrand())
                .vehicleModel(request.getVehicleModel())
                .registrationNumber(request.getVehicleRegistration())
                .vehicleType(request.getVehicleType())
                .serviceType(request.getServiceType())
                .completedDate(request.getUpdatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
        
        if (request.getUpdatedAt() != null) {
            dto.setFormattedCompletedDate(request.getUpdatedAt().format(DATE_FORMATTER));
        }
        
        // Get service advisor info if available
        if (request.getServiceAdvisor() != null && request.getServiceAdvisor().getUser() != null) {
            dto.setServiceAdvisorName(request.getServiceAdvisor().getUser().getFirstName() + " " + 
                                     request.getServiceAdvisor().getUser().getLastName());
        }
        
        return dto;
    }
    
    /**
     * Convert ServiceRequest to detailed CompletedServiceDTO (for detail view)
     */
    private CompletedServiceDTO convertToDetailedServiceDTO(ServiceRequest request, User user, CustomerProfile customerProfile) {
        CompletedServiceDTO dto = convertToCompletedServiceDTO(request);
        
        // Add customer details
        dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
        dto.setCustomerEmail(user.getEmail());
        dto.setCustomerPhone(user.getPhoneNumber());
        
        if (customerProfile != null) {
            dto.setCustomerId(customerProfile.getCustomerId());
            dto.setMembershipStatus(customerProfile.getMembershipStatus());
        } else {
            dto.setMembershipStatus(user.getMembershipType().name());
        }
        
        // Add vehicle year
        dto.setVehicleYear(request.getVehicleYear());
        
        // Add more service details
        dto.setServiceDescription(request.getServiceDescription());
        dto.setAdditionalDescription(request.getAdditionalDescription());
        
        return dto;
    }

    /**
     * Convert MaterialUsage to basic MaterialItemDTO (for list view)
     */
    private MaterialItemDTO convertToBasicMaterialItemDTO(MaterialUsage usage) {
        if (usage.getInventoryItem() == null) {
            return MaterialItemDTO.builder()
                    .itemId(usage.getMaterialUsageId())
                    .name("Unknown Item")
                    .quantity(usage.getQuantity())
                    .build();
        }
        
        return MaterialItemDTO.builder()
                .itemId(usage.getInventoryItem().getItemId())
                .name(usage.getInventoryItem().getName())
                .quantity(usage.getQuantity())
                .build();
    }

    /**
     * Convert MaterialUsage to detailed MaterialItemDTO with all information
     */
    private MaterialItemDTO convertToDetailedMaterialItemDTO(MaterialUsage usage) {
        if (usage.getInventoryItem() == null) {
            // Handle case where inventory item might be null
            return MaterialItemDTO.builder()
                    .itemId(usage.getMaterialUsageId())
                    .name("Unknown Item")
                    .quantity(usage.getQuantity())
                    .unitPrice(BigDecimal.ZERO)
                    .build();
        }
        
        return MaterialItemDTO.builder()
                .itemId(usage.getInventoryItem().getItemId())
                .name(usage.getInventoryItem().getName())
                .quantity(usage.getQuantity())
                .unitPrice(usage.getInventoryItem().getUnitPrice())
                .build();
    }

    /**
     * Extract labor charges from tracking records
     */
    private List<LaborChargeDTO> extractLaborCharges(List<ServiceTracking> trackingRecords) {
        List<LaborChargeDTO> charges = new ArrayList<>();
        
        // If no tracking records with labor info, create a default one
        if (trackingRecords.isEmpty() || trackingRecords.stream()
                .noneMatch(t -> t.getLaborMinutes() != null && t.getLaborMinutes() > 0)) {
            charges.add(LaborChargeDTO.builder()
                    .description("Service Labor")
                    .hours(BigDecimal.valueOf(2)) // Default 2 hours
                    .rate(BigDecimal.valueOf(500)) // Default rate ₹500/hr
                    .build());
            return charges;
        }
        
        // Extract labor details from tracking records
        for (ServiceTracking tracking : trackingRecords) {
            if (tracking.getLaborMinutes() != null && tracking.getLaborMinutes() > 0) {
                // Convert minutes to hours
                BigDecimal hours = BigDecimal.valueOf(tracking.getLaborMinutes())
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                
                // Calculate rate if available, otherwise use default
                BigDecimal rate = BigDecimal.valueOf(500); // Default rate
                if (tracking.getLaborCost() != null && hours.compareTo(BigDecimal.ZERO) > 0) {
                    rate = tracking.getLaborCost().divide(hours, 2, RoundingMode.HALF_UP);
                }
                
                String description = "Service Labor";
                if (tracking.getWorkDescription() != null && !tracking.getWorkDescription().isEmpty()) {
                    description = tracking.getWorkDescription();
                }
                
                charges.add(LaborChargeDTO.builder()
                        .description(description)
                        .hours(hours)
                        .rate(rate)
                        .build());
            }
        }
        
        // If no valid labor charges found, add a default one
        if (charges.isEmpty()) {
            charges.add(LaborChargeDTO.builder()
                    .description("Service Labor")
                    .hours(BigDecimal.valueOf(2)) // Default 2 hours
                    .rate(BigDecimal.valueOf(500)) // Default rate ₹500/hr
                    .build());
        }
        
        return charges;
    }

    /**
     * Calculate all financial details for the service
     */
    private void calculateFinancialDetails(CompletedServiceDTO serviceDTO, User user) {
        // Calculate materials total
        BigDecimal materialsTotal = serviceDTO.getMaterials().stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    if (item.getQuantity() == null || item.getUnitPrice() == null) return BigDecimal.ZERO;
                    return item.getQuantity().multiply(item.getUnitPrice());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        serviceDTO.setCalculatedMaterialsTotal(materialsTotal);
        
        // Calculate labor total
        BigDecimal laborTotal = serviceDTO.getLaborCharges().stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    if (item.getHours() == null || item.getRate() == null) return BigDecimal.ZERO;
                    return item.getHours().multiply(item.getRate());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        serviceDTO.setCalculatedLaborTotal(laborTotal);
        
        // REMOVED: Don't apply premium discount here as it's already factored into the stored prices
        // Set discount to zero
        serviceDTO.setCalculatedDiscount(BigDecimal.ZERO);
        
        // Calculate subtotal (without discount subtraction)
        BigDecimal subtotal = materialsTotal.add(laborTotal)
                .setScale(2, RoundingMode.HALF_UP);
        serviceDTO.setCalculatedSubtotal(subtotal);
        
        // Calculate tax (18% GST)
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.18))
                .setScale(2, RoundingMode.HALF_UP);
        serviceDTO.setCalculatedTax(tax);
        
        // Calculate total
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        serviceDTO.setCalculatedTotal(total);
    }
}