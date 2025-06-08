package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompletedServiceDTO {
    // Request and identification
    private Integer requestId;
    private Integer serviceId;
    
    // Vehicle information
    private String vehicleName;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String vehicleType;
    private String category;
    private Integer vehicleYear;
    
    // Customer information
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer customerId;
    private String membershipStatus;
    
    // Service information
    private String serviceType;
    private String serviceDescription;
    private String additionalDescription;
    private String serviceAdvisorName;
    
    // Date information
    private LocalDateTime completionDate;
    private LocalDateTime completedDate;
    private LocalDateTime updatedAt;
    private String formattedCompletedDate;
    
    // Invoice information
    private BigDecimal totalAmount;
    private BigDecimal totalCost;
    private boolean hasInvoice;
    private Integer invoiceId;
    private boolean paid;
    private boolean delivered;
    
    // Financial calculations
    private BigDecimal calculatedMaterialsTotal;
    private BigDecimal calculatedLaborTotal;
    private BigDecimal calculatedDiscount;
    private BigDecimal calculatedSubtotal;
    private BigDecimal calculatedTax;
    private BigDecimal calculatedTotal;
    
    // Service details
    private List<MaterialItemDTO> materials;
    private List<LaborChargeDTO> laborCharges;
    
    // Compatibility getters
    public boolean getIsPaid() {
        return paid;
    }
    
    public boolean getIsDelivered() {
        return delivered;
    }
}