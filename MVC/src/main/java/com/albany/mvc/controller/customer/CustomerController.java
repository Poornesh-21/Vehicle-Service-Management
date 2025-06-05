package com.albany.mvc.controller.customer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Customer controller for handling all customer-specific routes
 * These routes are for authenticated customers only
 */
@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Customer dashboard
     */
    @GetMapping({"", "/"})
    public String dashboard() {
        // This is the customer dashboard after login
        return "customer/dashboard";
    }
    
    /**
     * Profile page
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        return "customer/profile";
    }
    
    /**
     * My Vehicles page
     */
    @GetMapping("/myVehicles")
    public String myVehicles(Model model) {
        return "customer/myVehicles";
    }
    
    /**
     * Service History page
     */
    @GetMapping("/serviceHistory")
    public String serviceHistory(Model model) {
        return "customer/serviceHistory";
    }
    
    /**
     * Book Service page
     */
    @GetMapping("/bookService")
    public String bookService(Model model) {
        return "customer/bookService";
    }
    
    /**
     * Membership page
     */
    @GetMapping("/membership")
    public String membership(Model model) {
        return "customer/membership";
    }
    
    /**
     * Track Service page
     */
    @GetMapping("/trackService")
    public String trackService(Model model) {
        return "customer/trackService";
    }
    
    /**
     * Invoices page
     */
    @GetMapping("/invoices")
    public String invoices(Model model) {
        return "customer/invoices";
    }
}