package com.albany.restapi.repository;

import com.albany.restapi.model.MaterialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {
    // Use a list-based approach to avoid "unique result" errors
    List<MaterialUsage> findByServiceRequest_RequestIdOrderByUsedAtDesc(Integer requestId);
}