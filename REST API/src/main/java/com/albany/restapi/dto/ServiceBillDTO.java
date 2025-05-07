package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceBillDTO {
    private Integer requestId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private String customerEmail;
    
    private List<MaterialItemDTO> materials;
    private List<LaborChargeDTO> laborCharges;
    
    private BigDecimal partsSubtotal;
    private BigDecimal laborSubtotal;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    
    private String notes;
    private Boolean hasGeneratedBill;
    private Integer billId;
}