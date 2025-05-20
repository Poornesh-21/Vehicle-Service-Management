package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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