package com.albany.restapi.service.admin;

import com.albany.restapi.dto.InventoryItemDTO;
import com.albany.restapi.dto.InventoryStatsDTO;
import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.model.InventoryItem;
import com.albany.restapi.model.MaterialUsage;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.repository.InventoryRepository;
import com.albany.restapi.repository.MaterialUsageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MaterialUsageRepository materialUsageRepository;

    /**
     * Get all inventory items
     */
    @Cacheable("inventoryItems")
    public List<InventoryItemDTO> getAllInventoryItems() {
        return inventoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get inventory item by ID
     */
    public InventoryItemDTO getInventoryItemById(Integer itemId) {
        InventoryItem item = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found with id: " + itemId));
        
        return mapToDTO(item);
    }

    /**
     * Create a new inventory item
     */
    @Transactional
    @CacheEvict(value = {"inventoryItems", "inventoryStats"}, allEntries = true)
    public InventoryItemDTO createInventoryItem(InventoryItemDTO itemDTO) {
        InventoryItem item = InventoryItem.builder()
                .name(itemDTO.getName())
                .category(itemDTO.getCategory())
                .currentStock(itemDTO.getCurrentStock())
                .unitPrice(itemDTO.getUnitPrice())
                .reorderLevel(itemDTO.getReorderLevel())
                .build();

        InventoryItem savedItem = inventoryRepository.save(item);
        return mapToDTO(savedItem);
    }

    /**
     * Update an existing inventory item
     */
    @Transactional
    @CacheEvict(value = {"inventoryItems", "inventoryStats"}, allEntries = true)
    public InventoryItemDTO updateInventoryItem(Integer itemId, InventoryItemDTO itemDTO) {
        InventoryItem existingItem = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found with id: " + itemId));

        existingItem.setName(itemDTO.getName());
        existingItem.setCategory(itemDTO.getCategory());
        existingItem.setCurrentStock(itemDTO.getCurrentStock());
        existingItem.setUnitPrice(itemDTO.getUnitPrice());
        existingItem.setReorderLevel(itemDTO.getReorderLevel());

        InventoryItem updatedItem = inventoryRepository.save(existingItem);
        return mapToDTO(updatedItem);
    }

    /**
     * Delete an inventory item
     */
    @Transactional
    @CacheEvict(value = {"inventoryItems", "inventoryStats"}, allEntries = true)
    public void deleteInventoryItem(Integer itemId) {
        if (!inventoryRepository.existsById(itemId)) {
            throw new EntityNotFoundException("Inventory item not found with id: " + itemId);
        }
        
        // Check if the item has been used (has usage history)
        List<MaterialUsage> usages = materialUsageRepository.findByInventoryItem_ItemIdOrderByUsedAtDesc(itemId);
        if (!usages.isEmpty()) {
            throw new IllegalStateException("Cannot delete inventory item that has been used in service requests");
        }
        
        inventoryRepository.deleteById(itemId);
    }

    /**
     * Get inventory statistics
     */
    @Cacheable("inventoryStats")
    public InventoryStatsDTO getInventoryStats() {
        long totalItems = inventoryRepository.count();
        long lowStockItems = inventoryRepository.countItemsWithLowStock();
        BigDecimal totalValue = inventoryRepository.calculateTotalInventoryValue();
        
        if (totalValue == null) {
            totalValue = BigDecimal.ZERO;
        }

        return InventoryStatsDTO.builder()
                .totalItems(totalItems)
                .lowStockItems(lowStockItems)
                .totalInventoryValue(totalValue)
                .build();
    }

    /**
     * Get usage history for an inventory item
     */
    public List<MaterialUsageDTO> getItemUsageHistory(Integer itemId) {
        if (!inventoryRepository.existsById(itemId)) {
            throw new EntityNotFoundException("Inventory item not found with id: " + itemId);
        }

        return materialUsageRepository.findByInventoryItem_ItemIdOrderByUsedAtDesc(itemId).stream()
                .map(this::mapUsageToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search inventory items by name or category
     */
    public List<InventoryItemDTO> searchInventoryItems(String searchTerm) {
        return inventoryRepository.searchByNameOrCategory(searchTerm).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get inventory items by category
     */
    public List<InventoryItemDTO> getItemsByCategory(String category) {
        return inventoryRepository.findByCategoryOrderByNameAsc(category).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get low stock items
     */
    public List<InventoryItemDTO> getLowStockItems() {
        return inventoryRepository.findItemsWithLowStock().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map entity to DTO
     */
    private InventoryItemDTO mapToDTO(InventoryItem item) {
        // Ensure derived fields are calculated
        item.calculateDerivedFields();
        
        return InventoryItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .category(item.getCategory())
                .currentStock(item.getCurrentStock())
                .unitPrice(item.getUnitPrice())
                .reorderLevel(item.getReorderLevel())
                .totalValue(item.getTotalValue())
                .stockStatus(item.getStockStatus())
                .build();
    }

    /**
     * Map usage entity to DTO
     */
    private MaterialUsageDTO mapUsageToDTO(MaterialUsage usage) {
        ServiceRequest request = usage.getServiceRequest();
        String requestReference = request != null ? "REQ-" + request.getRequestId() : "N/A";
        
        String advisorName = "N/A";
        if (request != null && request.getServiceAdvisor() != null && 
            request.getServiceAdvisor().getUser() != null) {
            advisorName = request.getServiceAdvisor().getUser().getFirstName() + " " + 
                          request.getServiceAdvisor().getUser().getLastName();
        }
        
        return MaterialUsageDTO.builder()
                .materialUsageId(usage.getMaterialUsageId())
                .requestReference(requestReference)
                .usedAt(usage.getUsedAt())
                .quantity(usage.getQuantity())
                .serviceAdvisorName(advisorName)
                .build();
    }
}