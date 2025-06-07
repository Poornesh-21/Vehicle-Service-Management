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
public class ServiceTrackingDTO {
    private Integer trackingId;
    private Integer requestId;
    private String status;
    private String workDescription;
    private Integer laborMinutes;
    private BigDecimal laborCost;
    private BigDecimal totalMaterialCost;
    private LocalDateTime updatedAt;
    private String updatedBy;
}