package com.albany.restapi.service.admin;

import com.albany.restapi.dto.ServiceAdvisorDTO;
import com.albany.restapi.model.ServiceAdvisorProfile;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.ServiceAdvisorRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAdvisorService {

    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Get all service advisors with their workload information
     */
    public List<ServiceAdvisorDTO> getAllServiceAdvisors() {
        List<ServiceAdvisorProfile> advisors = serviceAdvisorRepository.findAllActiveAdvisors();

        // Get active request counts for each advisor
        Map<Integer, Long> advisorActiveServices = serviceRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() != ServiceRequest.Status.Completed && request.getServiceAdvisor() != null)
                .collect(Collectors.groupingBy(
                        request -> request.getServiceAdvisor().getAdvisorId(),
                        Collectors.counting()
                ));

        return advisors.stream()
                .map(advisor -> mapToDTO(advisor, advisorActiveServices))
                .collect(Collectors.toList());
    }

    /**
     * Get a service advisor by ID with workload information
     */
    public ServiceAdvisorDTO getServiceAdvisorById(Integer advisorId) {
        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                .orElseThrow(() -> new EntityNotFoundException("Service advisor not found with id: " + advisorId));

        long activeServices = serviceRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() != ServiceRequest.Status.Completed
                        && request.getServiceAdvisor() != null
                        && request.getServiceAdvisor().getAdvisorId().equals(advisorId))
                .count();

        Map<Integer, Long> advisorActiveServices = Map.of(advisorId, activeServices);

        return mapToDTO(advisor, advisorActiveServices);
    }

    /**
     * Create a new service advisor
     */
    @Transactional
    public ServiceAdvisorDTO createServiceAdvisor(ServiceAdvisorDTO advisorDTO) {
        // Check if email already exists
        if (userRepository.existsByEmail(advisorDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + advisorDTO.getEmail());
        }

        // Generate random password if not provided
        String password = advisorDTO.getPassword();
        if (password == null || password.isEmpty()) {
            password = generateRandomPassword();
        }

        // Create user
        User user = User.builder()
                .email(advisorDTO.getEmail())
                .password(passwordEncoder.encode(password))
                .firstName(advisorDTO.getFirstName())
                .lastName(advisorDTO.getLastName())
                .phoneNumber(advisorDTO.getPhoneNumber())
                .role(User.Role.serviceAdvisor)
                .isActive(true)
                .membershipType(User.MembershipType.STANDARD)
                .build();

        User savedUser = userRepository.save(user);

        // Create service advisor profile
        ServiceAdvisorProfile advisor = ServiceAdvisorProfile.builder()
                .user(savedUser)
                .department(advisorDTO.getDepartment())
                .specialization(advisorDTO.getSpecialization())
                .hireDate(LocalDate.now())
                .build();

        ServiceAdvisorProfile savedAdvisor = serviceAdvisorRepository.save(advisor);

        // Send email with credentials
        emailService.sendServiceAdvisorCredentials(
                advisorDTO.getEmail(),
                advisorDTO.getFirstName(),
                advisorDTO.getLastName(),
                password,
                true // new account
        );

        // Return the newly created advisor
        return mapToDTO(savedAdvisor, Map.of(savedAdvisor.getAdvisorId(), 0L));
    }

    /**
     * Update an existing service advisor
     */
    @Transactional
    public ServiceAdvisorDTO updateServiceAdvisor(Integer advisorId, ServiceAdvisorDTO advisorDTO) {
        ServiceAdvisorProfile existingAdvisor = serviceAdvisorRepository.findById(advisorId)
                .orElseThrow(() -> new EntityNotFoundException("Service advisor not found with id: " + advisorId));

        User existingUser = existingAdvisor.getUser();

        // Check if email is being changed and if it already exists
        if (!existingUser.getEmail().equals(advisorDTO.getEmail()) &&
                userRepository.existsByEmail(advisorDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + advisorDTO.getEmail());
        }

        // Update user details
        existingUser.setFirstName(advisorDTO.getFirstName());
        existingUser.setLastName(advisorDTO.getLastName());
        existingUser.setEmail(advisorDTO.getEmail());
        existingUser.setPhoneNumber(advisorDTO.getPhoneNumber());

        // Check if password is being reset
        boolean passwordReset = false;
        String newPassword = advisorDTO.getPassword();
        if (newPassword != null && !newPassword.isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(newPassword));
            passwordReset = true;
        }

        userRepository.save(existingUser);

        // Update advisor details
        existingAdvisor.setDepartment(advisorDTO.getDepartment());
        existingAdvisor.setSpecialization(advisorDTO.getSpecialization());

        ServiceAdvisorProfile updatedAdvisor = serviceAdvisorRepository.save(existingAdvisor);

        // Send email with new password if password was reset
        if (passwordReset) {
            emailService.sendServiceAdvisorCredentials(
                    existingUser.getEmail(),
                    existingUser.getFirstName(),
                    existingUser.getLastName(),
                    newPassword,
                    false // password reset
            );
        }

        // Get active services count
        long activeServices = serviceRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() != ServiceRequest.Status.Completed
                        && request.getServiceAdvisor() != null
                        && request.getServiceAdvisor().getAdvisorId().equals(advisorId))
                .count();

        Map<Integer, Long> advisorActiveServices = Map.of(advisorId, activeServices);

        return mapToDTO(updatedAdvisor, advisorActiveServices);
    }

    /**
     * Generate a random password for new service advisors
     */
    private String generateRandomPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String numbers = "123456789";

        StringBuilder password = new StringBuilder("SA2025-");

        // Add 3 random letters
        for (int i = 0; i < 3; i++) {
            password.append(letters.charAt((int) (Math.random() * letters.length())));
        }

        // Add 3 random numbers
        for (int i = 0; i < 3; i++) {
            password.append(numbers.charAt((int) (Math.random() * numbers.length())));
        }

        return password.toString();
    }

    /**
     * Map an entity to DTO with workload information
     */
    private ServiceAdvisorDTO mapToDTO(ServiceAdvisorProfile advisor, Map<Integer, Long> advisorActiveServices) {
        User user = advisor.getUser();

        // Calculate workload percentage based on active services
        long activeServices = advisorActiveServices.getOrDefault(advisor.getAdvisorId(), 0L);
        int workloadPercentage = calculateWorkloadPercentage(activeServices);

        return ServiceAdvisorDTO.builder()
                .advisorId(advisor.getAdvisorId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .department(advisor.getDepartment())
                .specialization(advisor.getSpecialization())
                .hireDate(advisor.getHireDate())
                .formattedId("SA-" + advisor.getAdvisorId())
                .workloadPercentage(workloadPercentage)
                .activeServices((int)activeServices)
                .build();
    }

    private int calculateWorkloadPercentage(long activeServices) {
        if (activeServices >= 8) return 100;
        return (int) (activeServices * 12.5); // 12.5% per service (100/8)
    }
}