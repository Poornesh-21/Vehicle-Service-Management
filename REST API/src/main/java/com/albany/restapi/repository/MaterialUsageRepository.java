package com.albany.restapi.repository;

import com.albany.restapi.model.MaterialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {
    
    // Find usage history for an inventory item
    List<MaterialUsage> findByInventoryItem_ItemIdOrderByUsedAtDesc(Integer itemId);
    
    // Find usage history for a service request
    List<MaterialUsage> findByServiceRequest_RequestIdOrderByUsedAtDesc(Integer requestId);
    
    // Get total quantity used for an item
    @Query("SELECT SUM(m.quantity) FROM MaterialUsage m WHERE m.inventoryItem.itemId = :itemId")
    Double getTotalQuantityUsedForItem(@Param("itemId") Integer itemId);
}