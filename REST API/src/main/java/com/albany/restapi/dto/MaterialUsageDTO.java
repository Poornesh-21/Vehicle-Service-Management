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
    private Integer requestId;
    private String requestReference; // For display purposes
    private Integer itemId;
    private String itemName; // For display purposes
    private BigDecimal quantity;
    private LocalDateTime usedAt;
    private String serviceAdvisorName; // For display purposes
}