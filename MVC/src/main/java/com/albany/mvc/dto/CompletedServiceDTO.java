package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class CompletedServiceDTO {
    private Integer serviceId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private String completedDate;
    private String serviceAdvisorName;
    private BigDecimal totalCost;
    private boolean hasInvoice;
}