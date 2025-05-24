package com.albany.mvc.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping({"/", "/admin/login"})
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
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