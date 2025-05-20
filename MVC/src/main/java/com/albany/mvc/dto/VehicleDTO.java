package com.albany.mvc.dto;

import lombok.Data;

@Data
public class VehicleDTO {
    private Integer vehicleId;
    private String brand;
    private String model;
    private Integer year;
    private String registrationNumber;
    private String category;
    private Integer customerId;
    // Display information
    private String customerName;
}