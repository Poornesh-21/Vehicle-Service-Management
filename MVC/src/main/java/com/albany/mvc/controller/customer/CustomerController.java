package com.albany.mvc.controller.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for customer pages
 */
@Controller
@RequestMapping("/customer")
public class CustomerController {

    /**
     * Customer about us page
     */
    @GetMapping("/aboutUs")
    public String aboutUs(Model model) {
        return "customer/aboutUs";
    }

//    /**
//     * Customer profile page
//     */
//    @GetMapping("/profile")
//    public String profile(Model model) {
//        return "customer/profile";
//    }

    /**
     * Customer vehicles page
     */
    @GetMapping("/myVehicles")
    public String myVehicles(Model model) {
        return "customer/myVehicles";
    }

    /**
     * Customer service history page
     */
    @GetMapping("/serviceHistory")
    public String serviceHistory(Model model) {
        return "customer/serviceHistory";
    }

}