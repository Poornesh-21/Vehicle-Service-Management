package com.albany.mvc.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @GetMapping({"", "/"})
    public String index() {
        return "customer/index";
    }
    
    @GetMapping("/bookService")
    public String bookService() {
        return "customer/bookService";
    }
    
    @GetMapping("/aboutUs")
    public String aboutUs() {
        return "customer/aboutUs";
    }
    
    @GetMapping("/membership")
    public String membership() {
        return "customer/membership";
    }
}