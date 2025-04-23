package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestDto {
    private Integer requestId;
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String serviceType;
    private LocalDate deliveryDate;
    private String additionalDescription;
    private Integer adminId;
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    private String status;
    private String customerName;
    private Integer customerId;
    private String membershipStatus;
    private String customerEmail;
    private String vehicleCategory;
    private String vehicleName; // Combined brand + model

    /**
     * Get status with better null handling
     * This ensures we never return null or empty status
     */
    public String getStatus() {
        // Return actual status if it exists, otherwise default to "Unknown"
        return (status != null && !status.trim().isEmpty()) ? status.trim() : "Unknown";
    }

    /**
     * Get membership status with better null handling
     * This ensures we never return null or empty membership status
     */
    public String getMembershipStatus() {
        // Return actual membership status if it exists, otherwise default to "Standard"
        return (membershipStatus != null && !membershipStatus.trim().isEmpty()) ?
                membershipStatus.trim() : "Standard";
    }

    /**
     * Get a combined vehicle name
     */
    public String getVehicleName() {
        if (vehicleName != null && !vehicleName.trim().isEmpty()) {
            return vehicleName;
        }

        if (vehicleBrand != null && vehicleModel != null) {
            return vehicleBrand + " " + vehicleModel;
        }

        return "Unknown Vehicle";
    }
}