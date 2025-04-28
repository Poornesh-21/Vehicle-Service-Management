package com.albany.restapi.repository;

import com.albany.restapi.model.MechanicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MechanicProfileRepository extends JpaRepository<MechanicProfile, Integer> {
    
    // Find by user ID
    Optional<MechanicProfile> findByUser_UserId(Integer userId);
    
    // Find all active mechanics
    @Query("SELECT m FROM MechanicProfile m JOIN m.user u WHERE u.isActive = true")
    List<MechanicProfile> findAllActive();
    
    // Count total mechanics
    @Query("SELECT COUNT(m) FROM MechanicProfile m")
    long countMechanics();
    
    // Find by specialization
    List<MechanicProfile> findBySpecialization(String specialization);
}