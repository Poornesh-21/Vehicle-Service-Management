package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class VehicleInServiceDTO {
    private Integer requestId;
    private String vehicleName;
    private String registrationNumber;
    private String serviceAdvisorName;
    private String serviceAdvisorId;
    private String status;
    private String startDate;
    private String estimatedCompletionDate;
    private String category;
}