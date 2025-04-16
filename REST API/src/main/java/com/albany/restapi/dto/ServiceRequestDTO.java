package com.albany.restapi.dto;

import com.albany.restapi.model.ServiceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestDTO {
    private Integer requestId;
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String serviceType;
    private LocalDate deliveryDate;
    private String additionalDescription;
    private Integer adminId;
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    private ServiceRequest.Status status;
    private String customerName;
    private Integer customerId;
}