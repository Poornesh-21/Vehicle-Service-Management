package com.albany.mvc.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InventoryItemDTO {
    private Integer itemId;
    private String name;
    private String category;
    private BigDecimal currentStock;
    private BigDecimal unitPrice;
    private BigDecimal reorderLevel;
    private BigDecimal totalValue;
    private String stockStatus;
}



