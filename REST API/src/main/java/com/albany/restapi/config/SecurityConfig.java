package com.albany.restapi.config;

import com.albany.restapi.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/debug/**").permitAll()

                        // Admin API paths - explicitly define all HTTP methods
                        .requestMatchers(HttpMethod.GET, "/api/service-advisors/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin")
                        .requestMatchers(HttpMethod.POST, "/api/service-advisors/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin")
                        .requestMatchers(HttpMethod.PUT, "/api/service-advisors/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/service-advisors/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin")

                        // Other API paths can remain as they were
                        .requestMatchers("/api/vehicles/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin", "ROLE_CUSTOMER", "ROLE_customer")
                        .requestMatchers("/api/customers/{customerId}/vehicles/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin", "ROLE_CUSTOMER", "ROLE_customer")
                        .requestMatchers("/api/service-requests/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin", "ROLE_CUSTOMER", "ROLE_customer",
                                "ROLE_SERVICE_ADVISOR", "ROLE_serviceAdvisor")
                        .requestMatchers("/api/inventory/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_admin", "ROLE_SERVICE_ADVISOR", "ROLE_serviceAdvisor")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8081",
                "http://127.0.0.1:8081",
                "http://localhost:8082",
                "http://127.0.0.1:8082"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}