package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestDTO {
    private Integer requestId;
    private Long userId;
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    private Integer vehicleId;
    private String vehicleType;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleRegistration; // This name must match the database column name
    private Integer vehicleYear;
    private String serviceType;
    private String serviceDescription;
    private String additionalDescription;
    private LocalDate deliveryDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // UI display fields
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String membershipStatus;
    private List<MaterialUsageDTO> materials;
    
    // Formatted dates for UI display
    private String formattedDeliveryDate;
    private String formattedCreatedDate;
    
    // Legacy field for backward compatibility with frontend
    private String registrationNumber; 
    
    // Getter for registrationNumber that actually returns vehicleRegistration
    public String getRegistrationNumber() {
        return this.vehicleRegistration;
    }
    
    // Setter for registrationNumber that sets vehicleRegistration
    public void setRegistrationNumber(String registrationNumber) {
        this.vehicleRegistration = registrationNumber;
    }
}