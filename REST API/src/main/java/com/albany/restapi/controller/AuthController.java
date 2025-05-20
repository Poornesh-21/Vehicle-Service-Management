package com.albany.restapi.controller;

import com.albany.restapi.dto.AuthRequest;
import com.albany.restapi.dto.AuthResponse;
import com.albany.restapi.model.User;
import com.albany.restapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
            
            User user = (User) authentication.getPrincipal();

            if (user.getRole() != User.Role.admin) {
                return ResponseEntity.status(403).body(Map.of("message", "Access denied. Only admin can login here."));
            }
            
            String token = jwtUtil.generateToken(user);
            
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .role(user.getRole().name())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .build());
                    
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email/password combination"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(Map.of("message", "User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Authentication failed: " + e.getMessage()));
        }
    }
}