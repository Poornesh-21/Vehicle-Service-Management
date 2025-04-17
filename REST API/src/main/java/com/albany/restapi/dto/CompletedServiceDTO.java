package com.albany.restapi.dto;
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
public class CompletedServiceDTO {
    private Integer serviceId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private LocalDate completedDate;
    private String serviceAdvisorName;
    private BigDecimal totalCost;
    private boolean hasInvoice;
}