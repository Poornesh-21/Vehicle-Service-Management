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
@Table(name = "ServiceTracking")
public class ServiceTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer trackingId;
    
    private Integer requestId;
    
    private String workDescription;
    
    private Integer laborMinutes;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal laborCost;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalMaterialCost;
    
    @Enumerated(EnumType.STRING)
    private ServiceRequest.Status status;
    
    @Column(updatable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "service_advisor_id")
    private ServiceAdvisorProfile serviceAdvisor;
    
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
}