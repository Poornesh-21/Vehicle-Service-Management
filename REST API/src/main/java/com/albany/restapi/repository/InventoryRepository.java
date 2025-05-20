package com.albany.restapi.repository;

import com.albany.restapi.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Integer> {
    
    // Find all inventory items, sorted by name
    List<InventoryItem> findAllByOrderByNameAsc();
    
    // Find items with stock below or equal to reorder level
    @Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.reorderLevel")
    List<InventoryItem> findItemsWithLowStock();
    
    // Count items with stock below or equal to reorder level
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock <= i.reorderLevel")
    long countItemsWithLowStock();
    
    // Find items by category
    List<InventoryItem> findByCategoryOrderByNameAsc(String category);
    
    // Search items by name or category containing the search term
    @Query("SELECT i FROM InventoryItem i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(i.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<InventoryItem> searchByNameOrCategory(String searchTerm);
    
    // Calculate total inventory value
    @Query("SELECT SUM(i.currentStock * i.unitPrice) FROM InventoryItem i")
    BigDecimal calculateTotalInventoryValue();
}