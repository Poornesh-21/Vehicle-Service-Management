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
public class BillRequestDTO {
    private List<MaterialItemDTO> materials;
    private List<LaborChargeDTO> laborCharges;
    private BigDecimal materialsTotal;
    private BigDecimal laborTotal;
    private BigDecimal subtotal;
    private BigDecimal gst;
    private BigDecimal grandTotal;
    private String notes;
    private boolean sendEmail;
}