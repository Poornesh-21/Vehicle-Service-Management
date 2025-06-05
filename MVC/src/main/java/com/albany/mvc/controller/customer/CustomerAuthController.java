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

@Controller
@RequestMapping("/authentication")
public class CustomerAuthController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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

    @PostMapping("/register/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody Map<String, Object> requestData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Extract OTP from request data
            String otp = (String) requestData.get("otp");
            String email = (String) requestData.get("email");

            // Create verification request
            Map<String, Object> verificationRequest = new HashMap<>();
            verificationRequest.put("email", email);
            verificationRequest.put("otp", otp);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(verificationRequest, headers);

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
}