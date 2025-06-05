package com.albany.mvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the MVC application
 */
@Configuration
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure view controllers for direct view resolution without controller logic
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // DO NOT add mappings for the root URL here - it's handled by MainController

        // Admin routes
        registry.addViewController("/admin/login").setViewName("admin/login");

        // Service Advisor routes
        registry.addViewController("/serviceAdvisor/login").setViewName("service advisor/login");

        // Authentication routes
        registry.addViewController("/authentication/login").setViewName("customer/login");
    }

    /**
     * Configure static resource handlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/CSS/**").addResourceLocations("classpath:/static/CSS/");
        registry.addResourceHandler("/Javascript/**").addResourceLocations("classpath:/static/Javascript/");
        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/");

        // Add explicit entries for Service Advisor resources
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
    }

    /**
     * Configure security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF for API calls

                .authorizeHttpRequests(authorize -> authorize
                        // Static resources
                        .requestMatchers("/CSS/**", "/Javascript/**", "/assets/**", "/css/**", "/js/**").permitAll()

                        // Public routes
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/about").permitAll()
                        .requestMatchers("/services").permitAll()
                        .requestMatchers("/contact").permitAll()

                        // Authentication routes
                        .requestMatchers("/authentication/**").permitAll()
                        .requestMatchers("/api/customer/auth/**").permitAll()

                        // Login pages
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/serviceAdvisor/login").permitAll()

                        // Customer routes - client-side auth only, can enable server-side if needed
                        .requestMatchers("/customer/**").permitAll()

                        // Allow everything else by default
                        .anyRequest().permitAll()
                )

                .formLogin(AbstractHttpConfigurer::disable);  // Disable default form login

        return http.build();
    }
}