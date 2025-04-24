package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class CompletedServiceDTO {
    private Integer serviceId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private LocalDate completedDate;  // Change from String to LocalDate
    private String serviceAdvisorName;
    private BigDecimal totalCost;
    private boolean hasBill;
    private boolean isPaid;
    private boolean hasInvoice;
}
