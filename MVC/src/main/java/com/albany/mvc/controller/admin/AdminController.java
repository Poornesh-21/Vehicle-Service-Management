package com.albany.mvc.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

    @GetMapping({"/admin/login"})
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        // Authentication checks now handled by JavaScript
        return "admin/dashboard";
    }

    @GetMapping("/admin/logout")
    public String logout() {
        return "redirect:/admin/login";
    }

    @GetMapping("/login")
    public String redirectLogin() {
        return "redirect:/admin/login";
    }
}