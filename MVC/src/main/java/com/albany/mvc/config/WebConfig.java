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

@Configuration
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Admin routes
        registry.addViewController("/admin/login").setViewName("admin/login");
        registry.addRedirectViewController("/service-advisors", "/admin/service-advisors");
        registry.addRedirectViewController("/", "/admin/login");
        registry.addRedirectViewController("/login", "/admin/login");

        // Service Advisor routes
        registry.addViewController("/serviceAdvisor/login").setViewName("service advisor/login");
        registry.addRedirectViewController("/serviceAdvisor", "/serviceAdvisor/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/CSS/**").addResourceLocations("classpath:/static/CSS/");
        registry.addResourceHandler("/Javascript/**").addResourceLocations("classpath:/static/Javascript/");
        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/");

        // Add explicit entries for Service Advisor resources
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authorize -> authorize
                        // Static resources
                        .requestMatchers("/CSS/**", "/Javascript/**", "/assets/**", "/css/**", "/js/**").permitAll()

                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/serviceAdvisor/login").permitAll()
                        .requestMatchers("/service-advisors").permitAll() // For redirects
                        .requestMatchers("/serviceAdvisor").permitAll() // For redirects

                        .anyRequest().permitAll()
                )

                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}