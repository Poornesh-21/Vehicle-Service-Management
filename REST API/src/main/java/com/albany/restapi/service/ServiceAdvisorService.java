package com.albany.restapi.service;

import com.albany.restapi.dto.ServiceAdvisorRequest;
import com.albany.restapi.dto.ServiceAdvisorResponse;
import com.albany.restapi.model.Role;
import com.albany.restapi.model.ServiceAdvisorProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.ServiceAdvisorProfileRepository;
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
public class ServiceAdvisorService {

    private final ServiceAdvisorProfileRepository advisorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public ServiceAdvisorResponse createServiceAdvisor(ServiceAdvisorRequest request) {
        // Create user first
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.serviceAdvisor)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Create service advisor profile
        ServiceAdvisorProfile profile = ServiceAdvisorProfile.builder()
                .user(user)
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .hireDate(LocalDate.now())
                .build();

        profile = advisorRepository.save(profile);

        // Send email with login credentials
        try {
            emailService.sendPasswordEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName(),
                    request.getPassword() // Use the plain text password from the request
            );
            log.info("Password email sent to new service advisor: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password email to {}: {}", user.getEmail(), e.getMessage(), e);
            // Continue with the transaction even if email fails
        }

        return mapToResponse(profile);
    }

    public List<ServiceAdvisorResponse> getAllServiceAdvisors() {
        return advisorRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ServiceAdvisorResponse getServiceAdvisorById(Integer id) {
        ServiceAdvisorProfile profile = advisorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Advisor not found"));

        return mapToResponse(profile);
    }

    @Transactional
    public ServiceAdvisorResponse updateServiceAdvisor(Integer id, ServiceAdvisorRequest request) {
        ServiceAdvisorProfile profile = advisorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Advisor not found"));

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
                log.info("Password reset email sent to service advisor: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage(), e);
                // Continue with the update even if email fails
            }
        }

        userRepository.save(user);

        profile.setDepartment(request.getDepartment());
        profile.setSpecialization(request.getSpecialization());
        profile = advisorRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional
    public void deleteServiceAdvisor(Integer id) {
        ServiceAdvisorProfile profile = advisorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Advisor not found"));

        // Soft delete - mark as inactive
        User user = profile.getUser();
        user.setActive(false);
        userRepository.save(user);
    }

    private ServiceAdvisorResponse mapToResponse(ServiceAdvisorProfile profile) {
        User user = profile.getUser();

        // Calculate active services and workload percentage (dummy values for now)
        // In a real system, we would query from service requests table
        int activeServices = getRandomNumber(0, 8);  // For demo purposes
        int workloadPercentage = calculateWorkload(activeServices);

        return ServiceAdvisorResponse.builder()
                .advisorId(profile.getAdvisorId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .department(profile.getDepartment())
                .specialization(profile.getSpecialization())
                .hireDate(profile.getHireDate())
                .formattedId(profile.getFormattedId())
                .isActive(user.isActive())
                .activeServices(activeServices)
                .workloadPercentage(workloadPercentage)
                .build();
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private int calculateWorkload(int activeServices) {
        // Simple workload calculation (each service is about 25% load)
        return Math.min(activeServices * 25, 100);
    }
}