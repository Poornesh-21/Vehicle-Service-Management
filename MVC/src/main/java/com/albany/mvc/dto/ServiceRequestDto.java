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

    /**
     * Get a default status if status is null
     */
    public String getStatus() {
        // Return actual status if it exists, otherwise default to "Unknown"
        return (status != null && !status.isEmpty()) ? status : "Unknown";
    }

    /**
     * Get a default membership status if it's null
     */
    public String getMembershipStatus() {
        // Return actual membership status if it exists, otherwise default to "Standard"
        return (membershipStatus != null && !membershipStatus.isEmpty()) ? membershipStatus : "Standard";
    }
}