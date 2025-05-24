package com.albany.mvc.controller.serviceAdvisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/serviceAdvisor")
public class ServiceAdvisorController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorController.class);

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "service advisor/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "service advisor/dashboard";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/serviceAdvisor/login?logout=true";
    }
}