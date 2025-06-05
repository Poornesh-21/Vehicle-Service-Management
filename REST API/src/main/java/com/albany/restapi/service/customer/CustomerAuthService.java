package com.albany.restapi.service.customer;

import com.albany.restapi.dto.CustomerAuthResponse;
import com.albany.restapi.dto.CustomerRegistrationDTO;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.security.JwtUtil;
import com.albany.restapi.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthService.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // In-memory OTP storage (would use Redis or a database table in production)
    private final Map<String, OtpData> otpStore = new HashMap<>();

    // Scheduled executor for cleanup
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static class OtpData {
        String otp;
        LocalDateTime expiryTime;
        CustomerRegistrationDTO registrationData;

        OtpData(String otp, CustomerRegistrationDTO registrationData) {
            this.otp = otp;
            // 5-minute expiry
            this.expiryTime = LocalDateTime.now().plusMinutes(5);
            this.registrationData = registrationData;
        }

        boolean isValid(String otpToCheck) {
            return otp.equals(otpToCheck) && LocalDateTime.now().isBefore(expiryTime);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    /**
     * Send OTP for login
     */
    public void sendLoginOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email not registered. Please sign up first."));

        // Check if user is a customer
        if (user.getRole() != User.Role.customer) {
            throw new BadCredentialsException("This email is not registered as a customer.");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new BadCredentialsException("Your account is inactive. Please contact support.");
        }

        // Generate OTP
        String otp = generateOtp();

        // Store OTP
        otpStore.put(email, new OtpData(otp, null));

        // Schedule cleanup
        scheduleOtpCleanup(email);

        // Send OTP via email
        sendOtpEmail(email, otp, "Login");
    }

    /**
     * Verify OTP for login
     */
    public CustomerAuthResponse verifyLoginOtp(String email, String otp) {
        // Check if OTP exists and is valid
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            throw new BadCredentialsException("No OTP found. Please request a new OTP.");
        }

        if (otpData.isExpired()) {
            // Remove expired OTP
            otpStore.remove(email);
            throw new BadCredentialsException("OTP has expired. Please request a new OTP.");
        }

        if (!otpData.isValid(otp)) {
            throw new BadCredentialsException("Invalid OTP. Please try again.");
        }

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Check if user is a customer
        if (user.getRole() != User.Role.customer) {
            throw new BadCredentialsException("Email is not registered as a customer");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        // Remove OTP from store
        otpStore.remove(email);

        // Return authentication response
        return CustomerAuthResponse.builder()
                .success(true)
                .token(token)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .membershipType(user.getMembershipType().name())
                .redirectUrl("/customer") // Redirect to customer index page
                .message("Login successful")
                .build();
    }

    /**
     * Send OTP for registration
     */
    public void sendRegistrationOtp(CustomerRegistrationDTO registrationDTO) {
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already registered. Please login instead.");
        }

        // Validate registration data
        validateRegistrationData(registrationDTO);

        // Generate OTP
        String otp = generateOtp();

        // Store OTP with registration data
        otpStore.put(registrationDTO.getEmail(), new OtpData(otp, registrationDTO));

        // Schedule cleanup
        scheduleOtpCleanup(registrationDTO.getEmail());

        // Send OTP via email
        sendOtpEmail(registrationDTO.getEmail(), otp, "Registration");
    }

    /**
     * Verify OTP for registration
     */
    @Transactional
    public CustomerAuthResponse verifyRegistrationOtp(String email, String otp) {
        // Check if OTP exists and is valid
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            throw new BadCredentialsException("No OTP found. Please request a new OTP.");
        }

        if (otpData.isExpired()) {
            // Remove expired OTP
            otpStore.remove(email);
            throw new BadCredentialsException("OTP has expired. Please request a new OTP.");
        }

        if (!otpData.isValid(otp) || otpData.registrationData == null) {
            throw new BadCredentialsException("Invalid OTP. Please try again.");
        }

        // Create new user
        CustomerRegistrationDTO regData = otpData.registrationData;

        // Check if email is already taken (double check)
        if (userRepository.existsByEmail(regData.getEmail())) {
            throw new IllegalArgumentException("Email already registered. Please login instead.");
        }

        // Generate a random password (user will need to use OTP login)
        String password = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .email(regData.getEmail())
                .firstName(regData.getFirstName())
                .lastName(regData.getLastName())
                .phoneNumber(regData.getPhoneNumber())
                .password(passwordEncoder.encode(password))
                .role(User.Role.customer)
                .membershipType(User.MembershipType.STANDARD)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        // Create customer profile
        CustomerProfile profile = CustomerProfile.builder()
                .user(user)
                .membershipStatus("STANDARD")
                .totalServices(0)
                .build();

        customerRepository.save(profile);

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        // Remove OTP from store
        otpStore.remove(email);

        // Send welcome email
        sendWelcomeEmail(user);

        // Return authentication response
        return CustomerAuthResponse.builder()
                .success(true)
                .token(token)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .membershipType(user.getMembershipType().name())
                .redirectUrl("/customer") // Redirect to customer index page
                .message("Registration successful")
                .build();
    }

    /**
     * Validate customer token
     */
    public User validateCustomerToken(String username, String token) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if (user.getRole() != User.Role.customer) {
            throw new BadCredentialsException("User is not a customer");
        }

        if (!jwtUtil.validateToken(token, user)) {
            throw new BadCredentialsException("Invalid token");
        }

        return user;
    }

    /**
     * Generate 4-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 1000 + random.nextInt(9000); // 4-digit OTP
        return String.valueOf(otp);
    }

    /**
     * Schedule OTP cleanup
     */
    private void scheduleOtpCleanup(String email) {
        scheduler.schedule(() -> {
            OtpData data = otpStore.get(email);
            if (data != null && data.isExpired()) {
                otpStore.remove(email);
                logger.info("Expired OTP removed for: {}", email);
            }
        }, 5, TimeUnit.MINUTES);
    }

    private void sendOtpEmail(String email, String otp, String action) {
        String subject = "Albany Vehicle Service - Your " + action + " OTP";
        String content = buildOtpEmailTemplate(otp, action);
        emailService.sendSimpleEmail(email, subject, content);
        logger.info("{} OTP sent to: {}", action, email);
    }

    /**
     * Build OTP email template
     */
    private String buildOtpEmailTemplate(String otp, String action) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>"
                + "<div style='text-align: center; background-color: #722F37; color: white; padding: 10px;'>"
                + "<h2>Albany Vehicle Service System</h2>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p>Hello,</p>"
                + "<p>Your " + action + " One-Time Password (OTP) is:</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; text-align: center;'>"
                + "<h2 style='font-family: monospace; letter-spacing: 5px;'>" + otp + "</h2>"
                + "</div>"
                + "<p>This OTP is valid for 5 minutes. Please do not share this with anyone.</p>"
                + "<p>If you did not request this OTP, please ignore this email.</p>"
                + "<p>Thank you,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>";
    }

    /**
     * Send welcome email to new user
     */
    private void sendWelcomeEmail(User user) {
        String subject = "Welcome to Albany Vehicle Service";
        String content = buildWelcomeEmailTemplate(user.getFirstName(), user.getLastName());
        emailService.sendSimpleEmail(user.getEmail(), subject, content);
    }

    /**
     * Build welcome email template
     */
    private String buildWelcomeEmailTemplate(String firstName, String lastName) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>"
                + "<div style='text-align: center; background-color: #722F37; color: white; padding: 10px;'>"
                + "<h2>Albany Vehicle Service System</h2>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p>Hello " + firstName + " " + lastName + ",</p>"
                + "<p>Welcome to Albany Vehicle Service! Your account has been created successfully.</p>"
                + "<p>With your new account, you can:</p>"
                + "<ul>"
                + "<li>Book vehicle services</li>"
                + "<li>Track service status in real-time</li>"
                + "<li>Manage your vehicles</li>"
                + "<li>View service history</li>"
                + "<li>And much more!</li>"
                + "</ul>"
                + "<p>Thank you for choosing Albany Vehicle Service for your vehicle maintenance needs.</p>"
                + "<p>Best regards,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>© " + java.time.Year.now().getValue() + " Albany Vehicle Service. All rights reserved.</p>"
                + "</div>"
                + "</div>";
    }

    /**
     * Validate registration data
     */
    private void validateRegistrationData(CustomerRegistrationDTO data) {
        if (data.getFirstName() == null || data.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (data.getLastName() == null || data.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (data.getEmail() == null || data.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!data.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }

        if (data.getPhoneNumber() == null || data.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (!data.getPhoneNumber().matches("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$")) {
            throw new IllegalArgumentException("Please enter a valid phone number");
        }
    }
}