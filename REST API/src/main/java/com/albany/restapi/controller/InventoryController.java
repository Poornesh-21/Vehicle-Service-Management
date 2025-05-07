package com.albany.restapi.controller;

import com.albany.restapi.dto.InventoryItemDTO;
import com.albany.restapi.model.InventoryItem;
import com.albany.restapi.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Get all inventory items
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getAllInventoryItems() {
        log.info("Fetching all inventory items");
        List<InventoryItem> items = inventoryItemRepository.findAll();
        
        List<InventoryItemDTO> itemDTOs = items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(itemDTOs);
    }

    /**
     * Get inventory items by category
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getInventoryItemsByCategory(@PathVariable String category) {
        log.info("Fetching inventory items by category: {}", category);
        List<InventoryItem> items = inventoryItemRepository.findByCategory(category);
        
        List<InventoryItemDTO> itemDTOs = items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(itemDTOs);
    }

    /**
     * Get inventory items that are low on stock
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> getLowStockItems() {
        log.info("Fetching low stock inventory items");
        List<InventoryItem> items = inventoryItemRepository.findAllLowStock();
        
        List<InventoryItemDTO> itemDTOs = items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(itemDTOs);
    }

    /**
     * Search inventory items by name
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<InventoryItemDTO>> searchInventoryItems(@RequestParam String query) {
        log.info("Searching inventory items with query: {}", query);
        List<InventoryItem> items = inventoryItemRepository.findByNameContainingIgnoreCase(query);
        
        List<InventoryItemDTO> itemDTOs = items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(itemDTOs);
    }

    /**
     * Map InventoryItem entity to DTO
     */
    private InventoryItemDTO mapToDTO(InventoryItem item) {
        // Calculate total value
        BigDecimal totalValue = item.getCurrentStock().multiply(item.getUnitPrice());
        
        // Determine stock status
        String stockStatus;
        if (item.getCurrentStock().compareTo(item.getReorderLevel()) <= 0) {
            stockStatus = "Low";
        } else if (item.getCurrentStock().compareTo(item.getReorderLevel().multiply(new BigDecimal("2"))) <= 0) {
            stockStatus = "Medium";
        } else {
            stockStatus = "Good";
        }
        
        return InventoryItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .category(item.getCategory())
                .currentStock(item.getCurrentStock())
                .unitPrice(item.getUnitPrice())
                .reorderLevel(item.getReorderLevel())
                .stockStatus(stockStatus)
                .totalValue(totalValue)
                .build();
    }
}