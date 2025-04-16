package com.albany.restapi.repository;

import com.albany.restapi.model.MaterialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {
    
    List<MaterialUsage> findByInventoryItem_ItemId(Integer itemId);
    
    List<MaterialUsage> findByServiceRequest_RequestId(Integer requestId);
    
    @Query("SELECT mu FROM MaterialUsage mu WHERE mu.inventoryItem.itemId = :itemId ORDER BY mu.usedAt DESC LIMIT 10")
    List<MaterialUsage> findRecentUsagesByItemId(@Param("itemId") Integer itemId);
}