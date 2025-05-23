package com.albany.restapi.service.admin;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.exception.ServiceRequestExceptions.ServiceRequestNotFoundException;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.albany.restapi.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompletedServiceService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompletedServiceService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    
    /**
     * Get all completed services
     */
    public List<CompletedServiceDTO> getAllCompletedServices() {
        List<ServiceRequest> completedRequests = serviceRequestRepository.findByStatusOrderByUpdatedAtDesc(ServiceRequest.Status.Completed);
        return completedRequests.stream()
                .map(this::mapToCompletedServiceDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get completed service by ID
     */
    public CompletedServiceDTO getCompletedServiceById(Integer requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException(requestId));
                
        if (request.getStatus() != ServiceRequest.Status.Completed) {
            throw new IllegalStateException("Service request is not in completed status");
        }
        
        return mapToCompletedServiceDTO(request);
    }
    
    /**
     * Get invoice details for a completed service
     */
    public CompletedServiceDTO getInvoiceDetails(Integer requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException(requestId));
                
        CompletedServiceDTO dto = mapToCompletedServiceDTO(request, false);
        return calculateServiceTotals(dto, request);
    }
    
    /**
     * Generate invoice for a completed service
     */
    @Transactional
    public Invoice generateInvoice(Integer requestId, String emailAddress, boolean sendEmail, String notes) {
        logger.info("Generating invoice for service ID: {}, Email: {}, SendEmail: {}", requestId, emailAddress, sendEmail);
        
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException(requestId));
                
        if (request.getStatus() != ServiceRequest.Status.Completed) {
            throw new IllegalStateException("Cannot generate invoice for a service that is not completed");
        }
        
        // Check if invoice already exists
        Optional<Invoice> existingInvoiceOpt = invoiceRepository.findByRequestId(requestId);
        if (existingInvoiceOpt.isPresent()) {
            Invoice existingInvoice = existingInvoiceOpt.get();
            logger.info("Invoice already exists with ID: {}", existingInvoice.getInvoiceId());
            
            // If sending email is requested and we have a valid email address
            if (sendEmail && isValidEmail(emailAddress)) {
                sendInvoiceEmail(request, existingInvoice, emailAddress);
            }
            
            return existingInvoice;
        }
        
        // Get invoice details
        CompletedServiceDTO serviceDetails = getInvoiceDetails(requestId);
        
        // Create new invoice
        Invoice invoice = Invoice.builder()
                .requestId(requestId)
                .invoiceDate(LocalDateTime.now())
                .netAmount(serviceDetails.getCalculatedSubtotal())
                .taxes(serviceDetails.getCalculatedTax())
                .totalAmount(serviceDetails.getCalculatedTotal())
                .isDownloadable(true)
                .build();
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice generated successfully with ID: {}", savedInvoice.getInvoiceId());
        
        // Send email if requested and we have a valid email
        if (sendEmail && isValidEmail(emailAddress)) {
            sendInvoiceEmail(request, savedInvoice, emailAddress);
        } else if (sendEmail) {
            logger.warn("Email sending requested but no valid email provided for service ID: {}", requestId);
        }
        
        return savedInvoice;
    }
    
    /**
     * Helper method to validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Helper method to send invoice email
     */
    private void sendInvoiceEmail(ServiceRequest request, Invoice invoice, String emailAddress) {
        try {
            CompletedServiceDTO serviceDTO = getInvoiceDetails(request.getRequestId());
            
            // Get customer name from service request
            String customerName = "Valued Customer";
            if (request.getUserId() != null) {
                User user = userRepository.findById(request.getUserId().intValue()).orElse(null);
                if (user != null) {
                    customerName = user.getFirstName() + " " + user.getLastName();
                }
            }
            
            // If the email address doesn't match user's email, use the provided one
            emailService.sendInvoiceEmail(emailAddress, customerName, serviceDTO, invoice);
            logger.info("Invoice email sent successfully to: {}", emailAddress);
        } catch (Exception e) {
            logger.error("Failed to send invoice email: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process payment for a completed service
     */
    @Transactional
    public Payment processPayment(Integer requestId, String paymentMethod, String transactionId, BigDecimal amount) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException(requestId));
                
        // Check if payment already exists
        Optional<Payment> existingPayment = paymentRepository.findByRequestId(requestId);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == Payment.Status.Completed) {
            throw new IllegalStateException("Payment has already been processed for this service");
        }
        
        // Get customer ID
        Integer customerId = null;
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId().intValue())
                    .orElse(null);
            if (user != null) {
                Optional<CustomerProfile> customerProfile = customerRepository.findByUser_UserId(user.getUserId());
                if (customerProfile.isPresent()) {
                    customerId = customerProfile.get().getCustomerId();
                }
            }
        }
        
        // Create payment
        Payment payment = Payment.builder()
                .requestId(requestId)
                .customerId(customerId)
                .amount(amount)
                .paymentMethod(Payment.PaymentMethod.valueOf(paymentMethod))
                .transactionId(transactionId)
                .status(Payment.Status.Completed)
                .paymentTimestamp(LocalDateTime.now())
                .build();
        
        return paymentRepository.save(payment);
    }
    
    /**
     * Mark a service as delivered
     */
    @Transactional
    public void markAsDelivered(Integer requestId, String deliveryType, Map<String, String> deliveryDetails) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException(requestId));
                
        // Update service request to mark as delivered
        // In a real application, you might have a separate entity for delivery tracking
        request.setAdditionalDescription(
            (request.getAdditionalDescription() != null ? request.getAdditionalDescription() : "") + 
            "\nDelivered via " + deliveryType + " on " + LocalDateTime.now().format(DATE_FORMATTER) +
            "\nDetails: " + deliveryDetails
        );
        
        serviceRequestRepository.save(request);
    }
    
    /**
     * Map ServiceRequest to CompletedServiceDTO
     */
    private CompletedServiceDTO mapToCompletedServiceDTO(ServiceRequest request) {
        return mapToCompletedServiceDTO(request, true);
    }
    
    private CompletedServiceDTO mapToCompletedServiceDTO(ServiceRequest request, boolean calculateTotals) {
        CompletedServiceDTO dto = CompletedServiceDTO.builder()
                .requestId(request.getRequestId())
                .serviceId(request.getRequestId()) // For backward compatibility
                .vehicleBrand(request.getVehicleBrand())
                .vehicleModel(request.getVehicleModel())
                .vehicleType(request.getVehicleType())
                .registrationNumber(request.getVehicleRegistration())
                .updatedAt(request.getUpdatedAt())
                .completionDate(request.getUpdatedAt()) // Use updatedAt as completion date
                .completedDate(request.getUpdatedAt()) // For backward compatibility
                .build();
                
        // Set vehicle name
        if (request.getVehicleBrand() != null && request.getVehicleModel() != null) {
            dto.setVehicleName(request.getVehicleBrand() + " " + request.getVehicleModel());
        } else if (request.getVehicle() != null) {
            Vehicle vehicle = request.getVehicle();
            dto.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());
            if (dto.getRegistrationNumber() == null) {
                dto.setRegistrationNumber(vehicle.getRegistrationNumber());
            }
        }
        
        // Set category
        if (request.getVehicleType() != null) {
            dto.setCategory(request.getVehicleType());
        } else if (request.getVehicle() != null && request.getVehicle().getCategory() != null) {
            dto.setCategory(request.getVehicle().getCategory().name());
        }
        
        // Set formatted date
        if (request.getUpdatedAt() != null) {
            dto.setFormattedCompletedDate(request.getUpdatedAt().format(DATE_FORMATTER));
        }
        
        // Get customer information
        if (request.getUserId() != null) {
            try {
                User user = userRepository.findById(request.getUserId().intValue())
                        .orElse(null);
                if (user != null) {
                    dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                    dto.setCustomerEmail(user.getEmail());
                    dto.setCustomerPhone(user.getPhoneNumber());
                    dto.setMembershipStatus(user.getMembershipType().name());
                    
                    // Get customer ID
                    Optional<CustomerProfile> customerProfile = customerRepository.findByUser_UserId(user.getUserId());
                    if (customerProfile.isPresent()) {
                        dto.setCustomerId(customerProfile.get().getCustomerId());
                        
                        // Override membership status if available in customer profile
                        if (customerProfile.get().getMembershipStatus() != null) {
                            dto.setMembershipStatus(customerProfile.get().getMembershipStatus());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error retrieving customer information for service request {}: {}", 
                        request.getRequestId(), e.getMessage());
            }
        }
        
        // Check if invoice exists
        Optional<Invoice> invoice = invoiceRepository.findByRequestId(request.getRequestId());
        dto.setHasInvoice(invoice.isPresent());
        invoice.ifPresent(inv -> dto.setInvoiceId(inv.getInvoiceId()));
        
        // Check if payment exists
        Optional<Payment> payment = paymentRepository.findByRequestId(request.getRequestId());
        dto.setPaid(payment.isPresent() && payment.get().getStatus() == Payment.Status.Completed);
        
        // For now, we'll assume delivery status based on additional description
        // In a real application, you might have a separate entity for delivery tracking
        boolean isDelivered = request.getAdditionalDescription() != null && 
                             request.getAdditionalDescription().contains("Delivered via");
        dto.setDelivered(isDelivered);
        
        // If calculateTotals is true, calculate totals here
        if (calculateTotals) {
            return calculateServiceTotals(dto, request);
        }
        
        return dto;
    }
    
    private CompletedServiceDTO calculateServiceTotals(CompletedServiceDTO dto, ServiceRequest request) {
        // Get materials used
        List<MaterialUsage> materialUsages = materialUsageRepository.findByServiceRequest_RequestIdOrderByUsedAtDesc(request.getRequestId());
        List<MaterialItemDTO> materials = materialUsages.stream()
                .map(usage -> {
                    InventoryItem item = usage.getInventoryItem();
                    String name = item != null ? item.getName() : "Unknown Item";
                    BigDecimal unitPrice = item != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal quantity = usage.getQuantity();
                    
                    return MaterialItemDTO.builder()
                            .itemId(item != null ? item.getItemId() : null)
                            .name(name)
                            .quantity(quantity)
                            .unitPrice(unitPrice)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Add default material if no materials exist (for testing)
        if (materials.isEmpty() && "Brake Service".equals(request.getServiceType())) {
            materials.add(MaterialItemDTO.builder()
                    .name("Brake Pad")
                    .quantity(new BigDecimal("3"))
                    .unitPrice(new BigDecimal("1000.00"))
                    .build());
        }
        
        // Calculate materials total
        BigDecimal materialsTotal = materials.stream()
                .map(m -> m.getUnitPrice().multiply(m.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Labor charges 
        List<LaborChargeDTO> laborCharges = new ArrayList<>();
        if (request.getServiceType() != null) {
            LaborChargeDTO laborCharge = LaborChargeDTO.builder()
                    .description("Service: " + request.getServiceType())
                    .hours(BigDecimal.valueOf(2)) // Default to 2 hours
                    .rate(BigDecimal.valueOf(40)) // ₹40 per hour
                    .build();
            laborCharges.add(laborCharge);
        }
        
        BigDecimal laborTotal = laborCharges.stream()
                .map(l -> l.getRate().multiply(l.getHours()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate discount (10% for premium members)
        BigDecimal discount = BigDecimal.ZERO;
        if (dto.getMembershipStatus() != null && 
            (dto.getMembershipStatus().equalsIgnoreCase("Premium") || 
             dto.getMembershipStatus().equalsIgnoreCase("PREMIUM"))) {
            discount = materialsTotal.add(laborTotal).multiply(BigDecimal.valueOf(0.1))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        BigDecimal subtotal = materialsTotal.add(laborTotal).subtract(discount);
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP); // 18% GST
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        
        dto.setMaterials(materials);
        dto.setLaborCharges(laborCharges);
        dto.setCalculatedMaterialsTotal(materialsTotal);
        dto.setCalculatedLaborTotal(laborTotal);
        dto.setCalculatedDiscount(discount);
        dto.setCalculatedSubtotal(subtotal);
        dto.setCalculatedTax(tax);
        dto.setCalculatedTotal(total);
        
        // Also set the total in multiple fields for backwards compatibility
        dto.setTotalAmount(total);
        dto.setTotalCost(total);
        
        return dto;
    }
}