package com.albany.restapi.service.admin;

import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.exception.InventoryExceptions;
import com.albany.restapi.model.InventoryItem;
import com.albany.restapi.model.MaterialUsage;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.repository.InventoryRepository;
import com.albany.restapi.repository.MaterialUsageRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling material usage operations
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryRepository inventoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    /**
     * Record material usage for a service request
     */
    @Transactional
    @CacheEvict(value = {"inventoryItems", "inventoryStats"}, allEntries = true)
    public MaterialUsageDTO recordMaterialUsage(Integer itemId, Integer requestId, BigDecimal quantity) {
        // Validate inventory item exists
        InventoryItem item = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found with id: " + itemId));
        
        // Validate service request exists
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service request not found with id: " + requestId));
        
        // Check if there's enough stock
        if (item.getCurrentStock().compareTo(quantity) < 0) {
            throw new InventoryExceptions.InsufficientStockException(
                    item.getName(), 
                    item.getItemId(), 
                    quantity.doubleValue(), 
                    item.getCurrentStock().doubleValue()
            );
        }
        
        // Update inventory stock
        item.setCurrentStock(item.getCurrentStock().subtract(quantity));
        inventoryRepository.save(item);
        
        // Record usage
        MaterialUsage usage = MaterialUsage.builder()
                .inventoryItem(item)
                .serviceRequest(request)
                .quantity(quantity)
                .usedAt(LocalDateTime.now())
                .build();
        
        MaterialUsage savedUsage = materialUsageRepository.save(usage);
        
        // Map to DTO and return
        return mapToDTO(savedUsage);
    }
    
    /**
     * Get all material usage records
     */
    public List<MaterialUsageDTO> getAllMaterialUsage() {
        return materialUsageRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get material usage by ID
     */
    public MaterialUsageDTO getMaterialUsageById(Integer usageId) {
        MaterialUsage usage = materialUsageRepository.findById(usageId)
                .orElseThrow(() -> new EntityNotFoundException("Material usage not found with id: " + usageId));
        
        return mapToDTO(usage);
    }
    
    /**
     * Get material usage for a service request
     */
    public List<MaterialUsageDTO> getMaterialUsageForRequest(Integer requestId) {
        return materialUsageRepository.findByServiceRequest_RequestIdOrderByUsedAtDesc(requestId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Map entity to DTO
     */
    private MaterialUsageDTO mapToDTO(MaterialUsage usage) {
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