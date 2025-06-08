package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTrackingRepository extends JpaRepository<ServiceTracking, Integer> {
    // Use a list-based approach to avoid "unique result" errors
    List<ServiceTracking> findByServiceRequest_RequestIdOrderByUpdatedAtDesc(Integer requestId);
}