package com.albany.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {


    @GetMapping("/")
    public String index() {
        return "customer/index";
    }
    @GetMapping("/about")
    public String about() {

        return "customer/aboutUs";
    }

    @GetMapping("/services")
    public String services() {

        return "customer/services";
    }

    @GetMapping("/contact")
    public String contact() {

        return "customer/contact";
    }
}