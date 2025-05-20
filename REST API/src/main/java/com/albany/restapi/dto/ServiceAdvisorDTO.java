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
public class ServiceAdvisorDTO {
    private Integer advisorId;
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String department;
    private String specialization;
    private LocalDate hireDate;
    private String formattedId;
    private int workloadPercentage;
    private int activeServices;
    private String password; // Added for password resets and new account creation
}