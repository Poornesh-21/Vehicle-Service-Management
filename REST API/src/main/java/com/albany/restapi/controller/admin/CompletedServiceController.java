package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.model.Invoice;
import com.albany.restapi.model.Payment;
import com.albany.restapi.service.admin.CompletedServiceService;
import lombok.RequiredArgsConstructor;
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

    private final CompletedServiceService completedServiceService;

    /**
     * Get all completed services
     */
    @GetMapping("/completed-services")
    public ResponseEntity<List<CompletedServiceDTO>> getAllCompletedServices() {
        try {
            List<CompletedServiceDTO> services = completedServiceService.getAllCompletedServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get completed service by ID
     */
    @GetMapping("/completed-services/{id}")
    public ResponseEntity<CompletedServiceDTO> getCompletedServiceById(@PathVariable("id") Integer requestId) {
        try {
            CompletedServiceDTO service = completedServiceService.getCompletedServiceById(requestId);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get invoice details for a completed service
     */
    @GetMapping("/completed-services/{id}/invoice-details")
    public ResponseEntity<CompletedServiceDTO> getInvoiceDetails(@PathVariable("id") Integer requestId) {
        try {
            CompletedServiceDTO invoiceDetails = completedServiceService.getInvoiceDetails(requestId);
            return ResponseEntity.ok(invoiceDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate invoice for a completed service
     */
    @PostMapping("/invoices/service-request/{id}/generate")
    public ResponseEntity<?> generateInvoice(
            @PathVariable("id") Integer requestId,
            @RequestBody Map<String, Object> request) {

        try {
            String emailAddress = (String) request.get("emailAddress");
            boolean sendEmail = Boolean.TRUE.equals(request.get("sendEmail"));
            String notes = (String) request.get("notes");

            Invoice invoice = completedServiceService.generateInvoice(requestId, emailAddress, sendEmail, notes);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "invoiceId", invoice.getInvoiceId(),
                    "message", "Invoice generated successfully"
            ));
        } catch (Exception e) {
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
            @RequestBody Map<String, Object> request) {

        try {
            String paymentMethod = (String) request.get("paymentMethod");
            String transactionId = (String) request.get("transactionId");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            Payment payment = completedServiceService.processPayment(requestId, paymentMethod, transactionId, amount);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "paymentId", payment.getPaymentId(),
                    "message", "Payment processed successfully"
            ));
        } catch (Exception e) {
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
            String deliveryType = (String) request.get("deliveryType");

            // Create a new Map<String, String> instead of casting
            Map<String, String> deliveryDetails = new HashMap<>();
            // Copy relevant entries from the request map, converting to strings as needed
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (entry.getValue() != null && !entry.getKey().equals("deliveryType")) {
                    deliveryDetails.put(entry.getKey(), entry.getValue().toString());
                }
            }

            completedServiceService.markAsDelivered(requestId, deliveryType, deliveryDetails);

            return ResponseEntity.ok(Map.of("message", "Service marked as delivered successfully"));
        } catch (Exception e) {
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
            // In a real application, this would generate a PDF invoice
            // For now, we'll just return a success message
            return ResponseEntity.ok(Map.of("message", "Invoice downloaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download invoice: " + e.getMessage()));
        }
    }
}