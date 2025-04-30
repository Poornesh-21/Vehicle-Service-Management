package com.albany.restapi.service;

import com.albany.restapi.dto.MechanicRequest;
import com.albany.restapi.dto.MechanicResponse;
import com.albany.restapi.model.MechanicProfile;
import com.albany.restapi.model.Role;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.MechanicProfileRepository;
import com.albany.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MechanicService {

    private final MechanicProfileRepository mechanicRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public MechanicResponse createMechanic(MechanicRequest request) {
        // Create user first
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.mechanic)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Create mechanic profile
        MechanicProfile profile = MechanicProfile.builder()
                .user(user)
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .experienceYears(request.getExperienceYears())
                .hireDate(LocalDate.now())
                .build();

        profile = mechanicRepository.save(profile);

        // Send email with login credentials if email service is available
        try {
            emailService.sendPasswordEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName(),
                    request.getPassword() // Use the plain text password from the request
            );
            log.info("Password email sent to new mechanic: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password email to {}: {}", user.getEmail(), e.getMessage(), e);
            // Continue with the transaction even if email fails
        }

        return mapToResponse(profile);
    }

    public List<MechanicResponse> getAllMechanics() {
        return mechanicRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MechanicResponse getMechanicById(Integer id) {
        MechanicProfile profile = mechanicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mechanic not found"));

        return mapToResponse(profile);
    }

    @Transactional
    public MechanicResponse updateMechanic(Integer id, MechanicRequest request) {
        MechanicProfile profile = mechanicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mechanic not found"));

        User user = profile.getUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        // Update password only if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            // Send email with new password
            try {
                emailService.sendPasswordEmail(
                        user.getEmail(),
                        user.getFirstName() + " " + user.getLastName(),
                        request.getPassword()
                );
                log.info("Password reset email sent to mechanic: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage(), e);
                // Continue with the update even if email fails
            }
        }

        userRepository.save(user);

        profile.setDepartment(request.getDepartment());
        profile.setSpecialization(request.getSpecialization());
        profile.setExperienceYears(request.getExperienceYears());
        profile = mechanicRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional
    public void deleteMechanic(Integer id) {
        MechanicProfile profile = mechanicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mechanic not found"));

        // Soft delete - mark as inactive
        User user = profile.getUser();
        user.setActive(false);
        userRepository.save(user);
    }

    private MechanicResponse mapToResponse(MechanicProfile profile) {
        User user = profile.getUser();

        return MechanicResponse.builder()
                .mechanicId(profile.getMechanicId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .department(profile.getDepartment())
                .specialization(profile.getSpecialization())
                .experienceYears(profile.getExperienceYears())
                .hireDate(profile.getHireDate())
                .formattedId(profile.getFormattedId())
                .isActive(user.isActive())
                .build();
    }
}