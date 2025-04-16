package com.albany.restapi.repository;

import com.albany.restapi.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    
    List<Vehicle> findByCustomer_CustomerId(Integer customerId);
    
    boolean existsByRegistrationNumber(String registrationNumber);
}