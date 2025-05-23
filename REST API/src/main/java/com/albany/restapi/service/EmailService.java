package com.albany.restapi.service;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.model.Invoice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

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
     * Send invoice email to customer
     * @param email recipient email address
     * @param customerName customer's name
     * @param serviceDTO completed service details
     * @param invoice invoice entity
     */
    @Async
    public void sendInvoiceEmail(String email, String customerName, CompletedServiceDTO serviceDTO, Invoice invoice) {
        String subject = "Albany Service - Your Invoice #" + invoice.getInvoiceId();
        String content = buildInvoiceEmailTemplate(customerName, serviceDTO, invoice);
        sendSimpleEmail(email, subject, content);
        logger.info("Invoice email sent to: {}, Invoice #: {}", email, invoice.getInvoiceId());
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

    /**
     * Build HTML email template for customer invoice
     */
    private String buildInvoiceEmailTemplate(String customerName, CompletedServiceDTO serviceDTO, Invoice invoice) {
        StringBuilder materialsHtml = new StringBuilder();
        List<MaterialItemDTO> materials = serviceDTO.getMaterials();

        if (materials != null && !materials.isEmpty()) {
            materialsHtml.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 15px;'>");
            materialsHtml.append("<tr style='background-color: #f3f3f3;'>");
            materialsHtml.append("<th style='padding: 8px; text-align: left; border-bottom: 1px solid #ddd;'>Item</th>");
            materialsHtml.append("<th style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>Quantity</th>");
            materialsHtml.append("<th style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>Unit Price</th>");
            materialsHtml.append("<th style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>Total</th>");
            materialsHtml.append("</tr>");

            for (MaterialItemDTO material : materials) {
                if (material == null) continue;

                String itemName = material.getName() != null ? material.getName() : "Unknown Item";
                BigDecimal quantity = material.getQuantity() != null ? material.getQuantity() : BigDecimal.ONE;
                BigDecimal unitPrice = material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal total = quantity.multiply(unitPrice);

                materialsHtml.append("<tr>");
                materialsHtml.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(itemName).append("</td>");
                materialsHtml.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>").append(quantity).append("</td>");
                materialsHtml.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>₹").append(formatDecimal(unitPrice)).append("</td>");
                materialsHtml.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>₹").append(formatDecimal(total)).append("</td>");
                materialsHtml.append("</tr>");
            }

            materialsHtml.append("</table>");
        } else {
            materialsHtml.append("<p>No materials used in this service.</p>");
        }

        // Labor charges section
        StringBuilder laborHtml = new StringBuilder();
        List<LaborChargeDTO> laborCharges = serviceDTO.getLaborCharges();

        if (laborCharges != null && !laborCharges.isEmpty()) {
            laborHtml.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 15px;'>");
            laborHtml.append("<tr style='background-color: #f3f3f3;'>");
            laborHtml.append("<th style='padding: 8px; text-align: left; border-bottom: 1px solid #ddd;'>Description</th>");
            laborHtml.append("<th style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>Hours</th>");
            laborHtml.append("<th style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>Rate</th>");
            laborHtml.append("<th style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>Total</th>");
            laborHtml.append("</tr>");

            for (LaborChargeDTO labor : laborCharges) {
                if (labor == null) continue;

                String description = labor.getDescription() != null ? labor.getDescription() : "Service Labor";
                BigDecimal hours = labor.getHours() != null ? labor.getHours() : BigDecimal.ZERO;
                BigDecimal rate = labor.getRate() != null ? labor.getRate() : BigDecimal.ZERO;
                BigDecimal total = hours.multiply(rate);

                laborHtml.append("<tr>");
                laborHtml.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(description).append("</td>");
                laborHtml.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>").append(formatDecimal(hours)).append("</td>");
                laborHtml.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>₹").append(formatDecimal(rate)).append("/hr</td>");
                laborHtml.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #ddd;'>₹").append(formatDecimal(total)).append("</td>");
                laborHtml.append("</tr>");
            }

            laborHtml.append("</table>");
        } else {
            laborHtml.append("<p>No labor charges for this service.</p>");
        }

        // Premium discount logic
        String discountHtml = "";
        if (serviceDTO.getCalculatedDiscount() != null && serviceDTO.getCalculatedDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml = "<tr>" +
                    "<td style='padding: 8px; text-align: right;' colspan='3'><strong>Premium Discount (30% off labor):</strong></td>" +
                    "<td style='padding: 8px; text-align: right;'>-₹" + formatDecimal(serviceDTO.getCalculatedDiscount()) + "</td>" +
                    "</tr>";
        }

        // Format dates
        String completionDate = serviceDTO.getCompletionDate() != null ?
                serviceDTO.getCompletionDate().format(DATE_FORMATTER) :
                (serviceDTO.getFormattedCompletedDate() != null ?
                        serviceDTO.getFormattedCompletedDate() : "N/A");

        // Vehicle details
        String vehicleName = serviceDTO.getVehicleName() != null ? serviceDTO.getVehicleName() :
                (serviceDTO.getVehicleBrand() != null && serviceDTO.getVehicleModel() != null ?
                        serviceDTO.getVehicleBrand() + " " + serviceDTO.getVehicleModel() : "Your Vehicle");

        String registrationNumber = serviceDTO.getRegistrationNumber() != null ?
                serviceDTO.getRegistrationNumber() : "N/A";

        // Build the complete email template
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>"
                + "<div style='text-align: center; background-color: #7B1113; color: white; padding: 15px;'>"
                + "<h2>Albany Vehicle Service</h2>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<h3>Invoice #" + invoice.getInvoiceId() + "</h3>"
                + "<p>Hello " + customerName + ",</p>"
                + "<p>Thank you for choosing Albany Vehicle Service. Your service has been completed, and your invoice is ready.</p>"

                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;'>"
                + "<h4 style='margin-top: 0;'>Service Details</h4>"
                + "<table style='width: 100%;'>"
                + "<tr><td><strong>Service ID:</strong></td><td>REQ-" + serviceDTO.getRequestId() + "</td></tr>"
                + "<tr><td><strong>Vehicle:</strong></td><td>" + vehicleName + "</td></tr>"
                + "<tr><td><strong>Registration:</strong></td><td>" + registrationNumber + "</td></tr>"
                + "<tr><td><strong>Completion Date:</strong></td><td>" + completionDate + "</td></tr>"
                + "</table>"
                + "</div>"

                + "<h4>Materials Used</h4>"
                + materialsHtml.toString()

                + "<h4>Labor Charges</h4>"
                + laborHtml.toString()

                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;'>"
                + "<h4 style='margin-top: 0;'>Invoice Summary</h4>"
                + "<table style='width: 100%;'>"
                + "<tr>"
                + "<td style='padding: 8px; text-align: right;' colspan='3'><strong>Materials Total:</strong></td>"
                + "<td style='padding: 8px; text-align: right;'>₹" + formatDecimal(serviceDTO.getCalculatedMaterialsTotal()) + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='padding: 8px; text-align: right;' colspan='3'><strong>Labor Total:</strong></td>"
                + "<td style='padding: 8px; text-align: right;'>₹" + formatDecimal(serviceDTO.getCalculatedLaborTotal()) + "</td>"
                + "</tr>"
                + discountHtml
                + "<tr>"
                + "<td style='padding: 8px; text-align: right;' colspan='3'><strong>Subtotal:</strong></td>"
                + "<td style='padding: 8px; text-align: right;'>₹" + formatDecimal(serviceDTO.getCalculatedSubtotal()) + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='padding: 8px; text-align: right;' colspan='3'><strong>GST (18%):</strong></td>"
                + "<td style='padding: 8px; text-align: right;'>₹" + formatDecimal(serviceDTO.getCalculatedTax()) + "</td>"
                + "</tr>"
                + "<tr style='background-color: #e9ecef;'>"
                + "<td style='padding: 8px; text-align: right;' colspan='3'><strong>Grand Total:</strong></td>"
                + "<td style='padding: 8px; text-align: right;'><strong>₹" + formatDecimal(serviceDTO.getCalculatedTotal()) + "</strong></td>"
                + "</tr>"
                + "</table>"
                + "</div>"

                + "<p>You can view and download a PDF copy of this invoice by logging into your account or by visiting our service center.</p>"
                + "<p>If you have any questions about this invoice, please contact our customer service team.</p>"
                + "<p>Thank you for your business!</p>"
                + "<p>Regards,<br>Albany Service Team</p>"
                + "</div>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f9fa; font-size: 12px; color: #666;'>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "<p>© " + java.time.Year.now().getValue() + " Albany Vehicle Service. All rights reserved.</p>"
                + "</div>"
                + "</div>";
    }

    /**
     * Format decimal values to 2 decimal places
     */
    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }
}