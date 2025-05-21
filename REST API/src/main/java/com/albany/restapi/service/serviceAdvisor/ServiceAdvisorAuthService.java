package com.albany.restapi.service.serviceAdvisor;

import com.albany.restapi.dto.AuthResponse;
import com.albany.restapi.model.ServiceAdvisorProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.ServiceAdvisorRepository;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ServiceAdvisorAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorAuthService.class);
    
    private final UserRepository userRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * Validate that the user is a service advisor and token is valid
     */
    public User validateServiceAdvisorToken(String username, String token) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        
        if (user.getRole() != User.Role.serviceAdvisor) {
            throw new BadCredentialsException("User is not a service advisor");
        }
        
        if (!jwtUtil.validateToken(token, user)) {
            throw new BadCredentialsException("Invalid token");
        }
        
        return user;
    }
    
    /**
     * Check if the service advisor exists
     */
    public boolean serviceAdvisorExists(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getRole() == User.Role.serviceAdvisor)
                .orElse(false);
    }
    
    /**
     * Get service advisor profile
     */
    public ServiceAdvisorProfile getServiceAdvisorProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        if (user.getRole() != User.Role.serviceAdvisor) {
            throw new BadCredentialsException("User is not a service advisor");
        }
        
        return serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found for user: " + email));
    }
    
    /**
     * Change password for a service advisor
     */
    @Transactional
    public AuthResponse changePassword(String username, String currentPassword, String newPassword, 
                                  String confirmPassword, boolean isTemporaryPassword) {
        
        // Validate inputs
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }
        
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
        
        // Password strength validation (skip for debugging/development if needed)
        validatePasswordStrength(newPassword);
        
        // Get user
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        
        // Verify current password (if not a temporary password reset)
        if (!isTemporaryPassword) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new BadCredentialsException("Current password is incorrect");
            }
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Generate new token
        String newToken = jwtUtil.generateToken(user);
        
        return AuthResponse.builder()
                .token(newToken)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
    
    /**
     * Validate password strength
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        
        if (!Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
    
    /**
     * Check if a password is a temporary password
     */
    public boolean isTemporaryPassword(String password) {
        return password != null && password.startsWith("SA2025-");
    }
}