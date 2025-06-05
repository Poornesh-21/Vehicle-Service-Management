package com.albany.mvc.controller.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @GetMapping({"", "/"})
    public String index() {
        return "customer/index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "customer/dashboard";
    }

    @GetMapping("/bookService")
    public String bookService() {
        return "customer/bookService";
    }

    @GetMapping("/profile")
    public String profile() {
        return "customer/profile";
    }

}