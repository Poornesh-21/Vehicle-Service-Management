package com.albany.restapi.repository;

import com.albany.restapi.model.MaterialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {
    // List-based approach for service requests
    List<MaterialUsage> findByServiceRequest_RequestIdOrderByUsedAtDesc(Integer requestId);

    // Add the missing method for inventory items
    List<MaterialUsage> findByInventoryItem_ItemIdOrderByUsedAtDesc(Integer itemId);
}