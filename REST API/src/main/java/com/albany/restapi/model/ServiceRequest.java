package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ServiceRequests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "additional_description")
    private String additionalDescription;

    @Column(name = "service_description")
    private String serviceDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private AdminProfile admin;

    @ManyToOne
    @JoinColumn(name = "service_advisor_id")
    private ServiceAdvisorProfile serviceAdvisor;

    // Vehicle-specific fields to match database schema
    @Column(name = "vehicle_model", nullable = false)
    private String vehicleModel;

    @Column(name = "vehicle_registration", nullable = false)
    private String vehicleRegistration;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    // Add user_id column
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Populate vehicle-specific fields from associated vehicle if not set
        if (vehicle != null) {
            if (this.vehicleModel == null) {
                this.vehicleModel = vehicle.getModel();
            }
            if (this.vehicleRegistration == null) {
                this.vehicleRegistration = vehicle.getRegistrationNumber();
            }
            if (this.vehicleType == null) {
                this.vehicleType = vehicle.getCategory() != null ? vehicle.getCategory().name() : null;
            }
            if (this.vehicleYear == null) {
                this.vehicleYear = vehicle.getYear();
            }

            // Set user_id from vehicle's customer's user
            if (vehicle.getCustomer() != null && vehicle.getCustomer().getUser() != null) {
                this.userId = vehicle.getCustomer().getUser().getUserId();
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum for status
    public enum Status {
        Received,
        Diagnosis,
        Repair,
        Completed
    }
}