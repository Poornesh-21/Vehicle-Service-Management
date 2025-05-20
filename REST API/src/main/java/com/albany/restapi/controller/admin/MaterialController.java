package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.MaterialUsageDTO;
import com.albany.restapi.exception.InventoryExceptions.InsufficientStockException;
import com.albany.restapi.service.admin.MaterialService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/materials")
@RequiredArgsConstructor
public class MaterialController {
    
    private static final Logger logger = LoggerFactory.getLogger(MaterialController.class);
    private final MaterialService materialService;

    /**
     * Record material usage for a service request
     */
    @PostMapping("/usage")
    public ResponseEntity<?> recordMaterialUsage(
            @RequestParam("itemId") Integer itemId,
            @RequestParam("requestId") Integer requestId,
            @RequestParam("quantity") BigDecimal quantity) {
        try {
            MaterialUsageDTO usage = materialService.recordMaterialUsage(itemId, requestId, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body(usage);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (InsufficientStockException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error recording material usage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to record material usage: " + e.getMessage()));
        }
    }
    
    /**
     * Get all material usage records
     */
    @GetMapping("/usage")
    public ResponseEntity<List<MaterialUsageDTO>> getAllMaterialUsage() {
        try {
            List<MaterialUsageDTO> usages = materialService.getAllMaterialUsage();
            return ResponseEntity.ok(usages);
        } catch (Exception e) {
            logger.error("Error fetching material usage records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get material usage by ID
     */
    @GetMapping("/usage/{id}")
    public ResponseEntity<MaterialUsageDTO> getMaterialUsageById(@PathVariable("id") Integer usageId) {
        try {
            MaterialUsageDTO usage = materialService.getMaterialUsageById(usageId);
            return ResponseEntity.ok(usage);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching material usage with ID: " + usageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get material usage for a service request
     */
    @GetMapping("/usage/request/{requestId}")
    public ResponseEntity<List<MaterialUsageDTO>> getMaterialUsageForRequest(
            @PathVariable("requestId") Integer requestId) {
        try {
            List<MaterialUsageDTO> usages = materialService.getMaterialUsageForRequest(requestId);
            return ResponseEntity.ok(usages);
        } catch (Exception e) {
            logger.error("Error fetching material usage for request with ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}