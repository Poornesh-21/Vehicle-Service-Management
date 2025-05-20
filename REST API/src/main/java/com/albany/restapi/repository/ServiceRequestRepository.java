package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {
    List<ServiceRequest> findByServiceAdvisor_AdvisorId(Integer advisorId);
    
    @Query("SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.serviceAdvisor.advisorId = :advisorId AND sr.status <> 'Completed'")
    long countActiveServicesByAdvisorId(@Param("advisorId") Integer advisorId);
}