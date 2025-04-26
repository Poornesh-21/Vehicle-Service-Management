package com.albany.restapi.controller;

import com.albany.restapi.dto.BillRequestDTO;
import com.albany.restapi.dto.BillResponseDTO;
import com.albany.restapi.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Slf4j
public class BillController {

    private final BillService billService;

    /**
     * Generate a bill for a service request
     */
    @PostMapping("/service-request/{requestId}/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<BillResponseDTO> generateBill(
            @PathVariable Integer requestId,
            @RequestBody BillRequestDTO billRequest) {
        
        log.info("Generating bill for service request ID: {}", requestId);
        BillResponseDTO response = billService.generateBill(requestId, billRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get bill details for a service request
     */
    @GetMapping("/service-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<BillResponseDTO> getBillByServiceRequest(@PathVariable Integer requestId) {
        log.info("Fetching bill for service request ID: {}", requestId);
        BillResponseDTO bill = billService.getBillByServiceRequest(requestId);
        return ResponseEntity.ok(bill);
    }

    /**
     * Download bill PDF
     * This endpoint is handled by a separate controller in the MVC application
     */
    @GetMapping("/service-request/{requestId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<byte[]> downloadBill(@PathVariable Integer requestId) {
        log.info("Downloading bill PDF for service request ID: {}", requestId);
        byte[] pdfContent = billService.generateBillPdf(requestId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=bill_" + requestId + ".pdf")
                .body(pdfContent);
    }
}