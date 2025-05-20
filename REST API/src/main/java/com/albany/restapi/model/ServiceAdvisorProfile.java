package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "service_advisor_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAdvisorProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer advisorId;
    
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    private String department;
    private String specialization;
    private LocalDate hireDate;
}