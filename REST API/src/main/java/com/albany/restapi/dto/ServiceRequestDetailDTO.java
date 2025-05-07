package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestDetailDTO {
    // Request information
    private Integer requestId;
    private String serviceType;
    private String additionalDescription;
    private String status;
    private LocalDate requestDate;
    private LocalDate estimatedCompletionDate;
    
    // Vehicle information
    private Integer vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String vehicleType;
    private Integer vehicleYear;
    
    // Customer information
    private Integer customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String membershipStatus;
    
    // Service advisor information
    private Integer serviceAdvisorId;
    private String serviceAdvisorName;
    
    // Service history
    private List<ServiceHistoryDTO> serviceHistory;
    
    // Current bill information
    private ServiceBillDTO currentBill;
}