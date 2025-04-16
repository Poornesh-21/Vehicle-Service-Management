package com.albany.restapi.service;

import com.albany.restapi.dto.InventoryItemDTO;
import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getAllInventoryItems() {
        log.info("Fetching all inventory items");
        return ResponseEntity.ok(inventoryService.getAllInventoryItems());
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getItemsByCategory(@PathVariable String category) {
        log.info("Fetching inventory items by category: {}", category);
        return ResponseEntity.ok(inventoryService.getInventoryItemsByCategory(category));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getLowStockItems() {
        log.info("Fetching low stock inventory items");
        return ResponseEntity.ok(inventoryService.getLowStockItems());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<InventoryItemDTO> getItemById(@PathVariable Integer id) {
        log.info("Fetching inventory item with ID: {}", id);
        return ResponseEntity.ok(inventoryService.getInventoryItemById(id));
    }

    @GetMapping("/{id}/usage-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<MaterialUsageDTO>> getItemUsageHistory(@PathVariable Integer id) {
        log.info("Fetching usage history for inventory item with ID: {}", id);
        return ResponseEntity.ok(inventoryService.getRecentUsagesByItemId(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<Map<String, Long>> getInventoryStats() {
        log.info("Fetching inventory statistics");
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalItems", inventoryService.countTotalItems());
        stats.put("lowStockItems", inventoryService.countLowStockItems());
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<InventoryItemDTO> createInventoryItem(@RequestBody Map<String, Object> request) {
        log.info("Creating new inventory item: {}", request);
        InventoryItemDTO item = inventoryService.createInventoryItem(request);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<InventoryItemDTO> updateInventoryItem(
            @PathVariable Integer id, 
            @RequestBody Map<String, Object> request) {
        log.info("Updating inventory item with ID: {}", id);
        InventoryItemDTO item = inventoryService.updateInventoryItem(id, request);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Integer id) {
        log.info("Deleting inventory item with ID: {}", id);
        inventoryService.deleteInventoryItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/record-usage")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<Void> recordMaterialUsage(@RequestBody Map<String, Object> request) {
        log.info("Recording material usage: {}", request);
        
        // Extract request parameters
        Integer requestId = Integer.valueOf(request.get("requestId").toString());
        Integer itemId = Integer.valueOf(request.get("itemId").toString());
        BigDecimal quantity = new BigDecimal(request.get("quantity").toString());
        
        inventoryService.recordMaterialUsage(requestId, itemId, quantity);
        return ResponseEntity.ok().build();
    }
}