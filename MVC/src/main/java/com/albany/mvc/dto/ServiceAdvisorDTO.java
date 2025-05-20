package com.albany.mvc.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
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
}