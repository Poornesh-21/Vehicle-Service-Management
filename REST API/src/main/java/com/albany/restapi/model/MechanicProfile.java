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
@Table(name = "mechanic_profiles")
public class MechanicProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mechanicId;

    @OneToOne
    @JoinColumn(name = "userId") // Ensure this exactly matches the column name in the database
    private User user;

    private String department;

    private LocalDate hireDate;

    private String specialization;

    private Integer experienceYears;

    // This field won't be stored in DB but used for displaying formatted IDs
    @Transient
    private String formattedId;

    public String getFormattedId() {
        return "MC-" + String.format("%03d", this.mechanicId);
    }
}