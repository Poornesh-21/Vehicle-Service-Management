package com.albany.mvc.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MaterialUsageDTO {
    private Integer materialUsageId;
    private String requestReference;
    private LocalDateTime usedAt;
    private BigDecimal quantity;
    private String serviceAdvisorName;

    private String formattedDate;
}