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

    public CustomerServiceHistoryController(ServiceRequestRepository serviceRequestRepository,
                                           MaterialUsageRepository materialUsageRepository,
                                           InvoiceRepository invoiceRepository,
                                           PaymentRepository paymentRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.materialUsageRepository = materialUsageRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Get completed service history for the current customer
     */
    @GetMapping
    public ResponseEntity<?> getServiceHistory(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Find all completed service requests for this user
            List<ServiceRequest> completedRequests = serviceRequestRepository.findAll().stream()
                    .filter(req -> req.getUserId().equals(user.getUserId().longValue()))
                    .filter(req -> req.getStatus() == ServiceRequest.Status.Completed)
                    .sorted(Comparator.comparing(ServiceRequest::getUpdatedAt).reversed())
                    .collect(Collectors.toList());
            
            List<CompletedServiceDTO> serviceDTOs = new ArrayList<>();
            
            for (ServiceRequest request : completedRequests) {
                CompletedServiceDTO dto = convertToCompletedServiceDTO(request);
                
                // Check if invoice exists
                Optional<Invoice> invoiceOpt = invoiceRepository.findByRequestId(request.getRequestId());
                if (invoiceOpt.isPresent()) {
                    Invoice invoice = invoiceOpt.get();
                    dto.setHasInvoice(true);
                    dto.setInvoiceId(invoice.getInvoiceId());
                    dto.setTotalAmount(invoice.getTotalAmount());
                    
                    // Check if payment exists
                    Optional<Payment> paymentOpt = paymentRepository.findByRequestId(request.getRequestId());
                    dto.setPaid(paymentOpt.isPresent() && 
                                paymentOpt.get().getStatus() == Payment.Status.Completed);
                }
                
                // Get materials used
                List<MaterialUsage> materials = materialUsageRepository
                        .findByServiceRequest_RequestIdOrderByUsedAtDesc(request.getRequestId());
                
                List<MaterialItemDTO> materialDTOs = materials.stream()
                        .map(this::convertToMaterialItemDTO)
                        .collect(Collectors.toList());
                
                dto.setMaterials(materialDTOs);
                
                serviceDTOs.add(dto);
            }
            
            return ResponseEntity.ok(serviceDTOs);
        } catch (Exception e) {
            logger.error("Error fetching service history: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Convert ServiceRequest to CompletedServiceDTO
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
        
        // Get user and service advisor info
        if (request.getServiceAdvisor() != null) {
            dto.setServiceAdvisorName(request.getServiceAdvisor().getUser().getFirstName() + " " + 
                                     request.getServiceAdvisor().getUser().getLastName());
        }
        
        return dto;
    }

    /**
     * Convert MaterialUsage to MaterialItemDTO
     */
    private MaterialItemDTO convertToMaterialItemDTO(MaterialUsage usage) {
        return MaterialItemDTO.builder()
                .itemId(usage.getInventoryItem().getItemId())
                .name(usage.getInventoryItem().getName())
                .quantity(usage.getQuantity())
                .unitPrice(usage.getInventoryItem().getUnitPrice())
                .build();
    }

    /**
     * Get service details for a specific completed service
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getServiceDetails(@PathVariable Integer requestId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Find the service request
            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request not found"
                ));
            }
            
            ServiceRequest request = requestOpt.get();
            
            // Verify that the request belongs to this user
            if (!request.getUserId().equals(user.getUserId().longValue())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request does not belong to this customer"
                ));
            }
            
            // Get complete service details
            CompletedServiceDTO serviceDTO = convertToCompletedServiceDTO(request);
            
            // Get materials used
            List<MaterialUsage> materials = materialUsageRepository
                    .findByServiceRequest_RequestIdOrderByUsedAtDesc(request.getRequestId());
            
            List<MaterialItemDTO> materialDTOs = materials.stream()
                    .map(this::convertToMaterialItemDTO)
                    .collect(Collectors.toList());
            
            serviceDTO.setMaterials(materialDTOs);
            
            // Calculate materials total
            BigDecimal materialsTotal = materialDTOs.stream()
                    .map(item -> item.getQuantity().multiply(item.getUnitPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            serviceDTO.setCalculatedMaterialsTotal(materialsTotal);
            
            // Add labor charges (simulated based on tracking records)
            List<LaborChargeDTO> laborCharges = new ArrayList<>();
            laborCharges.add(LaborChargeDTO.builder()
                    .description("Service Labor")
                    .hours(BigDecimal.valueOf(2))
                    .rate(BigDecimal.valueOf(500))
                    .build());
            
            serviceDTO.setLaborCharges(laborCharges);
            
            // Calculate labor total
            BigDecimal laborTotal = laborCharges.stream()
                    .map(item -> item.getHours().multiply(item.getRate()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            serviceDTO.setCalculatedLaborTotal(laborTotal);
            
            // Apply discount for premium members
            BigDecimal discount = BigDecimal.ZERO;
            if (user.getMembershipType() == User.MembershipType.PREMIUM) {
                // 30% discount on labor
                discount = laborTotal.multiply(BigDecimal.valueOf(0.3));
            }
            
            serviceDTO.setCalculatedDiscount(discount);
            
            // Calculate subtotal
            BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
            serviceDTO.setCalculatedSubtotal(subtotal);
            
            // Calculate tax (18% GST)
            BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.18));
            serviceDTO.setCalculatedTax(tax);
            
            // Calculate total
            BigDecimal total = subtotal.add(tax);
            serviceDTO.setCalculatedTotal(total);
            
            // Check if invoice exists
            Optional<Invoice> invoiceOpt = invoiceRepository.findByRequestId(request.getRequestId());
            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();
                serviceDTO.setHasInvoice(true);
                serviceDTO.setInvoiceId(invoice.getInvoiceId());
                serviceDTO.setTotalAmount(invoice.getTotalAmount());
                
                // Check if payment exists
                Optional<Payment> paymentOpt = paymentRepository.findByRequestId(request.getRequestId());
                serviceDTO.setPaid(paymentOpt.isPresent() && 
                                  paymentOpt.get().getStatus() == Payment.Status.Completed);
            }
            
            return ResponseEntity.ok(serviceDTO);
        } catch (Exception e) {
            logger.error("Error fetching service details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }
}