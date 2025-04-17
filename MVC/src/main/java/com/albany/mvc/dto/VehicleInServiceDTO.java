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

    // Added customer information fields
    private String customerName;
    private String customerEmail;
    private String membershipStatus;
    private String serviceType;
    private String additionalDescription;
}