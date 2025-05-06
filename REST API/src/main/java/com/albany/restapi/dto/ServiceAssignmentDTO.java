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
public class ServiceAssignmentDTO {
    private Integer serviceRequestId;
    private LocalDate estimatedCompletionDate;
    private String priority;
    private String serviceNotes;
}