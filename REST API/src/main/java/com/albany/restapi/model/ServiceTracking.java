package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer trackingId;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private ServiceRequest serviceRequest;

    @ManyToOne
    @JoinColumn(name = "service_advisor_id")
    private ServiceAdvisorProfile serviceAdvisor;

    @Enumerated(EnumType.STRING)
    private ServiceRequest.Status status;

    private String workDescription;

    // Make sure these fields are properly defined
    @Column(name = "labor_minutes")
    private Integer laborMinutes;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "total_material_cost", precision = 10, scale = 2)
    private BigDecimal totalMaterialCost;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }

        // Initialize null values to avoid NPEs
        if (laborMinutes == null) {
            laborMinutes = 0;
        }

        if (laborCost == null) {
            laborCost = BigDecimal.ZERO;
        }

        if (totalMaterialCost == null) {
            totalMaterialCost = BigDecimal.ZERO;
        }
    }
}