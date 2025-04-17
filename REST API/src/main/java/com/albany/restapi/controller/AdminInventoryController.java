package com.albany.restapi.controller;

import com.albany.restapi.dto.InventoryItemDTO;
import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.service.InventoryController;
import com.albany.restapi.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-specific endpoints for inventory management
 * This controller provides dedicated endpoints that are accessible through the admin API path
 */
@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@Slf4j
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final InventoryController inventoryController;

    // Delegate to the main inventory controller to avoid code duplication
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<InventoryItemDTO>> getAllInventoryItems() {
        log.info("Admin API: Fetching all inventory items");
        return inventoryController.getAllInventoryItems();
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<InventoryItemDTO>> getItemsByCategory(@PathVariable String category) {
        log.info("Admin API: Fetching inventory items by category: {}", category);
        return inventoryController.getItemsByCategory(category);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<InventoryItemDTO>> getLowStockItems() {
        log.info("Admin API: Fetching low stock inventory items");
        return inventoryController.getLowStockItems();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<InventoryItemDTO> getItemById(@PathVariable Integer id) {
        log.info("Admin API: Fetching inventory item with ID: {}", id);
        return inventoryController.getItemById(id);
    }

    @GetMapping("/{id}/usage-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<MaterialUsageDTO>> getItemUsageHistory(@PathVariable Integer id) {
        log.info("Admin API: Fetching usage history for inventory item with ID: {}", id);
        return inventoryController.getItemUsageHistory(id);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Long>> getInventoryStats() {
        log.info("Admin API: Fetching inventory statistics");
        return inventoryController.getInventoryStats();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<InventoryItemDTO> createInventoryItem(@RequestBody Map<String, Object> request) {
        log.info("Admin API: Creating new inventory item: {}", request);
        return inventoryController.createInventoryItem(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<InventoryItemDTO> updateInventoryItem(
            @PathVariable Integer id, 
            @RequestBody Map<String, Object> request) {
        log.info("Admin API: Updating inventory item with ID: {}", id);
        return inventoryController.updateInventoryItem(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Integer id) {
        log.info("Admin API: Deleting inventory item with ID: {}", id);
        return inventoryController.deleteInventoryItem(id);
    }

    @PostMapping("/record-usage")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Void> recordMaterialUsage(@RequestBody Map<String, Object> request) {
        log.info("Admin API: Recording material usage: {}", request);
        return inventoryController.recordMaterialUsage(request);
    }
}