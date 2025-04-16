package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ServiceAdvisorProfiles")
public class ServiceAdvisorProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer advisorId;
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String department;
    
    private LocalDate hireDate;
    
    private String specialization;
    
    // This field won't be stored in DB but used for displaying formatted IDs
    @Transient
    private String formattedId;
    
    public String getFormattedId() {
        return "SA-" + String.format("%03d", this.advisorId);
    }
}