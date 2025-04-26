package com.albany.restapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send an HTML email with a password
     */
    public void sendPasswordEmail(String toEmail, String name, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Albany Service - Your Account Credentials");

            String emailContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <div style="background-color: #722F37; color: white; padding: 15px; text-align: center; border-radius: 5px 5px 0 0;">
                            <h2>Welcome to Albany Vehicle Service Management</h2>
                        </div>
                        <div style="padding: 20px;">
                            <p>Dear %s,</p>
                            <p>Your service advisor account has been created. Please use the following credentials to log in to the system:</p>
                            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                                <p><strong>Email:</strong> %s</p>
                                <p><strong>Temporary Password:</strong> %s</p>
                            </div>
                            <p>For security reasons, please change your password after your first login.</p>
                            <p>If you have any questions or need assistance, please contact the administrator.</p>
                            <p>Thank you,<br>Albany Service Team</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, toEmail, password);

            helper.setText(emailContent, true); // true indicates HTML content

            mailSender.send(message);
            log.info("Password email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password email", e);
        }
    }

    /**
     * Send a bill email with PDF attachment
     */
    public void sendBillEmail(String toEmail, String subject, String content, byte[] pdfAttachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // true indicates HTML content

            // Add PDF attachment
            helper.addAttachment("service_bill.pdf", new org.springframework.core.io.ByteArrayResource(pdfAttachment));

            mailSender.send(message);
            log.info("Bill email with attachment sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send bill email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send bill email", e);
        }
    }
}