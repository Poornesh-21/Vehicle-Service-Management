package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MaterialsUsed")
public class MaterialUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer materialUsageId;
    
    @ManyToOne
    @JoinColumn(name = "request_id")
    private ServiceRequest serviceRequest;
    
    @ManyToOne
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}