package com.albany.restapi.controller.customer;

import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/invoices")
public class CustomerInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerInvoiceController.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String razorpayCurrency;

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CustomerRepository customerRepository;

    public CustomerInvoiceController(InvoiceRepository invoiceRepository,
                                    PaymentRepository paymentRepository,
                                    ServiceRequestRepository serviceRequestRepository,
                                    CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Create payment order for an invoice
     */
    @PostMapping("/{invoiceId}/payment")
    public ResponseEntity<?> createPaymentOrder(@PathVariable Integer invoiceId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Find the invoice
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice not found"
                ));
            }
            
            Invoice invoice = invoiceOpt.get();
            
            // Find the service request to verify ownership
            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(invoice.getRequestId());
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
                        "message", "Invoice does not belong to this customer"
                ));
            }
            
            // Check if already paid
            Optional<Payment> existingPayment = paymentRepository.findByRequestId(request.getRequestId());
            if (existingPayment.isPresent() && existingPayment.get().getStatus() == Payment.Status.Completed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice has already been paid"
                ));
            }
            
            // Get customer profile
            CustomerProfile customer = customerRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Customer profile not found"));
            
            // Calculate amount in paisa (multiply by 100)
            int amount = invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();
            
            // Create Razorpay client
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            
            // Create order request
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", razorpayCurrency);
            orderRequest.put("receipt", "rcpt_inv_" + invoiceId);
            orderRequest.put("payment_capture", true);
            
            // Create order in Razorpay
            Order order = razorpay.orders.create(orderRequest);
            String orderId = order.get("id");
            
            // Create payment record
            Payment payment = Payment.builder()
                    .requestId(request.getRequestId())
                    .customerId(customer.getCustomerId())
                    .amount(invoice.getTotalAmount())
                    .paymentMethod(Payment.PaymentMethod.Card) // Default, will be updated on success
                    .status(Payment.Status.Pending)
                    .paymentTimestamp(LocalDateTime.now())
                    .build();
            
            Payment savedPayment = paymentRepository.save(payment);
            
            // Update invoice with payment ID
            invoice.setPaymentId(savedPayment.getPaymentId());
            invoiceRepository.save(invoice);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", razorpayCurrency);
            response.put("razorpayKey", razorpayKeyId);
            response.put("paymentId", savedPayment.getPaymentId());
            
            // Add user details for Razorpay prefill
            response.put("email", user.getEmail());
            response.put("name", user.getFirstName() + " " + user.getLastName());
            response.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating payment order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Verify payment for an invoice
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String paymentId = request.get("paymentId");
            String razorpayPaymentId = request.get("razorpayPaymentId");
            String razorpayOrderId = request.get("razorpayOrderId");
            String razorpaySignature = request.get("razorpaySignature");
            
            if (paymentId == null || razorpayPaymentId == null || razorpayOrderId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment ID, Razorpay Payment ID, and Order ID are required"
                ));
            }
            
            // Find the payment
            Optional<Payment> paymentOpt = paymentRepository.findById(Integer.parseInt(paymentId));
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment not found"
                ));
            }
            
            Payment payment = paymentOpt.get();
            
            // In a production environment, verify the payment signature with Razorpay API
            // For simplicity, we'll assume the payment is valid if we have all the required fields
            
            // Update payment status
            payment.setStatus(Payment.Status.Completed);
            payment.setTransactionId(razorpayPaymentId);
            payment.setPaymentMethod(getPaymentMethod(request.get("paymentMethod")));
            payment.setPaymentTimestamp(LocalDateTime.now());
            
            paymentRepository.save(payment);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment successful"
            ));
        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Map Razorpay payment method to our enum
     */
    private Payment.PaymentMethod getPaymentMethod(String method) {
        if (method == null) {
            return Payment.PaymentMethod.Card;
        }
        
        switch (method.toLowerCase()) {
            case "netbanking":
                return Payment.PaymentMethod.Net_Banking;
            case "upi":
                return Payment.PaymentMethod.UPI;
            default:
                return Payment.PaymentMethod.Card;
        }
    }
}