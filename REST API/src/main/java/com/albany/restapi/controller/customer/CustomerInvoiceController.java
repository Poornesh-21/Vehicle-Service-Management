package com.albany.restapi.controller.customer;

import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/invoices")
public class CustomerInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerInvoiceController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

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
     * Get invoices for the current customer
     */
    @GetMapping
    public ResponseEntity<?> getInvoices(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Fetching invoices for user: {}", user.getEmail());

            // Check if user is a customer
            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to access customer invoices", user.getEmail());
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Find all completed service requests for this user
            List<ServiceRequest> completedRequests = serviceRequestRepository.findAll().stream()
                    .filter(req -> req.getUserId().equals(user.getUserId().longValue()))
                    .filter(req -> req.getStatus() == ServiceRequest.Status.Completed)
                    .collect(Collectors.toList());

            logger.info("Found {} completed service requests", completedRequests.size());

            List<Map<String, Object>> invoices = new ArrayList<>();

            for (ServiceRequest request : completedRequests) {
                // Get all invoices for this request - using findAllByRequestId instead
                List<Invoice> requestInvoices = invoiceRepository.findAllByRequestId(request.getRequestId());

                // If no invoices found, continue to next request
                if (requestInvoices.isEmpty()) {
                    continue;
                }

                // Use the most recent invoice (assuming the latest is most relevant)
                Invoice invoice = requestInvoices.get(0);

                // Check if payment exists - using findAllByRequestId instead
                List<Payment> payments = paymentRepository.findAllByRequestId(request.getRequestId());
                boolean isPaid = !payments.isEmpty() && payments.get(0).getStatus() == Payment.Status.Completed;

                Map<String, Object> invoiceData = new HashMap<>();
                invoiceData.put("invoiceId", invoice.getInvoiceId());
                invoiceData.put("requestId", request.getRequestId());
                invoiceData.put("vehicleBrand", request.getVehicleBrand());
                invoiceData.put("vehicleModel", request.getVehicleModel());
                invoiceData.put("totalAmount", invoice.getTotalAmount());
                invoiceData.put("completedDate", request.getUpdatedAt());
                invoiceData.put("formattedCompletedDate", request.getUpdatedAt() != null ?
                        request.getUpdatedAt().format(DATE_FORMATTER) : null);
                invoiceData.put("paid", isPaid);

                invoices.add(invoiceData);
            }

            logger.info("Returning {} invoices", invoices.size());
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            logger.error("Error fetching invoices: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Create payment order for an invoice
     */
    @PostMapping("/{invoiceId}/payment")
    public ResponseEntity<?> createPaymentOrder(@PathVariable Integer invoiceId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Creating payment order for invoice: {} by user: {}", invoiceId, user.getEmail());

            // Check if user is a customer
            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to create payment", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            // Find the invoice
            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                logger.warn("Invoice not found: {}", invoiceId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice not found"
                ));
            }

            Invoice invoice = invoiceOpt.get();

            // Find the service request to verify ownership
            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(invoice.getRequestId());
            if (requestOpt.isEmpty()) {
                logger.warn("Service request not found for invoice: {}", invoiceId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request not found"
                ));
            }

            ServiceRequest request = requestOpt.get();

            // Verify that the request belongs to this user
            if (!request.getUserId().equals(user.getUserId().longValue())) {
                logger.warn("Invoice {} does not belong to user {}", invoiceId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice does not belong to this customer"
                ));
            }

            // Check if already paid - using findAllByRequestId instead
            List<Payment> payments = paymentRepository.findAllByRequestId(request.getRequestId());
            boolean isPaid = !payments.isEmpty() && payments.stream()
                    .anyMatch(p -> p.getStatus() == Payment.Status.Completed);

            if (isPaid) {
                logger.info("Invoice {} is already paid", invoiceId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice has already been paid"
                ));
            }

            // Get customer profile - using findAllByUser_UserId instead
            List<CustomerProfile> customerProfiles = customerRepository.findAllByUser_UserId(user.getUserId());
            if (customerProfiles.isEmpty()) {
                logger.warn("Customer profile not found for user: {}", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Customer profile not found"
                ));
            }

            CustomerProfile customer = customerProfiles.get(0);

            // Calculate amount in paisa (multiply by 100)
            int amount = invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();

            try {
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
                response.put("invoiceId", invoice.getInvoiceId());

                // Add user details for Razorpay prefill
                response.put("email", user.getEmail());
                response.put("name", user.getFirstName() + " " + user.getLastName());
                response.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

                logger.info("Payment order created successfully for invoice: {}", invoiceId);
                return ResponseEntity.ok(response);
            } catch (RazorpayException e) {
                logger.error("Razorpay error: {}", e.getMessage(), e);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment gateway error: " + e.getMessage()
                ));
            }
        } catch (Exception e) {
            logger.error("Error creating payment order: {}", e.getMessage(), e);
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
    @Transactional
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            // Check if user is a customer
            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to verify payment", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            String paymentId = request.get("paymentId");
            String razorpayPaymentId = request.get("razorpayPaymentId");
            String razorpayOrderId = request.get("razorpayOrderId");
            String invoiceIdStr = request.get("invoiceId");

            logger.info("Verifying payment: {}, razorpayPaymentId: {}", paymentId, razorpayPaymentId);

            if (paymentId == null || razorpayPaymentId == null || razorpayOrderId == null) {
                logger.warn("Missing required parameters for payment verification");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment ID, Razorpay Payment ID, and Order ID are required"
                ));
            }

            // Find the payment
            Optional<Payment> paymentOpt = paymentRepository.findById(Integer.parseInt(paymentId));
            if (paymentOpt.isEmpty()) {
                logger.warn("Payment not found: {}", paymentId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment not found"
                ));
            }

            Payment payment = paymentOpt.get();

            // Verify that the payment belongs to this user
            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(payment.getRequestId());
            if (requestOpt.isEmpty() || !requestOpt.get().getUserId().equals(user.getUserId().longValue())) {
                logger.warn("Payment {} does not belong to user {}", paymentId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment does not belong to this customer"
                ));
            }

            // Update payment status - CRITICAL: This updates the payment in the database
            payment.setStatus(Payment.Status.Completed);
            payment.setTransactionId(razorpayPaymentId);
            payment.setPaymentMethod(getPaymentMethod(request.get("paymentMethod")));
            payment.setPaymentTimestamp(LocalDateTime.now());

            // Save the updated payment to the database
            Payment updatedPayment = paymentRepository.save(payment);
            logger.info("Payment {} updated with status: {}", updatedPayment.getPaymentId(), updatedPayment.getStatus());

            // Also update the invoice if invoiceId is provided
            if (invoiceIdStr != null && !invoiceIdStr.isEmpty()) {
                try {
                    Integer invoiceId = Integer.parseInt(invoiceIdStr);
                    Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
                    if (invoiceOpt.isPresent()) {
                        Invoice invoice = invoiceOpt.get();
                        invoice.setPaymentId(updatedPayment.getPaymentId());
                        invoiceRepository.save(invoice);
                        logger.info("Invoice {} updated with payment ID: {}", invoice.getInvoiceId(), updatedPayment.getPaymentId());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid invoice ID format: {}", invoiceIdStr);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment successful",
                    "paymentId", updatedPayment.getPaymentId(),
                    "status", updatedPayment.getStatus().name(),
                    "transactionId", updatedPayment.getTransactionId() != null ? updatedPayment.getTransactionId() : ""
            ));
        } catch (NumberFormatException e) {
            logger.error("Invalid payment ID format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid payment ID format"
            ));
        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage(), e);
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