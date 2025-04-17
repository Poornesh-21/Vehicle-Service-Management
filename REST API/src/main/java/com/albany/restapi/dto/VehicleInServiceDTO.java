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
public class VehicleInServiceDTO {
    private Integer requestId;
    private String vehicleName;
    private String registrationNumber;
    private String serviceAdvisorName;
    private String serviceAdvisorId;
    private String status;
    private LocalDate startDate;
    private LocalDate estimatedCompletionDate;
    private String category;
}