package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleTrackingDTO {
    private Integer requestId;
    private String vehicleName;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String category;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String membershipStatus;
    private String serviceType;
    private String serviceAdvisorName;
    private Integer serviceAdvisorId;
    private LocalDateTime startDate;
    private LocalDate estimatedCompletionDate;
    private String status;
    private String additionalDescription;
    private LocalDateTime updatedAt;
}