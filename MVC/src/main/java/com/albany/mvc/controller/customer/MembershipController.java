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

import jakarta.servlet.http.HttpServletRequest;
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
    public String membershipPage(HttpServletRequest request, HttpSession session, Model model,
                                 @RequestParam(required = false) String success,
                                 @RequestParam(required = false) String error,
                                 @RequestParam(required = false) Boolean showSuccess,
                                 @RequestParam(required = false) String redirect,
                                 @RequestParam(required = false) Boolean auth) {

        // Get authentication token from session AND header
        String token = (String) session.getAttribute("authToken");

        // Check for token in request header (for client-side authentication)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            // Store in session for future requests
            session.setAttribute("authToken", token);
        }

        // Default to standard plan if no user data is available
        model.addAttribute("isPremium", false);

        if (token != null && !token.isEmpty()) {
            try {
                // Set up headers with authentication token
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);

                // Fetch user profile data from API
                ResponseEntity<Map> response = restTemplate.exchange(
                        apiBaseUrl + "/api/customer/profile",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> userData = response.getBody();

                    // Add user data to model
                    model.addAttribute("user", userData);

                    // Check if user has premium membership - IMPORTANT: This must match the database value
                    String membershipType = (String) userData.get("membershipType");
                    boolean isPremium = "PREMIUM".equalsIgnoreCase(membershipType);

                    logger.info("User membership type from database: {}, isPremium: {}", membershipType, isPremium);

                    // Set isPremium flag to control UI display
                    model.addAttribute("isPremium", isPremium);

                    // Add membership expiration info if available
                    if (userData.containsKey("membershipEndDate")) {
                        model.addAttribute("membershipEndDate", userData.get("membershipEndDate"));
                    }

                    if (userData.containsKey("membershipDaysRemaining")) {
                        model.addAttribute("daysRemaining", userData.get("membershipDaysRemaining"));
                    }
                }
            } catch (HttpClientErrorException e) {
                logger.error("Error fetching user data: {}", e.getMessage());
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    // Clear invalid session token
                    session.removeAttribute("authToken");

                    // Redirect to login page with error message
                    String redirectParam = redirect != null ? "&redirect=" + redirect : "";
                    return "redirect:/authentication/login?message=Your session has expired. Please login again.&type=error" + redirectParam;
                }
            } catch (Exception e) {
                logger.error("Unexpected error fetching user data: {}", e.getMessage());
                // Continue with default values
            }
        } else if (Boolean.TRUE.equals(auth)) {
            // If auth=true parameter was passed but no token found, redirect to login
            return "redirect:/authentication/login?message=Please login to view membership options&type=info&redirect=/customer/membership";
        }

        // Add any success/error messages to model
        if (success != null && !success.isEmpty()) {
            model.addAttribute("success", success);
        }

        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }

        // Show success modal if payment was successful
        model.addAttribute("showSuccessModal", Boolean.TRUE.equals(showSuccess));

        return "customer/membership";
    }

    /**
     * Initiate payment for premium membership
     */
    @PostMapping("/initiate-payment")
    public String initiatePayment(HttpServletRequest request, HttpSession session, Model model) {
        // Get authentication token from session or header
        String token = (String) session.getAttribute("authToken");

        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            // Store in session for future requests
            session.setAttribute("authToken", token);
        }

        if (token == null || token.isEmpty()) {
            // Redirect to login if user is not authenticated
            return "redirect:/authentication/login?message=Please login to upgrade membership&type=info&redirect=/customer/membership";
        }

        try {
            // First, check if user is already a premium member
            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.set("Authorization", "Bearer " + token);

            // Get user profile to check current membership
            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/profile",
                    HttpMethod.GET,
                    new HttpEntity<>(profileHeaders),
                    Map.class
            );

            if (profileResponse.getStatusCode() == HttpStatus.OK && profileResponse.getBody() != null) {
                Map<String, Object> userData = profileResponse.getBody();
                String membershipType = (String) userData.get("membershipType");

                logger.info("Checking membership before payment: {}", membershipType);

                // If already premium, return with message
                if ("PREMIUM".equalsIgnoreCase(membershipType)) {
                    return "redirect:/customer/membership?error=You already have a Premium membership";
                }

                // Add user data to model for displaying in the membership page
                model.addAttribute("user", userData);
                model.addAttribute("isPremium", false);
            }

            // Set up headers for payment API request
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

                // Add Razorpay data to model
                model.addAttribute("razorpayKey", responseData.get("razorpayKey"));
                model.addAttribute("orderId", responseData.get("orderId"));
                model.addAttribute("amount", responseData.get("amount"));
                model.addAttribute("currency", responseData.get("currency"));
                model.addAttribute("userEmail", responseData.get("email"));
                model.addAttribute("userName", responseData.get("name"));
                model.addAttribute("userPhone", responseData.get("phone"));

                // Store auth token in session for the rendered page
                session.setAttribute("authToken", token);

                return "customer/membership";
            } else {
                return "redirect:/customer/membership?error=Failed to create payment order. Please try again.";
            }
        } catch (HttpClientErrorException e) {
            logger.error("Error initiating payment: {}, Status: {}", e.getMessage(), e.getStatusCode());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Clear invalid session token
                session.removeAttribute("authToken");

                return "redirect:/authentication/login?message=Your session has expired. Please login again.&type=error&redirect=/customer/membership";
            }

            String errorMsg = e.getMessage();
            // Extract message from response body if possible
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("message")) {
                    // Simple extraction, could use JSON parsing in a more robust implementation
                    errorMsg = responseBody.split("message")[1].split("\"")[2];
                }
            } catch (Exception ex) {
                // Fallback to default error message
            }

            return "redirect:/customer/membership?error=" + encodeErrorMessage(errorMsg);
        } catch (Exception e) {
            logger.error("Unexpected error initiating payment: {}", e.getMessage());
            return "redirect:/customer/membership?error=An unexpected error occurred. Please try again.";
        }
    }

    /**
     * Verify payment and update membership
     */
    @PostMapping("/verify-payment")
    public String verifyPayment(HttpServletRequest request, HttpSession session,
                                @RequestParam String paymentId,
                                @RequestParam String razorpayOrderId) {

        // Get authentication token from session
        String token = (String) session.getAttribute("authToken");

        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            // Store in session for future requests
            session.setAttribute("authToken", token);
        }

        if (token == null || token.isEmpty()) {
            return "redirect:/authentication/login?message=Please login to verify payment&type=info&redirect=/customer/membership";
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
                    // Update the session userInfo to reflect the premium status
                    return "redirect:/customer/membership?success=Congratulations! Your membership has been upgraded to Premium.&showSuccess=true";
                } else {
                    String message = (String) responseData.getOrDefault("message", "Payment verification failed");
                    return "redirect:/customer/membership?error=" + encodeErrorMessage(message);
                }
            } else {
                return "redirect:/customer/membership?error=Failed to verify payment. Please contact support.";
            }
        } catch (HttpClientErrorException e) {
            logger.error("Error verifying payment: {}, Status: {}", e.getMessage(), e.getStatusCode());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Clear invalid session token
                session.removeAttribute("authToken");

                return "redirect:/authentication/login?message=Your session has expired. Please login again.&type=error&redirect=/customer/membership";
            }

            String errorMsg = e.getMessage();
            // Extract message from response body if possible
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("message")) {
                    errorMsg = responseBody.split("message")[1].split("\"")[2];
                }
            } catch (Exception ex) {
                // Fallback to default error message
            }

            return "redirect:/customer/membership?error=" + encodeErrorMessage(errorMsg);
        } catch (Exception e) {
            logger.error("Unexpected error verifying payment: {}", e.getMessage());
            return "redirect:/customer/membership?error=An unexpected error occurred during payment verification. Please contact support.";
        }
    }

    /**
     * Helper method to encode error messages for URL parameters
     */
    private String encodeErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }

        // Simple encoding to avoid URL issues
        return message.replace(" ", "%20")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("?", "%3F")
                .replace("#", "%23");
    }
}