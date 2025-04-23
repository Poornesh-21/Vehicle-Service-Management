package com.albany.restapi.dto;

import com.albany.restapi.model.ServiceRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestDTO {
    private Integer requestId;

    // Vehicle details
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String vehicleType;
    private Integer vehicleYear;

    // Service details
    private String serviceType;
    private String serviceDescription;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    private String additionalDescription;

    // Status handling
    private String status;

    // Admin and Service Advisor details
    private Integer adminId;
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;

    // Customer details
    private String customerName;
    private Integer customerId;
    private String membershipStatus;

    /**
     * Get status as ServiceRequest.Status enum
     */
    @JsonIgnore
    public ServiceRequest.Status getStatusEnum() {
        if (status == null || status.isEmpty()) {
            return ServiceRequest.Status.Received; // Default
        }

        try {
            // Convert to uppercase to handle case-insensitive input
            return ServiceRequest.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to handle common variations
            switch (status.toLowerCase()) {
                case "diagnose":
                case "diagnosing":
                    return ServiceRequest.Status.Diagnosis;
                case "repairing":
                    return ServiceRequest.Status.Repair;
                case "complete":
                    return ServiceRequest.Status.Completed;
                default:
                    return ServiceRequest.Status.Received;
            }
        }
    }

    /**
     * Set status from ServiceRequest.Status enum
     */
    public void setStatus(ServiceRequest.Status statusEnum) {
        if (statusEnum != null) {
            this.status = statusEnum.name();
        }
    }

    /**
     * Set status from String with flexible parsing
     */
    public void setStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            this.status = ServiceRequest.Status.Received.name();
            return;
        }

        try {
            // Convert to uppercase to handle case-insensitive input
            ServiceRequest.Status parsedStatus = ServiceRequest.Status.valueOf(statusStr.toUpperCase());
            this.status = parsedStatus.name();
        } catch (IllegalArgumentException e) {
            // Handle common variations
            switch (statusStr.toLowerCase()) {
                case "diagnose":
                case "diagnosing":
                    this.status = ServiceRequest.Status.Diagnosis.name();
                    break;
                case "repairing":
                    this.status = ServiceRequest.Status.Repair.name();
                    break;
                case "complete":
                    this.status = ServiceRequest.Status.Completed.name();
                    break;
                default:
                    this.status = ServiceRequest.Status.Received.name();
            }
        }
    }

    /**
     * Get membership status with a default if null
     */
    public String getMembershipStatus() {
        return membershipStatus != null && !membershipStatus.isEmpty() ?
                membershipStatus : "Standard";
    }

    public void setCustomerEmail(String email) {
    }
}