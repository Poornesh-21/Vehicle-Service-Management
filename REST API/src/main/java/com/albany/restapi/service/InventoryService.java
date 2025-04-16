package com.albany.restapi.service;

import com.albany.restapi.dto.InventoryItemDTO;
import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.model.InventoryItem;
import com.albany.restapi.model.MaterialUsage;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.repository.InventoryItemRepository;
import com.albany.restapi.repository.MaterialUsageRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public List<InventoryItemDTO> getAllInventoryItems() {
        return inventoryItemRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InventoryItemDTO> getInventoryItemsByCategory(String category) {
        return inventoryItemRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InventoryItemDTO> getLowStockItems() {
        return inventoryItemRepository.findAllLowStock().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public InventoryItemDTO getInventoryItemById(Integer id) {
        return inventoryItemRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
    }

    @Transactional
    public InventoryItemDTO createInventoryItem(Map<String, Object> request) {
        // Validate request
        validateInventoryRequest(request, false);

        // Create new inventory item
        InventoryItem item = new InventoryItem();
        updateInventoryItemFromRequest(item, request);

        // Save and return
        InventoryItem savedItem = inventoryItemRepository.save(item);
        return convertToDTO(savedItem);
    }

    @Transactional
    public InventoryItemDTO updateInventoryItem(Integer id, Map<String, Object> request) {
        // Validate request
        validateInventoryRequest(request, true);

        // Find existing item
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        // Update fields
        updateInventoryItemFromRequest(item, request);

        // Save and return
        InventoryItem updatedItem = inventoryItemRepository.save(item);
        return convertToDTO(updatedItem);
    }

    @Transactional
    public void deleteInventoryItem(Integer id) {
        if (!inventoryItemRepository.existsById(id)) {
            throw new RuntimeException("Inventory item not found");
        }

        // Check if item is used in any service request
        List<MaterialUsage> usages = materialUsageRepository.findByInventoryItem_ItemId(id);
        if (!usages.isEmpty()) {
            throw new RuntimeException("Cannot delete item as it is used in service requests");
        }

        inventoryItemRepository.deleteById(id);
    }

    public List<MaterialUsageDTO> getRecentUsagesByItemId(Integer itemId) {
        return materialUsageRepository.findRecentUsagesByItemId(itemId).stream()
                .map(this::convertUsageToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void recordMaterialUsage(Integer requestId, Integer itemId, BigDecimal quantity) {
        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find inventory item
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        // Check if there's enough stock
        if (item.getCurrentStock().compareTo(quantity) < 0) {
            throw new RuntimeException("Not enough stock available");
        }

        // Update stock
        item.setCurrentStock(item.getCurrentStock().subtract(quantity));
        inventoryItemRepository.save(item);

        // Record usage
        MaterialUsage usage = new MaterialUsage();
        usage.setServiceRequest(request);
        usage.setInventoryItem(item);
        usage.setQuantity(quantity);

        materialUsageRepository.save(usage);
    }

    // Helper methods
    private void validateInventoryRequest(Map<String, Object> request, boolean isUpdate) {
        // Check required fields
        if (!request.containsKey("name") || request.get("name") == null) {
            throw new RuntimeException("Item name is required");
        }

        if (!request.containsKey("category") || request.get("category") == null) {
            throw new RuntimeException("Category is required");
        }

        if (!request.containsKey("currentStock") || request.get("currentStock") == null) {
            throw new RuntimeException("Current stock is required");
        }

        if (!request.containsKey("unitPrice") || request.get("unitPrice") == null) {
            throw new RuntimeException("Unit price is required");
        }

        if (!request.containsKey("reorderLevel") || request.get("reorderLevel") == null) {
            throw new RuntimeException("Reorder level is required");
        }

        // Check for duplicate name on create
        if (!isUpdate && inventoryItemRepository.existsByNameIgnoreCase(request.get("name").toString())) {
            throw new RuntimeException("An item with this name already exists");
        }
    }

    private void updateInventoryItemFromRequest(InventoryItem item, Map<String, Object> request) {
        item.setName(request.get("name").toString());
        item.setCategory(request.get("category").toString());
        
        // Parse and set numeric values
        try {
            item.setCurrentStock(new BigDecimal(request.get("currentStock").toString()));
            item.setUnitPrice(new BigDecimal(request.get("unitPrice").toString()));
            item.setReorderLevel(new BigDecimal(request.get("reorderLevel").toString()));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid numeric value in request");
        }
    }

    private InventoryItemDTO convertToDTO(InventoryItem item) {
        // Calculate total value
        BigDecimal totalValue = item.getCurrentStock().multiply(item.getUnitPrice());
        
        // Determine stock status
        String stockStatus;
        if (item.getCurrentStock().compareTo(item.getReorderLevel().multiply(new BigDecimal("0.5"))) <= 0) {
            stockStatus = "Low";
        } else if (item.getCurrentStock().compareTo(item.getReorderLevel()) <= 0) {
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

    private MaterialUsageDTO convertUsageToDTO(MaterialUsage usage) {
        return MaterialUsageDTO.builder()
                .materialUsageId(usage.getMaterialUsageId())
                .requestId(usage.getServiceRequest().getRequestId())
                .requestReference("REQ-" + usage.getServiceRequest().getRequestId())
                .itemId(usage.getInventoryItem().getItemId())
                .itemName(usage.getInventoryItem().getName())
                .quantity(usage.getQuantity())
                .usedAt(usage.getUsedAt())
                .serviceAdvisorName(usage.getServiceRequest().getServiceAdvisor() != null ? 
                        usage.getServiceRequest().getServiceAdvisor().getUser().getFirstName() + " " + 
                        usage.getServiceRequest().getServiceAdvisor().getUser().getLastName() : "Not Assigned")
                .build();
    }

    public long countTotalItems() {
        return inventoryItemRepository.count();
    }

    public long countLowStockItems() {
        return inventoryItemRepository.countLowStockItems();
    }
}