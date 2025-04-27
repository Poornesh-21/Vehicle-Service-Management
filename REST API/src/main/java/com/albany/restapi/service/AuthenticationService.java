package com.albany.restapi.service;

import com.albany.restapi.dto.AuthenticationRequest;
import com.albany.restapi.dto.AuthenticationResponse;
import com.albany.restapi.dto.PasswordChangeRequest;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            String jwtToken = jwtUtil.generateToken(user);

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .build();

        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid email/password combination");
        }
    }

    /**
     * Change user password
     */
    @Transactional
    public AuthenticationResponse changePassword(PasswordChangeRequest request, String userEmail) {
        // If no email is provided, use the currently authenticated user
        if (userEmail == null || userEmail.isEmpty()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                userEmail = ((User) authentication.getPrincipal()).getEmail();
            } else {
                throw new BadCredentialsException("User not authenticated");
            }
        }

        // Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // For temporary password change, we don't need to validate the current password
        if (!request.isTemporaryPassword()) {
            // Verify current password
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(userEmail, request.getCurrentPassword())
                );
            } catch (AuthenticationException e) {
                throw new BadCredentialsException("Current password is incorrect");
            }
        }

        // Verify that the new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadCredentialsException("New password and confirmation do not match");
        }

        // Update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Send email notification
        try {
            sendPasswordChangeEmail(user);
        } catch (Exception e) {
            // Log but don't fail if email sending fails
            System.err.println("Failed to send password change notification: " + e.getMessage());
        }

        // Generate a new token with updated credentials
        String jwtToken = jwtUtil.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Send a password change notification email
     */
    private void sendPasswordChangeEmail(User user) {
        String subject = "Albany Service - Password Changed";
        String content = "Dear " + user.getFirstName() + " " + user.getLastName() + ",\n\n" +
                "Your password for the Albany Vehicle Service Management System has been successfully changed.\n\n" +
                "If you did not make this change, please contact the system administrator immediately.\n\n" +
                "Best regards,\n" +
                "Albany Service Team";

        emailService.sendSimpleEmail(user.getEmail(), subject, content);
    }
}