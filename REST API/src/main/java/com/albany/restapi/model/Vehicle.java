package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    
    public enum Category {
        Car,
        Bike,
        Truck
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer vehicleId;
    
    private String brand;
    private String model;
    private Integer year;
    private String registrationNumber;
    
    @Enumerated(EnumType.STRING)
    private Category category;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;
}