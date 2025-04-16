package com.albany.restapi.repository;

import com.albany.restapi.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {
    
    List<InventoryItem> findByCategory(String category);
    
    List<InventoryItem> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.reorderLevel")
    List<InventoryItem> findAllLowStock();
    
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= i.reorderLevel")
    long countLowStockItems();
    
    boolean existsByNameIgnoreCase(String name);
}