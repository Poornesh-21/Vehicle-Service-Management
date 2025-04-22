package com.albany.restapi.dto;

import com.albany.restapi.model.ServiceRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    private String additionalDescription;
    private Integer adminId;
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;

    // Changed from ServiceRequest.Status to String to handle bidirectional conversion
    private String status;

    private String customerName;
    private Integer customerId;

    // Utility method to get the status as an enum
    public ServiceRequest.Status getStatusEnum() {
        if (status == null || status.isEmpty()) {
            return ServiceRequest.Status.Received; // Default
        }

        try {
            return ServiceRequest.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            return ServiceRequest.Status.Received; // Default if invalid
        }
    }

    // Utility method to set the status from an enum
    public void setStatus(ServiceRequest.Status statusEnum) {
        if (statusEnum != null) {
            this.status = statusEnum.name();
        }
    }
}