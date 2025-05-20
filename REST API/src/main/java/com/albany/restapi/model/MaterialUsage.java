package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "materials_used")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer materialUsageId;
    
    @ManyToOne
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;
    
    @ManyToOne
    @JoinColumn(name = "request_id")
    private ServiceRequest serviceRequest;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;
    
    private LocalDateTime usedAt;
    
    @PrePersist
    public void prePersist() {
        if (usedAt == null) {
            usedAt = LocalDateTime.now();
        }
    }
}