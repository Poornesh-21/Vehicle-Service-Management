package com.albany.mvc.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InventoryStatsDTO {
    private long totalItems;
    private long lowStockItems;
    private BigDecimal totalInventoryValue;
}