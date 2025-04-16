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
public class ServiceAdvisorDto {
    private Integer advisorId;
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String password;
    private String department;
    private String specialization;
    private LocalDate hireDate;
    private String formattedId;
    private boolean isActive;
    private int activeServices;
    private int workloadPercentage;
}