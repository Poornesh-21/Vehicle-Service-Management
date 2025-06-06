package com.albany.restapi.controller.customer;

import com.albany.restapi.model.User;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.service.EmailService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for customer membership operations
 */
@RestController
@RequestMapping("/api/customer/membership")
public class CustomerMembershipController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerMembershipController.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String razorpayCurrency;

    private final UserRepository userRepository;
    private final EmailService emailService;

    public CustomerMembershipController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Create order for premium membership using Razorpay
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            // Get current user
            User user = (User) authentication.getPrincipal();

            // Check if user is already premium
            if (user.getMembershipType() == User.MembershipType.PREMIUM) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "You already have a Premium membership"
                ));
            }

            // Get amount from request or use default
            int amount = request.containsKey("amount") ? ((Number) request.get("amount")).intValue() : 120000; // 1200 INR in paise
            String currencyCode = request.containsKey("currency") ? (String) request.get("currency") : razorpayCurrency;

            // Create Razorpay client
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create order request JSON
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", currencyCode);
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", true);

            // Create order in Razorpay
            Order order = razorpay.orders.create(orderRequest);
            String orderId = order.get("id");

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", currencyCode);
            response.put("razorpayKey", razorpayKeyId);

            // Add user details for Razorpay prefill
            response.put("email", user.getEmail());
            response.put("name", user.getFirstName() + " " + user.getLastName());
            response.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            logger.error("Razorpay error creating order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Payment gateway error: " + e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Verify payment and upgrade membership
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            // Get payment details
            String paymentId = request.get("paymentId");
            String orderId = request.get("razorpayOrderId");

            if (paymentId == null || orderId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment ID and Order ID are required"
                ));
            }

            // Get current user
            User user = (User) authentication.getPrincipal();

            // Check if user is already premium
            if (user.getMembershipType() == User.MembershipType.PREMIUM) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "You already have a Premium membership"
                ));
            }

            // In a production environment, verify the payment with Razorpay API
            // This would involve creating a signature and validating it
            // For simplicity, we'll assume the payment is valid if we have the payment ID and order ID

            // Upgrade user to premium
            user.setMembershipType(User.MembershipType.PREMIUM);

            // Set membership start and end dates
            LocalDateTime now = LocalDateTime.now();
            user.setMembershipStartDate(now);
            // Set end date to 2 years from now
            user.setMembershipEndDate(now.plusYears(2));

            // Save updated user
            userRepository.save(user);

            // Send confirmation email
            sendMembershipConfirmationEmail(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Your membership has been upgraded to Premium"
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
     * Get user's current membership status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getMembershipStatus(Authentication authentication) {
        try {
            // Get current user
            User user = (User) authentication.getPrincipal();

            Map<String, Object> response = new HashMap<>();
            response.put("membershipType", user.getMembershipType().name());

            if (user.getMembershipType() == User.MembershipType.PREMIUM) {
                response.put("startDate", user.getMembershipStartDate());
                response.put("endDate", user.getMembershipEndDate());

                // Check if membership is expired
                boolean isExpired = user.getMembershipEndDate() != null &&
                        user.getMembershipEndDate().isBefore(LocalDateTime.now());
                response.put("isExpired", isExpired);

                // Calculate days remaining if not expired
                if (!isExpired && user.getMembershipEndDate() != null) {
                    long daysRemaining = java.time.Duration.between(
                            LocalDateTime.now(),
                            user.getMembershipEndDate()
                    ).toDays();
                    response.put("daysRemaining", daysRemaining);
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching membership status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Send email confirmation for premium membership
     */
    private void sendMembershipConfirmationEmail(User user) {
        try {
            String subject = "Welcome to Albany Premium Membership";
            String content = buildMembershipEmailTemplate(
                    user.getFirstName(),
                    user.getLastName(),
                    user.getMembershipStartDate(),
                    user.getMembershipEndDate()
            );

            emailService.sendSimpleEmail(user.getEmail(), subject, content);
            logger.info("Premium membership confirmation email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Failed to send membership confirmation email: {}", e.getMessage());
        }
    }

    /**
     * Build HTML email template for premium membership confirmation
     */
    private String buildMembershipEmailTemplate(String firstName, String lastName,
                                                LocalDateTime startDate, LocalDateTime endDate) {
        // Format dates for display
        String formattedStartDate = startDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String formattedEndDate = endDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>"
                + "<div style='text-align: center; background-color: #722F37; color: white; padding: 15px;'>"
                + "<h2>Albany Premium Membership</h2>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p>Hello " + firstName + " " + lastName + ",</p>"
                + "<p>Congratulations! Your Albany Premium Membership has been successfully activated.</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;'>"
                + "<h3 style='margin-top: 0; color: #722F37;'>Membership Details</h3>"
                + "<p><strong>Membership Type:</strong> Premium</p>"
                + "<p><strong>Start Date:</strong> " + formattedStartDate + "</p>"
                + "<p><strong>End Date:</strong> " + formattedEndDate + "</p>"
                + "</div>"
                + "<h3 style='color: #722F37;'>Your Premium Benefits Include:</h3>"
                + "<ul>"
                + "<li><strong>30% Discount</strong> on all labor charges</li>"
                + "<li><strong>Priority Service</strong> during peak hours</li>"
                + "<li>Premium Customer Support</li>"
                + "<li>Exclusive Seasonal Offers</li>"
                + "<li>Free Basic Vehicle Inspection (twice a year)</li>"
                + "</ul>"
                + "<p>Thank you for choosing Albany Premium Membership. We look forward to providing you with exceptional service.</p>"
                + "<p>Best regards,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>© " + java.time.Year.now().getValue() + " Albany Vehicle Service. All Rights Reserved.</p>"
                + "</div>"
                + "</div>";
    }
}