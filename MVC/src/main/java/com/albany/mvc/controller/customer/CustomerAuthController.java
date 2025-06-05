package com.albany.mvc.controller.customer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for customer authentication
 */
@Controller
@RequestMapping("/authentication")
public class CustomerAuthController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Login/registration page
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String message,
                            @RequestParam(required = false) String type,
                            Model model) {
        if (message != null) {
            model.addAttribute("message", message);
            model.addAttribute("messageType", type != null ? type : "info");
        }
        return "customer/login";
    }

    /**
     * Send login OTP
     */
    @PostMapping("/login/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendLoginOtp(@RequestParam String email) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("email", email);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/auth/login/send-otp",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to send OTP: " + e.getMessage(),
                    "errorField", "email"
            ));
        }
    }

    /**
     * Verify login OTP
     */
    @PostMapping("/login/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyLoginOtp(@RequestParam String email, @RequestParam String otp) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("otp", otp);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/auth/login/verify-otp",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to verify OTP: " + e.getMessage(),
                    "errorField", "otp"
            ));
        }
    }

    /**
     * Send registration OTP
     */
    @PostMapping("/register/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendRegistrationOtp(@RequestBody Map<String, Object> registrationData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registrationData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/auth/register/send-otp",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to send registration OTP: " + e.getMessage(),
                    "errorField", "email"
            ));
        }
    }

    /**
     * Verify registration OTP
     */
    @PostMapping("/register/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody Map<String, Object> requestData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/auth/register/verify-otp",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to verify registration OTP: " + e.getMessage(),
                    "errorField", "otp"
            ));
        }
    }

    /**
     * Validate token endpoint
     */
    @GetMapping("/validate-token")
    @ResponseBody
    public ResponseEntity<?> validateToken(@RequestParam(required = false) String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/auth/validate-token",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Failed to validate token: " + e.getMessage()
            ));
        }
    }
}