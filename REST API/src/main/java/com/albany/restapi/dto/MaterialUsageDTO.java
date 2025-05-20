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
public class MaterialUsageDTO {
    private Integer materialUsageId;
    private String requestReference;
    private String name;
    private LocalDateTime usedAt;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String serviceAdvisorName;
    
    // Computed fields
    private BigDecimal totalCost;
}