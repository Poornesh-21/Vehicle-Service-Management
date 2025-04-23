package com.albany.restapi.controller;

import com.albany.restapi.model.Invoice;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.model.Vehicle;
import com.albany.restapi.repository.InvoiceRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import com.albany.restapi.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final InvoiceService invoiceService;

    /**
     * Download an invoice as PDF
     */
    @GetMapping("/{invoiceId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable Integer invoiceId) {
        log.info("Downloading invoice with ID: {}", invoiceId);
        
        // Find the invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
        
        // Find the service request
        ServiceRequest serviceRequest = serviceRequestRepository.findById(invoice.getRequestId())
                .orElseThrow(() -> new RuntimeException("Service request not found with ID: " + invoice.getRequestId()));
        
        // Generate the PDF
        byte[] pdfBytes = invoiceService.generateInvoicePdf(invoice, serviceRequest);
        
        // Prepare the response with PDF file
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        
        // Create a descriptive filename
        Vehicle vehicle = serviceRequest.getVehicle();
        String vehicleInfo = vehicle != null ? vehicle.getRegistrationNumber() : String.valueOf(invoice.getRequestId());
        String filename = "Invoice_" + invoice.getInvoiceId() + "_" + vehicleInfo + ".pdf";
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }
    
    /**
     * Download an invoice for a service request
     */
    @GetMapping("/service-request/{requestId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<?> downloadInvoiceByServiceRequest(@PathVariable Integer requestId) {
        log.info("Finding and downloading invoice for service request ID: {}", requestId);
        
        // Find the invoice by service request ID
        Invoice invoice = invoiceRepository.findByRequestId(requestId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for service request ID: " + requestId));
        
        // Redirect to the invoice download endpoint
        return downloadInvoice(invoice.getInvoiceId());
    }
}