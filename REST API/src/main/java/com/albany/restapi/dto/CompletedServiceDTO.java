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
    private Integer requestId;
    private Integer serviceId;
    private String vehicleName;
    private String vehicleBrand;
    private String vehicleModel;
    private String registrationNumber;
    private String vehicleType;
    private String category;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer customerId;
    private String membershipStatus;
    private LocalDateTime completionDate;
    private LocalDateTime completedDate;
    private LocalDateTime updatedAt;
    private String formattedCompletedDate;
    private BigDecimal totalAmount;
    private BigDecimal totalCost;
    private BigDecimal calculatedTotal;
    private boolean hasInvoice;
    private Integer invoiceId;
    private boolean paid;
    private boolean delivered;
    
    // These fields were missing and caused the compilation errors
    private String serviceType;
    private String serviceAdvisorName;

    // Invoice related fields
    private BigDecimal calculatedMaterialsTotal;
    private BigDecimal calculatedLaborTotal;
    private BigDecimal calculatedDiscount;
    private BigDecimal calculatedSubtotal;
    private BigDecimal calculatedTax;
    
    // Materials and labor charges
    private List<MaterialItemDTO> materials;
    private List<LaborChargeDTO> laborCharges;
    
    // Backward compatibility getters
    public boolean getIsPaid() {
        return paid;
    }
    
    public boolean getIsDelivered() {
        return delivered;
    }
}