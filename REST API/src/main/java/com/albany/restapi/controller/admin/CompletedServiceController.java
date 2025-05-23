package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.model.Invoice;
import com.albany.restapi.model.Payment;
import com.albany.restapi.service.admin.CompletedServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class CompletedServiceController {

    private static final Logger logger = LoggerFactory.getLogger(CompletedServiceController.class);
    private final CompletedServiceService completedServiceService;

    /**
     * Get all completed services
     */
    @GetMapping("/completed-services")
    public ResponseEntity<List<CompletedServiceDTO>> getAllCompletedServices() {
        try {
            logger.info("Fetching all completed services");
            List<CompletedServiceDTO> services = completedServiceService.getAllCompletedServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            logger.error("Error fetching completed services: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get completed service by ID
     */
    @GetMapping("/completed-services/{id}")
    public ResponseEntity<CompletedServiceDTO> getCompletedServiceById(@PathVariable("id") Integer requestId) {
        try {
            logger.info("Fetching completed service with ID: {}", requestId);
            CompletedServiceDTO service = completedServiceService.getCompletedServiceById(requestId);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            logger.error("Error fetching completed service {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get invoice details for a completed service
     */
    @GetMapping("/completed-services/{id}/invoice-details")
    public ResponseEntity<CompletedServiceDTO> getInvoiceDetails(@PathVariable("id") Integer requestId) {
        try {
            logger.info("Fetching invoice details for service ID: {}", requestId);
            CompletedServiceDTO invoiceDetails = completedServiceService.getInvoiceDetails(requestId);
            return ResponseEntity.ok(invoiceDetails);
        } catch (Exception e) {
            logger.error("Error fetching invoice details for service {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate invoice for a completed service
     * This endpoint handles sending emails to customers with their invoice
     */
    @PostMapping("/invoices/service-request/{id}/generate")
    public ResponseEntity<?> generateInvoice(
            @PathVariable("id") Integer requestId,
            @Valid @RequestBody Map<String, Object> request) {

        try {
            logger.info("Generating invoice for service ID: {} with request: {}", requestId, request);

            // Extract request parameters
            String emailAddress = (String) request.get("emailAddress");
            boolean sendEmail = Boolean.TRUE.equals(request.get("sendEmail"));
            String notes = (String) request.get("notes");

            // Validate email if sending is requested
            if (sendEmail && (emailAddress == null || emailAddress.trim().isEmpty())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email address is required when send email is enabled"));
            }

            Invoice invoice = completedServiceService.generateInvoice(requestId, emailAddress, sendEmail, notes);

            // Build success response
            Map<String, Object> response = new HashMap<>();
            response.put("invoiceId", invoice.getInvoiceId());
            response.put("message", sendEmail ?
                    "Invoice generated and email sent successfully" :
                    "Invoice generated successfully");
            response.put("emailSent", sendEmail);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            // Handle specific known errors
            logger.warn("Business rule violation when generating invoice: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected errors
            logger.error("Error generating invoice for service {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate invoice: " + e.getMessage()));
        }
    }

    /**
     * Process payment for a completed service
     */
    @PostMapping("/completed-services/{id}/payment")
    public ResponseEntity<?> processPayment(
            @PathVariable("id") Integer requestId,
            @Valid @RequestBody Map<String, Object> request) {

        try {
            logger.info("Processing payment for service ID: {} with request: {}", requestId, request);

            // Extract request parameters
            String paymentMethod = (String) request.get("paymentMethod");
            String transactionId = (String) request.get("transactionId");

            // Validate required fields
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Payment method is required"));
            }

            // Parse amount with validation
            BigDecimal amount;
            try {
                amount = new BigDecimal(request.get("amount").toString());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Amount must be greater than zero");
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Valid payment amount is required"));
            }

            Payment payment = completedServiceService.processPayment(requestId, paymentMethod, transactionId, amount);

            // Build success response
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getPaymentId());
            response.put("message", "Payment processed successfully");
            response.put("status", payment.getStatus().name());
            response.put("timestamp", payment.getPaymentTimestamp());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            // Handle specific known errors
            logger.warn("Business rule violation when processing payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected errors
            logger.error("Error processing payment for service {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process payment: " + e.getMessage()));
        }
    }

    /**
     * Mark a service as delivered
     */
    @PostMapping("/completed-services/{id}/dispatch")
    public ResponseEntity<?> markAsDelivered(
            @PathVariable("id") Integer requestId,
            @RequestBody Map<String, Object> request) {

        try {
            logger.info("Marking service ID: {} as delivered with request: {}", requestId, request);

            String deliveryType = (String) request.get("deliveryType");
            if (deliveryType == null || deliveryType.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Delivery type is required"));
            }

            // Create a new Map<String, String> instead of casting
            Map<String, String> deliveryDetails = new HashMap<>();
            // Copy relevant entries from the request map, converting to strings as needed
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (entry.getValue() != null && !entry.getKey().equals("deliveryType")) {
                    deliveryDetails.put(entry.getKey(), entry.getValue().toString());
                }
            }

            completedServiceService.markAsDelivered(requestId, deliveryType, deliveryDetails);

            return ResponseEntity.ok(Map.of(
                    "message", "Service marked as delivered successfully",
                    "deliveryType", deliveryType
            ));
        } catch (Exception e) {
            logger.error("Error marking service {} as delivered: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark service as delivered: " + e.getMessage()));
        }
    }

    /**
     * Download invoice for a completed service
     */
    @GetMapping("/completed-services/{id}/invoice/download")
    public ResponseEntity<?> downloadInvoice(@PathVariable("id") Integer requestId) {
        try {
            logger.info("Request to download invoice for service ID: {}", requestId);

            // In a real application, this would generate a PDF invoice
            // For now, we'll just return a success message
            return ResponseEntity.ok(Map.of("message", "Invoice downloaded successfully"));
        } catch (Exception e) {
            logger.error("Error downloading invoice for service {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download invoice: " + e.getMessage()));
        }
    }
}