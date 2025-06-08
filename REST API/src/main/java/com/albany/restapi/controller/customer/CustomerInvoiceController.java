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
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final MaterialUsageRepository materialUsageRepository;

    public CustomerInvoiceController(InvoiceRepository invoiceRepository,
                                     PaymentRepository paymentRepository,
                                     ServiceRequestRepository serviceRequestRepository,
                                     CustomerRepository customerRepository,
                                     ServiceTrackingRepository serviceTrackingRepository,
                                     MaterialUsageRepository materialUsageRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.customerRepository = customerRepository;
        this.serviceTrackingRepository = serviceTrackingRepository;
        this.materialUsageRepository = materialUsageRepository;
    }

    @GetMapping
    public ResponseEntity<?> getInvoices(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Fetching invoices for user: {}", user.getEmail());

            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to access customer invoices", user.getEmail());
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<ServiceRequest> completedRequests = serviceRequestRepository.findAll().stream()
                    .filter(req -> req.getUserId().equals(user.getUserId().longValue()))
                    .filter(req -> req.getStatus() == ServiceRequest.Status.Completed)
                    .collect(Collectors.toList());

            logger.info("Found {} completed service requests", completedRequests.size());

            List<Map<String, Object>> invoices = new ArrayList<>();

            for (ServiceRequest request : completedRequests) {
                List<Invoice> requestInvoices = invoiceRepository.findAllByRequestId(request.getRequestId());

                if (requestInvoices.isEmpty()) {
                    continue;
                }

                Invoice invoice = requestInvoices.get(0);

                List<Payment> payments = paymentRepository.findAllByRequestId(request.getRequestId());
                boolean isPaid = !payments.isEmpty() && payments.stream().anyMatch(p -> p.getStatus() == Payment.Status.Completed);

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

    @PostMapping("/{invoiceId}/payment")
    public ResponseEntity<?> createPaymentOrder(@PathVariable Integer invoiceId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Creating payment order for invoice: {} by user: {}", invoiceId, user.getEmail());

            if (user.getRole() != User.Role.customer) {
                logger.warn("Non-customer user {} attempted to create payment", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                logger.warn("Invoice not found: {}", invoiceId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice not found"
                ));
            }

            Invoice invoice = invoiceOpt.get();

            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(invoice.getRequestId());
            if (requestOpt.isEmpty()) {
                logger.warn("Service request not found for invoice: {}", invoiceId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Service request not found"
                ));
            }

            ServiceRequest request = requestOpt.get();

            if (!request.getUserId().equals(user.getUserId().longValue())) {
                logger.warn("Invoice {} does not belong to user {}", invoiceId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invoice does not belong to this customer"
                ));
            }

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

            List<CustomerProfile> customerProfiles = customerRepository.findAllByUser_UserId(user.getUserId());
            if (customerProfiles.isEmpty()) {
                logger.warn("Customer profile not found for user: {}", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Customer profile not found"
                ));
            }

            CustomerProfile customer = customerProfiles.get(0);

            int amount = invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();

            try {
                RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amount);
                orderRequest.put("currency", razorpayCurrency);
                orderRequest.put("receipt", "rcpt_inv_" + invoiceId);
                orderRequest.put("payment_capture", true);

                Order order = razorpay.orders.create(orderRequest);
                String orderId = order.get("id");

                Payment payment;
                if (!payments.isEmpty()) {
                    payment = payments.stream()
                            .filter(p -> p.getStatus() != Payment.Status.Completed)
                            .findFirst()
                            .orElseGet(() -> Payment.builder()
                                    .requestId(request.getRequestId())
                                    .customerId(customer.getCustomerId())
                                    .amount(invoice.getTotalAmount())
                                    .paymentMethod(Payment.PaymentMethod.Card)
                                    .status(Payment.Status.Pending)
                                    .paymentTimestamp(LocalDateTime.now())
                                    .build());

                    payment.setStatus(Payment.Status.Pending);
                    payment.setPaymentTimestamp(LocalDateTime.now());
                } else {
                    payment = Payment.builder()
                            .requestId(request.getRequestId())
                            .customerId(customer.getCustomerId())
                            .amount(invoice.getTotalAmount())
                            .paymentMethod(Payment.PaymentMethod.Card)
                            .status(Payment.Status.Pending)
                            .paymentTimestamp(LocalDateTime.now())
                            .build();
                }

                Payment savedPayment = paymentRepository.save(payment);

                invoice.setPaymentId(savedPayment.getPaymentId());
                invoiceRepository.save(invoice);

                Map<String, Object> response = new HashMap<>();
                response.put("orderId", orderId);
                response.put("amount", amount);
                response.put("currency", razorpayCurrency);
                response.put("razorpayKey", razorpayKeyId);
                response.put("paymentId", savedPayment.getPaymentId());
                response.put("invoiceId", invoiceId);

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

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Verifying payment for user: {}", user.getEmail());

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

            logger.info("Verifying payment: {}, razorpayPaymentId: {}, for invoice: {}",
                    paymentId, razorpayPaymentId, invoiceIdStr);

            if (paymentId == null || razorpayPaymentId == null || razorpayOrderId == null) {
                logger.warn("Missing required parameters for payment verification");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment ID, Razorpay Payment ID, and Order ID are required"
                ));
            }

            Optional<Payment> paymentOpt = paymentRepository.findById(Integer.parseInt(paymentId));
            if (paymentOpt.isEmpty()) {
                logger.warn("Payment not found: {}", paymentId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment not found"
                ));
            }

            Payment payment = paymentOpt.get();

            Optional<ServiceRequest> requestOpt = serviceRequestRepository.findById(payment.getRequestId());
            if (requestOpt.isEmpty() || !requestOpt.get().getUserId().equals(user.getUserId().longValue())) {
                logger.warn("Payment {} does not belong to user {}", paymentId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment does not belong to this customer"
                ));
            }

            payment.setStatus(Payment.Status.Completed);
            payment.setTransactionId(razorpayPaymentId);
            payment.setPaymentMethod(getPaymentMethod(request.get("paymentMethod")));
            payment.setPaymentTimestamp(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Payment {} verified successfully, marked as COMPLETED", paymentId);

            if (invoiceIdStr != null && !invoiceIdStr.isEmpty()) {
                try {
                    Integer invoiceId = Integer.parseInt(invoiceIdStr);
                    Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
                    if (invoiceOpt.isPresent()) {
                        Invoice invoice = invoiceOpt.get();
                        invoice.setPaymentId(savedPayment.getPaymentId());
                        invoiceRepository.save(invoice);
                        logger.info("Updated invoice {} with payment {}", invoiceId, savedPayment.getPaymentId());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid invoice ID format: {}", invoiceIdStr);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment successful",
                    "paymentId", savedPayment.getPaymentId()
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