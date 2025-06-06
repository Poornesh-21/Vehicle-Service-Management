package com.albany.mvc.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for customer membership pages
 */
@Controller
@RequestMapping("/customer/membership")
public class MembershipController {

    private static final Logger logger = LoggerFactory.getLogger(MembershipController.class);

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public MembershipController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Display membership plans page
     */
    @GetMapping
    public String membershipPage(HttpSession session, Model model,
                                 @RequestParam(required = false) String success,
                                 @RequestParam(required = false) String error,
                                 @RequestParam(required = false) boolean showSuccess) {

        // Always set isPremium to false by default to avoid null values
        model.addAttribute("isPremium", false);

        // Get authentication token from session
        String token = (String) session.getAttribute("authToken");

        if (token != null && !token.isEmpty()) {
            try {
                // Set up headers with authentication token
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);

                // Fetch user data
                ResponseEntity<Map> response = restTemplate.exchange(
                        apiBaseUrl + "/api/customer/profile",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> userData = response.getBody();
                    model.addAttribute("user", userData);

                    // Check if user has premium membership
                    String membershipType = (String) userData.get("membershipType");
                    boolean isPremium = "PREMIUM".equalsIgnoreCase(membershipType);
                    model.addAttribute("isPremium", isPremium);
                }
            } catch (HttpClientErrorException e) {
                logger.error("Error fetching user data: {}", e.getMessage());
                // If unauthorized, don't add user data to model
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    model.addAttribute("error", "Your session has expired. Please login again.");
                    return "redirect:/authentication/login";
                }
            } catch (Exception e) {
                logger.error("Unexpected error fetching user data: {}", e.getMessage());
                // Log the error but continue with the default isPremium = false
            }
        }

        // Add any success/error messages
        if (success != null) {
            model.addAttribute("success", success);
        }

        if (error != null) {
            model.addAttribute("error", error);
        }

        // Show success modal if payment was successful
        model.addAttribute("showSuccessModal", showSuccess);

        return "customer/membership";
    }

    /**
     * Initiate payment for premium membership
     */
    @PostMapping("/initiate-payment")
    public String initiatePayment(HttpSession session, Model model) {
        // Get authentication token from session
        String token = (String) session.getAttribute("authToken");

        if (token == null || token.isEmpty()) {
            return "redirect:/authentication/login?message=Please login to upgrade membership&type=info";
        }

        try {
            // Set up headers with authentication token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", 120000); // 1200 INR in paise
            requestBody.put("currency", "INR");

            // Make API call to create payment order
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/membership/create-order",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseData = response.getBody();

                // Add data for Razorpay integration
                model.addAttribute("razorpayKey", responseData.get("razorpayKey"));
                model.addAttribute("orderId", responseData.get("orderId"));
                model.addAttribute("amount", responseData.get("amount"));
                model.addAttribute("currency", responseData.get("currency"));
                model.addAttribute("userEmail", responseData.get("email"));
                model.addAttribute("userName", responseData.get("name"));
                model.addAttribute("userPhone", responseData.get("phone"));

                // Set default isPremium to false
                model.addAttribute("isPremium", false);

                return "customer/membership";
            } else {
                return "redirect:/customer/membership?error=Failed to create payment order. Please try again.";
            }
        } catch (HttpClientErrorException e) {
            logger.error("Error initiating payment: {}, Status: {}", e.getMessage(), e.getStatusCode());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "redirect:/authentication/login?message=Your session has expired. Please login again.&type=error";
            }

            return "redirect:/customer/membership?error=Payment initiation failed: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error initiating payment: {}", e.getMessage());
            return "redirect:/customer/membership?error=An unexpected error occurred. Please try again.";
        }
    }

    /**
     * Verify payment and update membership
     */
    @PostMapping("/verify-payment")
    public String verifyPayment(HttpSession session,
                                @RequestParam String paymentId,
                                @RequestParam String razorpayOrderId) {

        // Get authentication token from session
        String token = (String) session.getAttribute("authToken");

        if (token == null || token.isEmpty()) {
            return "redirect:/authentication/login?message=Please login to verify payment&type=info";
        }

        try {
            // Set up headers with authentication token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentId", paymentId);
            requestBody.put("razorpayOrderId", razorpayOrderId);

            // Make API call to verify payment
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/membership/verify-payment",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseData = response.getBody();
                boolean success = (boolean) responseData.getOrDefault("success", false);

                if (success) {
                    return "redirect:/customer/membership?success=Congratulations! Your membership has been upgraded to Premium.&showSuccess=true";
                } else {
                    String message = (String) responseData.getOrDefault("message", "Payment verification failed");
                    return "redirect:/customer/membership?error=" + message;
                }
            } else {
                return "redirect:/customer/membership?error=Failed to verify payment. Please contact support.";
            }
        } catch (HttpClientErrorException e) {
            logger.error("Error verifying payment: {}, Status: {}", e.getMessage(), e.getStatusCode());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "redirect:/authentication/login?message=Your session has expired. Please login again.&type=error";
            }

            return "redirect:/customer/membership?error=Payment verification failed: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error verifying payment: {}", e.getMessage());
            return "redirect:/customer/membership?error=An unexpected error occurred during payment verification. Please contact support.";
        }
    }
}