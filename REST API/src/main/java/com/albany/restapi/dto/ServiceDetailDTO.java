package com.albany.restapi.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ServiceDetailDTO {
    private Integer requestId;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleType;
    private String registrationNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String membershipStatus;
    private String serviceType;
    private String serviceAdvisor;
    private String status;
    private String additionalDescription;
    private LocalDateTime requestDate;
    private LocalDateTime lastStatusUpdate;
    private Integer laborMinutes;
    private BigDecimal laborCost;
    private BigDecimal totalMaterialCost;
    private String workDescription;
    private Map<String, Object> currentBill;
}