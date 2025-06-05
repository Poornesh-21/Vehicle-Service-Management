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
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping({"", "/"})
    public String index() {
        // The JavaScript will handle redirecting non-authenticated users
        return "customer/index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        // The JavaScript will handle redirecting non-authenticated users
        return "customer/dashboard";
    }

    @GetMapping("/bookService")
    public String bookService() {
        // The JavaScript will handle redirecting non-authenticated users
        return "customer/bookService";
    }

    @GetMapping("/profile")
    public String profile() {
        // The JavaScript will handle redirecting non-authenticated users
        return "customer/profile";
    }
}