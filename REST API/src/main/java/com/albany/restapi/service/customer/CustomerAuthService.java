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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthService.class);
    
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    
    // In-memory OTP storage (in production, use Redis or another cache solution)
    private final Map<String, OtpData> otpMap = new HashMap<>();
    
    /**
     * Send OTP for login
     */
    public CustomerAuthResponse sendLoginOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        if (user.getRole() != User.Role.customer) {
            throw new BadCredentialsException("This login is only for customers");
        }
        
        // Generate and store OTP
        String otp = generateOtp();
        otpMap.put(email, new OtpData(otp, false, LocalDateTime.now()));
        
        // Send OTP email
        sendOtpEmail(email, user.getFirstName(), otp, "Login");
        
        return CustomerAuthResponse.builder()
                .success(true)
                .message("OTP sent successfully")
                .build();
    }
    
    /**
     * Verify OTP for login
     */
    public CustomerAuthResponse verifyLoginOtp(String email, String otp) {
        // Validate OTP
        validateOtp(email, otp);
        
        // Mark OTP as used
        otpMap.get(email).setUsed(true);
        
        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user);
        
        return CustomerAuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .membershipType(user.getMembershipType().name())
                .success(true)
                .message("Login successful")
                .redirectUrl("/customer/bookService")
                .build();
    }
    
    /**
     * Send OTP for registration
     */
    public CustomerAuthResponse sendRegistrationOtp(CustomerRegistrationDTO registrationDTO) {
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Validate email format
        if (!isValidEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Validate phone number
        if (!isValidPhone(registrationDTO.getPhoneNumber())) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        
        // Generate and store OTP
        String otp = generateOtp();
        otpMap.put(registrationDTO.getEmail(), new OtpData(otp, false, LocalDateTime.now(), registrationDTO));
        
        // Send OTP email
        sendOtpEmail(registrationDTO.getEmail(), registrationDTO.getFirstName(), otp, "Registration");
        
        return CustomerAuthResponse.builder()
                .success(true)
                .message("OTP sent successfully")
                .build();
    }
    
    /**
     * Verify OTP for registration
     */
    @Transactional
    public CustomerAuthResponse verifyRegistrationOtp(CustomerRegistrationDTO registrationDTO, String otp) {
        String email = registrationDTO.getEmail();
        
        // Validate OTP
        validateOtp(email, otp);
        
        // Get stored registration data
        OtpData otpData = otpMap.get(email);
        CustomerRegistrationDTO storedData = otpData.getRegistrationData();
        
        // Mark OTP as used
        otpData.setUsed(true);
        
        // Create user
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("temp-pass"))  // Temporary password, will be updated in next step
                .firstName(storedData.getFirstName())
                .lastName(storedData.getLastName())
                .phoneNumber(storedData.getPhoneNumber())
                .role(User.Role.customer)
                .membershipType(User.MembershipType.STANDARD)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Create customer profile
        CustomerProfile customerProfile = CustomerProfile.builder()
                .user(savedUser)
                .membershipStatus(User.MembershipType.STANDARD.name())
                .totalServices(0)
                .build();
        
        customerRepository.save(customerProfile);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);
        
        // Send welcome email
        sendWelcomeEmail(savedUser);
        
        return CustomerAuthResponse.builder()
                .token(token)
                .role(savedUser.getRole().name())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .membershipType(savedUser.getMembershipType().name())
                .success(true)
                .message("Registration successful")
                .redirectUrl("/customer/bookService")
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
        int otp = 1000 + random.nextInt(9000);
        return String.valueOf(otp);
    }
    
    /**
     * Validate OTP
     */
    private void validateOtp(String email, String otp) {
        OtpData otpData = otpMap.get(email);
        
        if (otpData == null) {
            throw new IllegalArgumentException("No OTP request found for this email");
        }
        
        if (otpData.isUsed()) {
            throw new IllegalArgumentException("OTP has already been used");
        }
        
        if (otpData.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }
        
        if (!otpData.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }
    }
    
    /**
     * Send OTP email
     */
    private void sendOtpEmail(String email, String firstName, String otp, String purpose) {
        String subject = "Albany Vehicle Service - Your OTP for " + purpose;
        String content = buildOtpEmailTemplate(firstName, otp, purpose);
        emailService.sendSimpleEmail(email, subject, content);
        logger.info("OTP email sent to: {}", email);
    }
    
    /**
     * Send welcome email
     */
    private void sendWelcomeEmail(User user) {
        String subject = "Welcome to Albany Vehicle Service";
        String content = buildWelcomeEmailTemplate(user.getFirstName(), user.getLastName());
        emailService.sendSimpleEmail(user.getEmail(), subject, content);
        logger.info("Welcome email sent to: {}", user.getEmail());
    }
    
    /**
     * Build OTP email template
     */
    private String buildOtpEmailTemplate(String firstName, String otp, String purpose) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>"
                + "<div style='text-align: center; background-color: #722F37; color: white; padding: 10px;'>"
                + "<h2>Albany Vehicle Service System</h2>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p>Hello " + firstName + ",</p>"
                + "<p>Your OTP for " + purpose + " is:</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; text-align: center;'>"
                + "<h2 style='color: #722F37; font-size: 32px; letter-spacing: 5px;'>" + otp + "</h2>"
                + "</div>"
                + "<p>This OTP will expire in 5 minutes for security reasons.</p>"
                + "<p>If you did not request this OTP, please ignore this email.</p>"
                + "<p>Thank you,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>";
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
                + "<p>You can now book services, track service status, and manage your vehicles through our platform.</p>"
                + "<p>We're excited to have you join our community of satisfied customers.</p>"
                + "<p>Thank you,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>";
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }
    
    /**
     * Validate phone format
     */
    private boolean isValidPhone(String phone) {
        String phoneRegex = "^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$";
        return Pattern.compile(phoneRegex).matcher(phone).matches();
    }
    
    /**
     * Inner class for OTP data
     */
    private static class OtpData {
        private final String otp;
        private boolean used;
        private final LocalDateTime createdAt;
        private final CustomerRegistrationDTO registrationData;
        
        public OtpData(String otp, boolean used, LocalDateTime createdAt) {
            this.otp = otp;
            this.used = used;
            this.createdAt = createdAt;
            this.registrationData = null;
        }
        
        public OtpData(String otp, boolean used, LocalDateTime createdAt, CustomerRegistrationDTO registrationData) {
            this.otp = otp;
            this.used = used;
            this.createdAt = createdAt;
            this.registrationData = registrationData;
        }
        
        public String getOtp() {
            return otp;
        }
        
        public boolean isUsed() {
            return used;
        }
        
        public void setUsed(boolean used) {
            this.used = used;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public CustomerRegistrationDTO getRegistrationData() {
            return registrationData;
        }
    }
}