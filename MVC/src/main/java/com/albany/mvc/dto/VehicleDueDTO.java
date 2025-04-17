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
class VehicleDueDTO {
    private Integer requestId;
    private String vehicleName;
    private String registrationNumber;
    private String customerName;
    private String customerEmail;
    private String status;
    private LocalDate dueDate;  // Changed from String to LocalDate
    private String category;
}