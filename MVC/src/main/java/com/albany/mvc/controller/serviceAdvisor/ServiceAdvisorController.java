package com.albany.mvc.controller.serviceAdvisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
@RequestMapping("/serviceAdvisor")
public class ServiceAdvisorController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorController.class);
    
    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;
    
    private final RestTemplate restTemplate;
    
    public ServiceAdvisorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "service advisor/login";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/serviceAdvisor/login?error=session_expired";
        }
        
        try {
            // Validate token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/validate-token",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("valid"))) {
                    Map<String, String> userInfo = (Map<String, String>) responseBody.get("user");
                    
                    // Add user info to the model
                    if (userInfo != null) {
                        String firstName = userInfo.get("firstName");
                        String lastName = userInfo.get("lastName");
                        String fullName = (firstName != null ? firstName : "") + " " + 
                                         (lastName != null ? lastName : "");
                        
                        model.addAttribute("userName", fullName.trim());
                        model.addAttribute("userEmail", userInfo.get("email"));
                        model.addAttribute("token", token);
                    }
                    
                    return "service advisor/dashboard";
                }
            }
            
            return "redirect:/serviceAdvisor/login?error=invalid_token";
        } catch (HttpClientErrorException e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return "redirect:/serviceAdvisor/login?error=invalid_token";
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return "redirect:/serviceAdvisor/login?error=server_error";
        }
    }
    
    @GetMapping("/logout")
    public String logout() {
        return "redirect:/serviceAdvisor/login?logout=true";
    }
}