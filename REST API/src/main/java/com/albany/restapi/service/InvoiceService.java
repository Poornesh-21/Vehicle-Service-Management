package com.albany.restapi.service;

import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final ServiceTrackingRepository serviceTrackingRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CustomerProfileRepository customerProfileRepository;

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
    private static final Font TOTAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(114, 47, 55));

    /**
     * Generate a PDF invoice for a service request
     */
    public byte[] generateInvoicePdf(Invoice invoice, ServiceRequest serviceRequest) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            // Add metadata
            document.addTitle("Invoice #" + invoice.getInvoiceId());
            document.addSubject("Service Invoice");
            document.addKeywords("Albany, Vehicle, Service, Invoice");
            document.addAuthor("Albany Vehicle Management System");
            document.addCreator("Albany Invoice Generator");
            
            document.open();
            
            // Add header with logo and company info
            addHeader(document, invoice);
            
            // Add invoice details
            addInvoiceDetails(document, invoice, serviceRequest);
            
            // Add customer and vehicle details
            addCustomerAndVehicleDetails(document, serviceRequest);
            
            // Add service details
            addServiceDetails(document, serviceRequest);
            
            // Add materials used
            addMaterialsUsed(document, serviceRequest);
            
            // Add labor charges
            addLaborCharges(document, serviceRequest);
            
            // Add invoice summary
            addInvoiceSummary(document, invoice, serviceRequest);
            
            // Add footer
            addFooter(document);
            
            document.close();
            writer.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating invoice PDF", e);
            throw new RuntimeException("Failed to generate invoice PDF: " + e.getMessage());
        }
    }
    
    private void addHeader(Document document, Invoice invoice) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 1});
        headerTable.setSpacingAfter(20);
        
        // Company logo and name
        PdfPCell logoCell = new PdfPCell();
        Paragraph companyName = new Paragraph("ALBANY MOTORS", TITLE_FONT);
        logoCell.addElement(companyName);
        logoCell.addElement(new Paragraph("Premium Vehicle Service Center", NORMAL_FONT));
        logoCell.addElement(new Paragraph("123 Service Road, Albany, NY 12345", SMALL_FONT));
        logoCell.addElement(new Paragraph("Phone: +1 (555) 123-4567", SMALL_FONT));
        logoCell.addElement(new Paragraph("Email: service@albanymotors.com", SMALL_FONT));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingBottom(10);
        headerTable.addCell(logoCell);
        
        // Invoice information
        PdfPCell invoiceInfoCell = new PdfPCell();
        Paragraph invoiceTitle = new Paragraph("INVOICE", TITLE_FONT);
        invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
        invoiceInfoCell.addElement(invoiceTitle);
        
        Paragraph invoiceNumber = new Paragraph("Invoice #: INV-" + invoice.getInvoiceId(), NORMAL_FONT);
        invoiceNumber.setAlignment(Element.ALIGN_RIGHT);
        invoiceInfoCell.addElement(invoiceNumber);
        
        Paragraph invoiceDate = new Paragraph("Date: " + 
            invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), NORMAL_FONT);
        invoiceDate.setAlignment(Element.ALIGN_RIGHT);
        invoiceInfoCell.addElement(invoiceDate);
        
        Paragraph requestId = new Paragraph("Service #: REQ-" + invoice.getRequestId(), NORMAL_FONT);
        requestId.setAlignment(Element.ALIGN_RIGHT);
        invoiceInfoCell.addElement(requestId);
        
        invoiceInfoCell.setBorder(Rectangle.NO_BORDER);
        invoiceInfoCell.setPaddingBottom(10);
        headerTable.addCell(invoiceInfoCell);
        
        document.add(headerTable);
        
        // Add separator line
        LineSeparator line = new LineSeparator(1, 100, new BaseColor(114, 47, 55), Element.ALIGN_CENTER, -5);
        document.add(line);
    }
    
    private void addInvoiceDetails(Document document, Invoice invoice, ServiceRequest serviceRequest) throws DocumentException {
        Paragraph invoiceDetailsTitle = new Paragraph("INVOICE DETAILS", HEADER_FONT);
        invoiceDetailsTitle.setSpacingBefore(20);
        invoiceDetailsTitle.setSpacingAfter(10);
        document.add(invoiceDetailsTitle);
        
        PdfPTable detailsTable = new PdfPTable(4);
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(20);
        
        // Add column headers
        addCell(detailsTable, "Invoice Number", BOLD_FONT);
        addCell(detailsTable, "Service Request ID", BOLD_FONT);
        addCell(detailsTable, "Invoice Date", BOLD_FONT);
        addCell(detailsTable, "Status", BOLD_FONT);
        
        // Add values
        addCell(detailsTable, "INV-" + invoice.getInvoiceId(), NORMAL_FONT);
        addCell(detailsTable, "REQ-" + invoice.getRequestId(), NORMAL_FONT);
        addCell(detailsTable, invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), NORMAL_FONT);
        addCell(detailsTable, serviceRequest.getStatus().toString(), NORMAL_FONT);
        
        document.add(detailsTable);
    }
    
    private void addCustomerAndVehicleDetails(Document document, ServiceRequest serviceRequest) throws DocumentException {
        PdfPTable customerVehicleTable = new PdfPTable(2);
        customerVehicleTable.setWidthPercentage(100);
        customerVehicleTable.setWidths(new float[]{1, 1});
        customerVehicleTable.setSpacingAfter(20);
        
        // Customer information
        PdfPCell customerCell = new PdfPCell();
        customerCell.addElement(new Paragraph("CUSTOMER INFORMATION", HEADER_FONT));
        
        if (serviceRequest.getVehicle() != null && serviceRequest.getVehicle().getCustomer() != null) {
            CustomerProfile customer = serviceRequest.getVehicle().getCustomer();
            User user = customer.getUser();
            
            if (user != null) {
                customerCell.addElement(new Paragraph(user.getFirstName() + " " + user.getLastName(), BOLD_FONT));
                customerCell.addElement(new Paragraph("Email: " + user.getEmail(), NORMAL_FONT));
                customerCell.addElement(new Paragraph("Phone: " + user.getPhoneNumber(), NORMAL_FONT));
                
                customerCell.addElement(new Paragraph("Address:", NORMAL_FONT));
                
                String address = customer.getStreet() + ", " + customer.getCity() + ", " + 
                                customer.getState() + " " + customer.getPostalCode();
                customerCell.addElement(new Paragraph(address, NORMAL_FONT));
                
                String membershipStatus = customer.getMembershipStatus() != null ? 
                                        customer.getMembershipStatus() : "Standard";
                customerCell.addElement(new Paragraph("Membership: " + membershipStatus, BOLD_FONT));
            } else {
                customerCell.addElement(new Paragraph("Customer details not available", NORMAL_FONT));
            }
        } else {
            customerCell.addElement(new Paragraph("Customer details not available", NORMAL_FONT));
        }
        
        customerCell.setBorder(Rectangle.BOX);
        customerCell.setPadding(10);
        customerVehicleTable.addCell(customerCell);
        
        // Vehicle information
        PdfPCell vehicleCell = new PdfPCell();
        vehicleCell.addElement(new Paragraph("VEHICLE INFORMATION", HEADER_FONT));
        
        if (serviceRequest.getVehicle() != null) {
            Vehicle vehicle = serviceRequest.getVehicle();
            
            vehicleCell.addElement(new Paragraph(vehicle.getBrand() + " " + vehicle.getModel(), BOLD_FONT));
            vehicleCell.addElement(new Paragraph("Registration: " + vehicle.getRegistrationNumber(), NORMAL_FONT));
            vehicleCell.addElement(new Paragraph("Year: " + vehicle.getYear(), NORMAL_FONT));
            vehicleCell.addElement(new Paragraph("Category: " + vehicle.getCategory(), NORMAL_FONT));
        } else {
            vehicleCell.addElement(new Paragraph("Vehicle details not available", NORMAL_FONT));
        }
        
        vehicleCell.setBorder(Rectangle.BOX);
        vehicleCell.setPadding(10);
        customerVehicleTable.addCell(vehicleCell);
        
        document.add(customerVehicleTable);
    }
    
    private void addServiceDetails(Document document, ServiceRequest serviceRequest) throws DocumentException {
        Paragraph serviceDetailsTitle = new Paragraph("SERVICE DETAILS", HEADER_FONT);
        serviceDetailsTitle.setSpacingBefore(5);
        serviceDetailsTitle.setSpacingAfter(10);
        document.add(serviceDetailsTitle);
        
        PdfPTable serviceTable = new PdfPTable(2);
        serviceTable.setWidthPercentage(100);
        serviceTable.setSpacingAfter(20);
        
        // Service type
        addCell(serviceTable, "Service Type:", BOLD_FONT);
        addCell(serviceTable, serviceRequest.getServiceType(), NORMAL_FONT);
        
        // Service description
        addCell(serviceTable, "Description:", BOLD_FONT);
        addCell(serviceTable, serviceRequest.getAdditionalDescription() != null ? 
                serviceRequest.getAdditionalDescription() : "General service and maintenance", NORMAL_FONT);
        
        // Service start date
        addCell(serviceTable, "Service Start:", BOLD_FONT);
        addCell(serviceTable, serviceRequest.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), NORMAL_FONT);
        
        // Service completion date
        addCell(serviceTable, "Service Completion:", BOLD_FONT);
        LocalDateTime completionDate = serviceRequest.getUpdatedAt() != null ? 
                                    serviceRequest.getUpdatedAt() : LocalDateTime.now();
        addCell(serviceTable, completionDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), NORMAL_FONT);
        
        document.add(serviceTable);
    }
    
    private void addMaterialsUsed(Document document, ServiceRequest serviceRequest) throws DocumentException {
        Paragraph materialsTitle = new Paragraph("MATERIALS USED", HEADER_FONT);
        materialsTitle.setSpacingBefore(5);
        materialsTitle.setSpacingAfter(10);
        document.add(materialsTitle);
        
        // Get materials used for this service request
        List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(serviceRequest.getRequestId());
        
        if (materials.isEmpty()) {
            document.add(new Paragraph("No materials recorded for this service.", NORMAL_FONT));
            return;
        }
        
        PdfPTable materialsTable = new PdfPTable(4);
        materialsTable.setWidthPercentage(100);
        materialsTable.setSpacingAfter(10);
        
        // Add table headers
        String[] headers = {"Item", "Quantity", "Unit Price (₹)", "Total (₹)"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
            cell.setBackgroundColor(new BaseColor(238, 238, 238));
            cell.setPadding(5);
            materialsTable.addCell(cell);
        }
        
        // Add materials
        BigDecimal materialsTotalCost = BigDecimal.ZERO;
        
        for (MaterialUsage usage : materials) {
            InventoryItem item = usage.getInventoryItem();
            
            if (item != null) {
                addCell(materialsTable, item.getName(), NORMAL_FONT);
                addCell(materialsTable, usage.getQuantity().toString(), NORMAL_FONT);
                addCell(materialsTable, formatCurrency(item.getUnitPrice()), NORMAL_FONT);
                
                BigDecimal totalItemCost = item.getUnitPrice().multiply(usage.getQuantity());
                addCell(materialsTable, formatCurrency(totalItemCost), NORMAL_FONT);
                
                materialsTotalCost = materialsTotalCost.add(totalItemCost);
            }
        }
        
        // Add total row
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Materials Total", BOLD_FONT));
        totalLabelCell.setColspan(3);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(5);
        materialsTable.addCell(totalLabelCell);
        
        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(materialsTotalCost), BOLD_FONT));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(5);
        materialsTable.addCell(totalValueCell);
        
        document.add(materialsTable);
    }
    
    private void addLaborCharges(Document document, ServiceRequest serviceRequest) throws DocumentException {
        Paragraph laborTitle = new Paragraph("LABOR CHARGES", HEADER_FONT);
        laborTitle.setSpacingBefore(5);
        laborTitle.setSpacingAfter(10);
        document.add(laborTitle);
        
        // Get labor charges from service tracking
        List<ServiceTracking> trackingEntries = serviceTrackingRepository.findByRequestId(serviceRequest.getRequestId());
        
        if (trackingEntries.isEmpty()) {
            document.add(new Paragraph("No labor charges recorded for this service.", NORMAL_FONT));
            return;
        }
        
        PdfPTable laborTable = new PdfPTable(4);
        laborTable.setWidthPercentage(100);
        laborTable.setSpacingAfter(10);
        
        // Add table headers
        String[] headers = {"Description", "Hours", "Rate/Hour (₹)", "Total (₹)"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
            cell.setBackgroundColor(new BaseColor(238, 238, 238));
            cell.setPadding(5);
            laborTable.addCell(cell);
        }
        
        // Add labor entries
        BigDecimal laborTotalCost = BigDecimal.ZERO;
        
        for (ServiceTracking tracking : trackingEntries) {
            // Skip entries without labor information
            if (tracking.getLaborMinutes() == null || tracking.getLaborMinutes() == 0) {
                continue;
            }
            
            // Calculate hours from minutes
            BigDecimal hours = new BigDecimal(tracking.getLaborMinutes())
                                .divide(new BigDecimal(60), 1, RoundingMode.HALF_UP);
            
            // Calculate hourly rate
            BigDecimal hourlyRate = tracking.getLaborCost()
                                  .multiply(new BigDecimal(60))
                                  .divide(new BigDecimal(tracking.getLaborMinutes()), 2, RoundingMode.HALF_UP);
            
            addCell(laborTable, tracking.getWorkDescription(), NORMAL_FONT);
            addCell(laborTable, hours.toString(), NORMAL_FONT);
            addCell(laborTable, formatCurrency(hourlyRate), NORMAL_FONT);
            addCell(laborTable, formatCurrency(tracking.getLaborCost()), NORMAL_FONT);
            
            laborTotalCost = laborTotalCost.add(tracking.getLaborCost());
        }
        
        // Add total row
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Labor Total", BOLD_FONT));
        totalLabelCell.setColspan(3);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(5);
        laborTable.addCell(totalLabelCell);
        
        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(laborTotalCost), BOLD_FONT));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(5);
        laborTable.addCell(totalValueCell);
        
        document.add(laborTable);
    }

    private void addInvoiceSummary(Document document, Invoice invoice, ServiceRequest serviceRequest) throws DocumentException {
        Paragraph summaryTitle = new Paragraph("INVOICE SUMMARY", HEADER_FONT);
        summaryTitle.setSpacingBefore(10);
        summaryTitle.setSpacingAfter(10);
        document.add(summaryTitle);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setSpacingAfter(20);

        // Materials total
        BigDecimal materialsTotalCost = getTotalMaterialCost(serviceRequest);
        addCell(summaryTable, "Materials Total:", BOLD_FONT, Element.ALIGN_LEFT);
        addCell(summaryTable, formatCurrency(materialsTotalCost), NORMAL_FONT, Element.ALIGN_RIGHT);

        // Check if customer is premium
        boolean isPremiumCustomer = false;
        if (serviceRequest.getVehicle() != null &&
                serviceRequest.getVehicle().getCustomer() != null &&
                "Premium".equalsIgnoreCase(serviceRequest.getVehicle().getCustomer().getMembershipStatus())) {
            isPremiumCustomer = true;
        }

        // Original labor total
        BigDecimal laborTotalCost = getTotalLaborCost(serviceRequest);

        // For premium customers, apply 20% discount on labor
        BigDecimal laborDiscount = BigDecimal.ZERO;
        BigDecimal discountedLaborCost = laborTotalCost;

        if (isPremiumCustomer) {
            // Calculate 20% discount on labor
            laborDiscount = laborTotalCost.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            discountedLaborCost = laborTotalCost.subtract(laborDiscount);

            // Show original labor cost
            addCell(summaryTable, "Labor Total (Original):", BOLD_FONT, Element.ALIGN_LEFT);
            addCell(summaryTable, formatCurrency(laborTotalCost), NORMAL_FONT, Element.ALIGN_RIGHT);

            // Show premium discount
            addCell(summaryTable, "Premium Discount (20% on Labor):", BOLD_FONT, Element.ALIGN_LEFT);
            addCell(summaryTable, "-" + formatCurrency(laborDiscount), NORMAL_FONT, Element.ALIGN_RIGHT);

            // Show discounted labor cost
            addCell(summaryTable, "Labor Total (After Discount):", BOLD_FONT, Element.ALIGN_LEFT);
            addCell(summaryTable, formatCurrency(discountedLaborCost), NORMAL_FONT, Element.ALIGN_RIGHT);
        } else {
            // Regular labor cost for non-premium customers
            addCell(summaryTable, "Labor Total:", BOLD_FONT, Element.ALIGN_LEFT);
            addCell(summaryTable, formatCurrency(laborTotalCost), NORMAL_FONT, Element.ALIGN_RIGHT);
        }

        // Subtotal (materials + discounted labor)
        BigDecimal subtotal = materialsTotalCost.add(discountedLaborCost);
        addCell(summaryTable, "Subtotal:", BOLD_FONT, Element.ALIGN_LEFT);
        addCell(summaryTable, formatCurrency(subtotal), NORMAL_FONT, Element.ALIGN_RIGHT);

        // Taxes (from invoice or calculated)
        BigDecimal taxes = invoice.getTaxes() != null ? invoice.getTaxes() :
                subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        addCell(summaryTable, "GST (18%):", BOLD_FONT, Element.ALIGN_LEFT);
        addCell(summaryTable, formatCurrency(taxes), NORMAL_FONT, Element.ALIGN_RIGHT);

        // Grand total
        BigDecimal grandTotal = subtotal.add(taxes);

        // Add separator line
        PdfPCell separatorCell = new PdfPCell(new Phrase(""));
        separatorCell.setColspan(2);
        separatorCell.setBorder(Rectangle.TOP);
        separatorCell.setPaddingTop(2);
        summaryTable.addCell(separatorCell);

        // Grand Total row
        addCell(summaryTable, "Grand Total:", TOTAL_FONT, Element.ALIGN_LEFT);
        addCell(summaryTable, formatCurrency(grandTotal), TOTAL_FONT, Element.ALIGN_RIGHT);

        document.add(summaryTable);

        // Add payment note
        Paragraph paymentNote = new Paragraph("This invoice has been paid in full. Thank you for your business!", NORMAL_FONT);
        paymentNote.setAlignment(Element.ALIGN_CENTER);
        paymentNote.setSpacingBefore(20);
        document.add(paymentNote);

        // If customer is premium, add special thank you note
        if (isPremiumCustomer) {
            Paragraph premiumNote = new Paragraph(
                    "Thank you for being a Premium member! You've received a 20% discount on labor charges.",
                    new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, new BaseColor(114, 47, 55))
            );
            premiumNote.setAlignment(Element.ALIGN_CENTER);
            premiumNote.setSpacingBefore(10);
            document.add(premiumNote);
        }
    }
    
    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(30);
        
        LineSeparator line = new LineSeparator(1, 100, new BaseColor(200, 200, 200), Element.ALIGN_CENTER, -5);
        footer.add(line);
        
        Paragraph companyInfo = new Paragraph("Albany Motors - Premium Vehicle Service Center", SMALL_FONT);
        companyInfo.setAlignment(Element.ALIGN_CENTER);
        footer.add(companyInfo);
        
        Paragraph contactInfo = new Paragraph("123 Service Road, Albany, NY 12345 | Phone: +1 (555) 123-4567 | Email: service@albanymotors.com", SMALL_FONT);
        contactInfo.setAlignment(Element.ALIGN_CENTER);
        footer.add(contactInfo);
        
        document.add(footer);
    }
    
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal getTotalMaterialCost(ServiceRequest serviceRequest) {
        List<MaterialUsage> materials = materialUsageRepository.findByServiceRequest_RequestId(serviceRequest.getRequestId());
        
        return materials.stream()
            .filter(m -> m.getInventoryItem() != null)
            .map(m -> m.getInventoryItem().getUnitPrice().multiply(m.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal getTotalLaborCost(ServiceRequest serviceRequest) {
        return serviceTrackingRepository.findTotalLaborCostByRequestId(serviceRequest.getRequestId())
            .orElse(BigDecimal.ZERO);
    }
}