package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillResponseDTO {
    private Integer billId;
    private Integer requestId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal materialsTotal;
    private BigDecimal laborTotal;
    private BigDecimal subtotal;
    private BigDecimal gst;
    private BigDecimal grandTotal;
    private LocalDateTime generatedAt;
    private String notes;
    private boolean emailSent;
    private String downloadUrl;
}