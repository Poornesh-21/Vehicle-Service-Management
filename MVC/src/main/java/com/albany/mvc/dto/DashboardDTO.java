package com.albany.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {
    private Integer vehiclesDue;
    private Integer vehiclesInProgress;
    private Integer vehiclesCompleted;
    private BigDecimal totalRevenue;
    private List<VehicleDueDTO> vehiclesDueList;
    private List<VehicleInServiceDTO> vehiclesInServiceList;
    private List<CompletedServiceDTO> completedServicesList;
}

