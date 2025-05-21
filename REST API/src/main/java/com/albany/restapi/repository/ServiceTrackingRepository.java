package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing ServiceTracking entities
 */
@Repository
public interface ServiceTrackingRepository extends JpaRepository<ServiceTracking, Integer> {
    
    /**
     * Find service tracking records by request ID
     */
    List<ServiceTracking> findByServiceRequest_RequestIdOrderByUpdatedAtDesc(Integer requestId);
    
    /**
     * Find service tracking records by advisor ID
     */
    List<ServiceTracking> findByServiceAdvisor_AdvisorIdOrderByUpdatedAtDesc(Integer advisorId);
}