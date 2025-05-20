package com.albany.restapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    /**
     * Send a simple text email
     * @param to recipient email address
     * @param subject email subject
     * @param text email content
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // true = HTML content

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send service advisor credentials email
     * @param email recipient email address
     * @param firstName advisor's first name
     * @param lastName advisor's last name
     * @param password temporary password
     * @param isNewAccount whether this is a new account or password reset
     */
    @Async
    public void sendServiceAdvisorCredentials(String email, String firstName, String lastName,
                                              String password, boolean isNewAccount) {
        String subject = isNewAccount ?
                "Welcome to Albany Service - Your Account Credentials" :
                "Albany Service - Your Password Has Been Reset";

        String content = buildServiceAdvisorEmailTemplate(firstName, lastName, email, password, isNewAccount);

        sendSimpleEmail(email, subject, content);
    }

    /**
     * Build HTML email template for service advisor credentials
     */
    private String buildServiceAdvisorEmailTemplate(String firstName, String lastName, String email,
                                                    String password, boolean isNewAccount) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>"
                + "<div style='text-align: center; background-color: #0056b3; color: white; padding: 10px;'>"
                + "<h2>Albany Vehicle Service System</h2>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p>Hello " + firstName + " " + lastName + ",</p>"
                + "<p>" + (isNewAccount ?
                "Your account has been created successfully as a Service Advisor." :
                "Your password has been reset by an administrator.") + "</p>"
                + "<p>Please use the following credentials to login to the system:</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;'>"
                + "<p><strong>Email:</strong> " + "<span style='font-family: monospace;'>" + email + "</span></p>"
                + "<p><strong>Temporary Password:</strong> " + "<span style='font-family: monospace;'>" + password + "</span></p>"
                + "</div>"
                + "<p style='color: #dc3545;'><strong>Important:</strong> Please change your password after the first login for security purposes.</p>"
                + "<p>If you have any questions, please contact the administrator.</p>"
                + "<p>Thank you,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>";
    }
}