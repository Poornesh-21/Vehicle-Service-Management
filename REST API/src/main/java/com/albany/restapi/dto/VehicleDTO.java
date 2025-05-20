package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDTO {
    private Integer vehicleId;
    private String brand;
    private String model;
    private Integer year;
    private String registrationNumber;
    private String category;
    private Integer customerId;
    
    // Customer info for display purposes
    private String customerName;
    private String customerEmail;
    private String membershipStatus;
}