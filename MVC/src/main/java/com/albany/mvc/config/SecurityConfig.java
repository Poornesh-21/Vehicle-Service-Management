package com.albany.mvc.config;

import com.albany.mvc.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Explicitly permit these paths without authentication
                        .requestMatchers("/admin/login", "/admin/api/login",
                                "/serviceAdvisor/login", "/serviceAdvisor/api/login",
                                "/test-auth",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                        // Allow dashboard access with token parameter (will be handled by the controllers)
                        .requestMatchers(request ->
                                (request.getServletPath().equals("/admin/dashboard") ||
                                        request.getServletPath().equals("/serviceAdvisor/dashboard")) &&
                                        request.getParameter("token") != null).permitAll()
                        // Accept both ROLE_ADMIN and ROLE_admin for admin paths
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_admin")
                        // Accept both ROLE_SERVICEADVISOR and ROLE_serviceAdvisor for service advisor paths
                        .requestMatchers("/serviceAdvisor/**").hasAnyAuthority("ROLE_SERVICEADVISOR", "ROLE_serviceAdvisor")
                        // Any other request needs authentication
                        .anyRequest().authenticated()
                )
                // IMPORTANT: Use custom login controller instead of form login
                .formLogin(form -> form
                        .disable()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Make sure to maintain some session state for token storage between requests
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Add exception handling
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/admin/login?error=access_denied")
                );

        return http.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}