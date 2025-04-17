package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String dueDate;
    private String category;
}