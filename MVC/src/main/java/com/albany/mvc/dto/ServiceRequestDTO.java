package com.albany.mvc.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceRequestDTO {
    private Integer requestId;
    private Long userId;
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    private Integer vehicleId;
    private String vehicleType;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleRegistration;
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
    
    // Formatted date for UI display
    private String formattedDeliveryDate;
    private String formattedCreatedDate;
}