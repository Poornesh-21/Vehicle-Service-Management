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
@Table(name = "CustomerProfiles")
public class CustomerProfile {

    @Id
    private Integer customerId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String street;

    private String city;

    private String state;

    private String postalCode;

    @Column(name = "total_services")
    private Integer totalServices = 0;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    // No default value, this will ensure it uses exactly what's in the database
    @Column(name = "membership_status")
    private String membershipStatus;
}