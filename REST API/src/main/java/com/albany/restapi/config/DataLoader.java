package com.albany.restapi.config;

import com.albany.restapi.model.Role;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Initialize admin user if not exists
        if (!userRepository.existsByEmail("info.albanyservice@gmail.com")) {
            User admin = User.builder()
                    .email("info.albanyservice@gmail.com")
                    .password(passwordEncoder.encode("admin@albany"))
                    .firstName("Arthur")
                    .lastName("Morgan")
                    .role(Role.admin)
                    .isActive(true)
                    .build();
            
            userRepository.save(admin);
            System.out.println("Admin user created successfully");
        }
    }
}