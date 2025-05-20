package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.InventoryItemDTO;
import com.albany.restapi.dto.InventoryStatsDTO;
import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.service.admin.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    /**
     * Get all inventory items
     */
    @GetMapping("/api/items")
    public ResponseEntity<List<InventoryItemDTO>> getAllInventoryItems() {
        try {
            List<InventoryItemDTO> items = inventoryService.getAllInventoryItems();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching inventory items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get inventory item by ID
     */
    @GetMapping("/api/items/{id}")
    public ResponseEntity<InventoryItemDTO> getInventoryItemById(@PathVariable("id") Integer itemId) {
        try {
            InventoryItemDTO item = inventoryService.getInventoryItemById(itemId);
            return ResponseEntity.ok(item);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching inventory item with ID: " + itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new inventory item
     */
    @PostMapping
    public ResponseEntity<?> createInventoryItem(@Valid @RequestBody InventoryItemDTO itemDTO) {
        try {
            InventoryItemDTO newItem = inventoryService.createInventoryItem(itemDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(newItem);
        } catch (Exception e) {
            logger.error("Error creating inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create inventory item: " + e.getMessage()));
        }
    }

    /**
     * Update an existing inventory item
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInventoryItem(
            @PathVariable("id") Integer itemId,
            @Valid @RequestBody InventoryItemDTO itemDTO) {
        try {
            InventoryItemDTO updatedItem = inventoryService.updateInventoryItem(itemId, itemDTO);
            return ResponseEntity.ok(updatedItem);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating inventory item with ID: " + itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update inventory item: " + e.getMessage()));
        }
    }

    /**
     * Delete an inventory item
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInventoryItem(@PathVariable("id") Integer itemId) {
        try {
            inventoryService.deleteInventoryItem(itemId);
            return ResponseEntity.ok(Map.of("message", "Inventory item deleted successfully"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting inventory item with ID: " + itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete inventory item: " + e.getMessage()));
        }
    }

    /**
     * Get inventory statistics
     */
    @GetMapping("/api/stats")
    public ResponseEntity<InventoryStatsDTO> getInventoryStats() {
        try {
            InventoryStatsDTO stats = inventoryService.getInventoryStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching inventory statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get usage history for an inventory item
     */
    @GetMapping("/api/items/{id}/usage-history")
    public ResponseEntity<List<MaterialUsageDTO>> getItemUsageHistory(@PathVariable("id") Integer itemId) {
        try {
            List<MaterialUsageDTO> usageHistory = inventoryService.getItemUsageHistory(itemId);
            return ResponseEntity.ok(usageHistory);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching usage history for item with ID: " + itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search inventory items by name or category
     */
    @GetMapping("/api/search")
    public ResponseEntity<List<InventoryItemDTO>> searchInventoryItems(
            @RequestParam("term") String searchTerm) {
        try {
            List<InventoryItemDTO> items = inventoryService.searchInventoryItems(searchTerm);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error searching inventory items with term: " + searchTerm, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get inventory items by category
     */
    @GetMapping("/api/category/{category}")
    public ResponseEntity<List<InventoryItemDTO>> getItemsByCategory(
            @PathVariable("category") String category) {
        try {
            List<InventoryItemDTO> items = inventoryService.getItemsByCategory(category);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching inventory items by category: " + category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get low stock items
     */
    @GetMapping("/api/low-stock")
    public ResponseEntity<List<InventoryItemDTO>> getLowStockItems() {
        try {
            List<InventoryItemDTO> items = inventoryService.getLowStockItems();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching low stock items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}