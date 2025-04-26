package com.albany.restapi.service;

import com.albany.restapi.dto.BillRequestDTO;
import com.albany.restapi.dto.BillResponseDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final EmailService emailService;
    

    @Transactional
    public BillResponseDTO generateBill(Integer requestId, BillRequestDTO billRequest) {
        log.info("Generating bill for service request ID: {}", requestId);
        
        // Find the service request
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));
        
        // Ensure service request is in Completed status
        if (serviceRequest.getStatus() != ServiceRequest.Status.Completed) {
            serviceRequest.setStatus(ServiceRequest.Status.Completed);
            serviceRequest = serviceRequestRepository.save(serviceRequest);
        }
        
        // Get vehicle and customer details
        Vehicle vehicle = serviceRequest.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User user = customer.getUser();
        
        // Create response DTO
        BillResponseDTO response = BillResponseDTO.builder()
                .billId(generateBillId())
                .requestId(requestId)
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .customerName(user.getFirstName() + " " + user.getLastName())
                .customerEmail(user.getEmail())
                .materialsTotal(billRequest.getMaterialsTotal())
                .laborTotal(billRequest.getLaborTotal())
                .subtotal(billRequest.getSubtotal())
                .gst(billRequest.getGst())
                .grandTotal(billRequest.getGrandTotal())
                .generatedAt(LocalDateTime.now())
                .notes(billRequest.getNotes())
                .downloadUrl("/api/bills/service-request/" + requestId + "/download")
                .build();
        
        // Send email if requested
        if (billRequest.isSendEmail()) {
            try {
                sendBillEmail(response);
                response.setEmailSent(true);
            } catch (Exception e) {
                log.error("Failed to send bill email: {}", e.getMessage(), e);
                response.setEmailSent(false);
            }
        }
        
        // In a real implementation, you would save the bill to the database here
        
        return response;
    }
    
    /**
     * Get bill details for a service request
     */
    public BillResponseDTO getBillByServiceRequest(Integer requestId) {
        // In a real implementation, this would fetch the bill from a database
        // This is a simplified implementation
        
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + requestId));
        
        Vehicle vehicle = serviceRequest.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User user = customer.getUser();
        
        // Create a dummy bill response
        return BillResponseDTO.builder()
                .billId(requestId + 1000)
                .requestId(requestId)
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .customerName(user.getFirstName() + " " + user.getLastName())
                .customerEmail(user.getEmail())
                .materialsTotal(new BigDecimal("5000.00"))
                .laborTotal(new BigDecimal("3000.00"))
                .subtotal(new BigDecimal("8000.00"))
                .gst(new BigDecimal("1440.00"))
                .grandTotal(new BigDecimal("9440.00"))
                .generatedAt(LocalDateTime.now().minusDays(1))
                .notes("Service completed as per requirements.")
                .downloadUrl("/api/bills/service-request/" + requestId + "/download")
                .emailSent(true)
                .build();
    }
    
    /**
     * Generate a PDF for the bill
     */
    public byte[] generateBillPdf(Integer requestId) {
        BillResponseDTO bill = getBillByServiceRequest(requestId);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
            
            Paragraph title = new Paragraph("ALBANY MOTORS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subtitle = new Paragraph("Service Bill", headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);
            
            // Add bill details
            document.add(new Paragraph("Bill ID: " + bill.getBillId(), headerFont));
            document.add(new Paragraph("Service Request ID: REQ-" + bill.getRequestId(), normalFont));
            document.add(new Paragraph("Date: " + bill.getGeneratedAt().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), normalFont));
            document.add(new Paragraph("Customer: " + bill.getCustomerName(), normalFont));
            document.add(new Paragraph("Vehicle: " + bill.getVehicleName() + " (" + bill.getRegistrationNumber() + ")", normalFont));
            document.add(new Paragraph(" "));
            
            // Add materials table
            document.add(new Paragraph("Materials & Parts", headerFont));
            PdfPTable materialsTable = new PdfPTable(4);
            materialsTable.setWidthPercentage(100);
            
            // Add table headers
            addTableHeader(materialsTable, new String[]{"Item", "Quantity", "Unit Price (₹)", "Total (₹)"});
            
            // Add sample materials (in a real implementation, this would come from the bill request)
            addTableRow(materialsTable, new String[]{"Engine Oil", "4", "850.00", "3400.00"});
            addTableRow(materialsTable, new String[]{"Oil Filter", "1", "350.00", "350.00"});
            addTableRow(materialsTable, new String[]{"Air Filter", "1", "650.00", "650.00"});
            
            // Add materials total row
            PdfPCell cell = new PdfPCell(new Phrase("Materials Total", headerFont));
            cell.setColspan(3);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            materialsTable.addCell(cell);
            
            cell = new PdfPCell(new Phrase("₹" + bill.getMaterialsTotal(), headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            materialsTable.addCell(cell);
            
            document.add(materialsTable);
            document.add(new Paragraph(" "));
            
            // Add labor charges table
            document.add(new Paragraph("Labor Charges", headerFont));
            PdfPTable laborTable = new PdfPTable(4);
            laborTable.setWidthPercentage(100);
            
            // Add table headers
            addTableHeader(laborTable, new String[]{"Description", "Hours", "Rate/Hour (₹)", "Total (₹)"});
            
            // Add sample labor charges (in a real implementation, this would come from the bill request)
            addTableRow(laborTable, new String[]{"Regular Service", "2", "600.00", "1200.00"});
            
            // Add labor total row
            cell = new PdfPCell(new Phrase("Labor Total", headerFont));
            cell.setColspan(3);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            laborTable.addCell(cell);
            
            cell = new PdfPCell(new Phrase("₹" + bill.getLaborTotal(), headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            laborTable.addCell(cell);
            
            document.add(laborTable);
            document.add(new Paragraph(" "));
            
            // Add summary
            document.add(new Paragraph("Bill Summary", headerFont));
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            // Add summary rows
            addSummaryRow(summaryTable, "Subtotal:", bill.getSubtotal().toString());
            addSummaryRow(summaryTable, "GST (18%):", bill.getGst().toString());
            addSummaryRow(summaryTable, "Grand Total:", bill.getGrandTotal().toString());
            
            document.add(summaryTable);
            document.add(new Paragraph(" "));
            
            // Add notes
            document.add(new Paragraph("Notes:", headerFont));
            document.add(new Paragraph(bill.getNotes(), normalFont));
            
            // Add footer
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Thank you for choosing Albany Motors!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating bill PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate bill PDF", e);
        }
    }
    
    /**
     * Helper method to add a table header
     */
    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
            cell.setBackgroundColor(new BaseColor(240, 240, 240));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
    
    /**
     * Helper method to add a table row
     */
    private void addTableRow(PdfPTable table, String[] cells) {
        for (String cell : cells) {
            PdfPCell pdfCell = new PdfPCell(new Phrase(cell));
            pdfCell.setPadding(5);
            table.addCell(pdfCell);
        }
    }
    
    /**
     * Helper method to add a summary row
     */
    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label));
        labelCell.setPadding(5);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase("₹" + value));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }
    
    /**
     * Generate a unique bill ID
     */
    private Integer generateBillId() {
        // In a real implementation, this would be auto-generated by the database
        return (int) (System.currentTimeMillis() % 100000);
    }
    
    /**
     * Send bill email to the customer
     */
    private void sendBillEmail(BillResponseDTO bill) {
        // Sample implementation - in a real app, you would use a template and attach the PDF
        String subject = "Albany Motors - Service Bill for " + bill.getVehicleName();
        
        String emailContent = String.format("""
            Dear %s,
            
            Your service bill for %s (%s) is now ready.
            
            Bill ID: %d
            Service Request ID: REQ-%d
            Total Amount: ₹%.2f
            
            You can download the bill from your customer portal or by visiting our service center.
            
            Thank you for choosing Albany Motors!
            
            Best regards,
            Albany Motors Service Team
            """,
            bill.getCustomerName(),
            bill.getVehicleName(),
            bill.getRegistrationNumber(),
            bill.getBillId(),
            bill.getRequestId(),
            bill.getGrandTotal()
        );
        
        // Send email using EmailService
        emailService.sendSimpleEmail(bill.getCustomerEmail(), subject, emailContent);
    }
}