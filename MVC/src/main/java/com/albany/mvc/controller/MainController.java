package com.albany.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main controller for public pages
 * Handles the landing page and other public routes
 */
@Controller
public class MainController {

    /**
     * Landing page - accessible by everyone
     */
    @GetMapping("/")
    public String index() {
        return "customer/index";
    }
    
    /**
     * About Us page - public access
     */
    @GetMapping("/about")
    public String about() {
        return "customer/aboutUs";
    }
    
    /**
     * Services information page - public access
     */
    @GetMapping("/services")
    public String services() {
        return "customer/services";
    }
    
    /**
     * Contact page - public access
     */
    @GetMapping("/contact")
    public String contact() {
        return "customer/contact";
    }
}