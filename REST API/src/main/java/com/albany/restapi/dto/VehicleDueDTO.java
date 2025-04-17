package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDueDTO {
    private Integer requestId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private String customerEmail;
    private String status;
    private LocalDate dueDate;
    private String category;
    private String membershipStatus;
}