package com.albany.mvc.controller.admin;

import com.albany.mvc.dto.CustomerDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
public class AdminCustomerController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/admin/customers")
    public String customersPage(@RequestParam(required = false) String token, 
                               @RequestParam(required = false) String success,
                               Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin/login?error=session_expired";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CustomerDTO[]> response = restTemplate.exchange(
                apiBaseUrl + "/admin/customers/api", 
                HttpMethod.GET, 
                entity, 
                CustomerDTO[].class
            );

            List<CustomerDTO> customers = Arrays.asList(response.getBody());
            customers.forEach(customer -> {
                if (customer.getLastServiceDate() != null) {
                    customer.setFormattedLastServiceDate(
                        customer.getLastServiceDate().format(
                            DateTimeFormatter.ofPattern("MMM d, yyyy")
                        )
                    );
                } else {
                    customer.setFormattedLastServiceDate("No service yet");
                }
            });

            model.addAttribute("customers", customers);
        } catch (Exception e) {
            model.addAttribute("customers", Collections.emptyList());
        }

        model.addAttribute("token", token);
        if (success != null) {
            model.addAttribute("success", success);
        }

        return "admin/customers";
    }
}