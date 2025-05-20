package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;
    
    private String name;
    private String category;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal currentStock;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal reorderLevel;
    
    @Transient
    private BigDecimal totalValue;
    
    @Transient
    private String stockStatus;
    
    @PostLoad
    @PostPersist
    @PostUpdate
    public void calculateDerivedFields() {
        // Calculate total value
        if (currentStock != null && unitPrice != null) {
            totalValue = currentStock.multiply(unitPrice);
        } else {
            totalValue = BigDecimal.ZERO;
        }
        
        // Determine stock status
        if (currentStock != null && reorderLevel != null) {
            // If stock is less than or equal to reorder level, it's low
            if (currentStock.compareTo(reorderLevel) <= 0) {
                stockStatus = "Low";
            } 
            // If stock is between reorder level and 2x reorder level, it's medium
            else if (currentStock.compareTo(reorderLevel.multiply(BigDecimal.valueOf(2))) <= 0) {
                stockStatus = "Medium";
            } 
            // Otherwise, it's good
            else {
                stockStatus = "Good";
            }
        } else {
            stockStatus = "Unknown";
        }
    }
}