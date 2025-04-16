package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceAdvisorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceAdvisorProfileRepository extends JpaRepository<ServiceAdvisorProfile, Integer> {
    // Change this method to use the correct property name with underscore notation
    Optional<ServiceAdvisorProfile> findByUser_UserId(Integer userId);

    @Query("SELECT sa FROM ServiceAdvisorProfile sa JOIN sa.user u WHERE u.isActive = true")
    List<ServiceAdvisorProfile> findAllActive();

    @Query("SELECT COUNT(sa) FROM ServiceAdvisorProfile sa")
    long countServiceAdvisors();
}