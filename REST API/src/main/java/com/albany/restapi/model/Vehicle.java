package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Vehicles")
public class Vehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer vehicleId;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;
    
    private String registrationNumber;
    
    @Enumerated(EnumType.STRING)
    private Category category;
    
    private String brand;
    
    private String model;
    
    private Integer year;
    
    public enum Category {
        Bike, Car, Truck
    }
}