package com.albany.restapi.config;

import com.albany.restapi.model.User;
import com.albany.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialsInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("info.albanyservice@gmail.com")) {
            User admin = User.builder()
                    .email("info.albanyservice@gmail.com")
                    .password(passwordEncoder.encode("admin@albany"))
                    .firstName("Arthur")
                    .lastName("Morgan")
                    .role(User.Role.admin)
                    .membershipType(User.MembershipType.STANDARD)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
            System.out.println("Admin user created successfully");
        }
    }
}