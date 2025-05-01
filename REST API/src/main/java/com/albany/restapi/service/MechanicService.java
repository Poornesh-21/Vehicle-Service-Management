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

    /**
     * Get all mechanics
     */
    public List<MechanicResponse> getAllMechanics() {
        log.debug("Getting all active mechanics");
        try {
            List<MechanicProfile> mechanics = mechanicRepository.findAllActive();
            log.debug("Found {} active mechanics", mechanics.size());
            return mechanics.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting mechanics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get mechanics", e);
        }
    }

    /**
     * Get mechanic by ID
     */
    public MechanicResponse getMechanicById(Integer id) {
        log.debug("Getting mechanic with ID: {}", id);
        try {
            MechanicProfile profile = mechanicRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mechanic not found with ID: " + id));

            return mapToResponse(profile);
        } catch (Exception e) {
            log.error("Error getting mechanic with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get mechanic: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new mechanic
     */
    @Transactional
    public MechanicResponse createMechanic(MechanicRequest request) {
        log.info("Creating mechanic with email: {}", request.getEmail());

        // Validation checks
        if (request.getEmail() == null || request.getFirstName() == null || request.getLastName() == null) {
            throw new IllegalArgumentException("Email, first name, and last name are required");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            request.setPassword(generateRandomPassword());
        }

        try {
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

            // Save and FLUSH the user to ensure it's committed to the database
            user = userRepository.saveAndFlush(user);
            log.debug("Created and flushed user entity with ID: {}", user.getUserId());

            // Create mechanic profile
            MechanicProfile profile = MechanicProfile.builder()
                    .user(user)
                    .department(request.getDepartment())
                    .specialization(request.getSpecialization())
                    .experienceYears(request.getExperienceYears())
                    .hireDate(LocalDate.now())
                    .build();

            profile = mechanicRepository.save(profile);
            log.debug("Created mechanic profile with ID: {}", profile.getMechanicId());

            // Email sending
            try {
                emailService.sendPasswordEmail(
                        user.getEmail(),
                        user.getFirstName() + " " + user.getLastName(),
                        request.getPassword()
                );
                log.info("Password email sent to new mechanic: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send password email to {}: {}", user.getEmail(), e.getMessage(), e);
                // Continue with the transaction even if email fails
            }

            return mapToResponse(profile);
        } catch (Exception e) {
            log.error("Error creating mechanic: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create mechanic: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing mechanic
     */
    @Transactional
    public MechanicResponse updateMechanic(Integer id, MechanicRequest request) {
        log.debug("Updating mechanic with ID: {}", id);
        try {
            MechanicProfile profile = mechanicRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mechanic not found with ID: " + id));

            User user = profile.getUser();

            // Check email uniqueness if changing email
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("A user with this email already exists");
            }

            // Update user fields
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

            // Update mechanic profile fields
            profile.setDepartment(request.getDepartment());
            profile.setSpecialization(request.getSpecialization());
            profile.setExperienceYears(request.getExperienceYears());
            profile = mechanicRepository.save(profile);

            return mapToResponse(profile);
        } catch (Exception e) {
            log.error("Error updating mechanic with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update mechanic: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a mechanic (soft delete)
     */
    @Transactional
    public void deleteMechanic(Integer id) {
        log.debug("Deleting mechanic with ID: {}", id);
        try {
            MechanicProfile profile = mechanicRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mechanic not found with ID: " + id));

            // Soft delete - mark as inactive
            User user = profile.getUser();
            user.setActive(false);
            userRepository.save(user);

            log.info("Mechanic with ID {} has been soft-deleted", id);
        } catch (Exception e) {
            log.error("Error deleting mechanic with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete mechanic: " + e.getMessage(), e);
        }
    }

    private String generateRandomPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excluded I and O to avoid confusion
        final String numbers = "123456789"; // Excluded 0 to avoid confusion with O

        StringBuilder password = new StringBuilder("MECH2025-");

        // Add 3 random letters
        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * letters.length());
            password.append(letters.charAt(index));
        }

        // Add 3 random numbers
        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * numbers.length());
            password.append(numbers.charAt(index));
        }

        return password.toString();
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