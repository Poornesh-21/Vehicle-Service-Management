package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MechanicAssignmentDTO {
    private Integer serviceRequestId;
    private Integer primaryMechanicId;
    private List<Integer> additionalMechanicIds;
    private LocalDate estimatedCompletionDate;
    private String priority;
    private String serviceNotes;
}