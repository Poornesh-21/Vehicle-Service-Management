package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {
    
    public enum Status {
        Received,
        Diagnosis,
        Repair,
        Completed
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requestId;
    
    @Column(nullable = false)
    private Long userId;
    
    @ManyToOne
    @JoinColumn(name = "service_advisor_id")
    private ServiceAdvisorProfile serviceAdvisor;
    
    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;
    
    @Column(nullable = false)
    private String vehicleType;
    
    @Column(nullable = false, name = "vehicle_brand")
    private String vehicleBrand;
    
    @Column(nullable = false)
    private String vehicleModel;
    
    @Column(nullable = false, name = "vehicle_registration")
    private String vehicleRegistration;
    
    private Integer vehicleYear;
    
    private String serviceType;
    private String serviceDescription;
    private String additionalDescription;
    
    private LocalDate deliveryDate;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        
        // Ensure vehicleRegistration is not null
        if (vehicleRegistration == null) {
            vehicleRegistration = "Unknown";
        }
        
        // Ensure vehicleBrand is not null
        if (vehicleBrand == null) {
            vehicleBrand = "Unknown";
        }
        
        // Ensure vehicleModel is not null
        if (vehicleModel == null) {
            vehicleModel = "Unknown";
        }
        
        // Ensure vehicleType is not null
        if (vehicleType == null) {
            vehicleType = "Car";
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}