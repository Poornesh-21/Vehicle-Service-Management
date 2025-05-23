package com.albany.restapi.service.serviceAdvisor;

import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialItemDTO;
import com.albany.restapi.dto.MaterialRequestDTO;
import com.albany.restapi.dto.ServiceDetailDTO;
import com.albany.restapi.dto.ServiceStatusUpdateDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.albany.restapi.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAdvisorDashboardService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final ServiceAdvisorRepository serviceAdvisorRepository;
    private final InventoryRepository inventoryRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * Get all vehicles assigned to the service advisor
     */
    public List<Map<String, Object>> getAssignedVehicles(String token) {
        Integer advisorId = getAdvisorIdFromToken(token);
        List<ServiceRequest> requests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisorId);
        
        return requests.stream()
                .filter(request -> request.getStatus() != ServiceRequest.Status.Completed)
                .map(request -> {
                    Map<String, Object> vehicle = new HashMap<>();
                    vehicle.put("requestId", request.getRequestId());
                    
                    String vehicleName = "";
                    if (request.getVehicleBrand() != null && request.getVehicleModel() != null) {
                        vehicleName = request.getVehicleBrand() + " " + request.getVehicleModel();
                        if (request.getVehicleYear() != null) {
                            vehicleName += " (" + request.getVehicleYear() + ")";
                        }
                    }
                    vehicle.put("vehicleName", vehicleName);
                    vehicle.put("vehicleBrand", request.getVehicleBrand());
                    vehicle.put("vehicleModel", request.getVehicleModel());
                    vehicle.put("registrationNumber", request.getVehicleRegistration());
                    
                    // Get customer details
                    User user = null;
                    if (request.getUserId() != null) {
                        user = userRepository.findById(request.getUserId().intValue()).orElse(null);
                    }
                    
                    if (user != null) {
                        vehicle.put("customerName", user.getFirstName() + " " + user.getLastName());
                        vehicle.put("customerEmail", user.getEmail());
                        vehicle.put("customerPhone", user.getPhoneNumber());
                    } else {
                        vehicle.put("customerName", "Unknown Customer");
                        vehicle.put("customerEmail", "No Email");
                    }
                    
                    vehicle.put("serviceType", request.getServiceType());
                    vehicle.put("startDate", request.getCreatedAt());
                    vehicle.put("status", request.getStatus().name());
                    
                    return vehicle;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get details for a specific service request
     */
    public ServiceDetailDTO getServiceDetails(Integer requestId, String token) {
        Integer advisorId = getAdvisorIdFromToken(token);
        ServiceRequest request = findServiceRequestWithPermissionCheck(requestId, advisorId);
        
        ServiceDetailDTO details = new ServiceDetailDTO();
        details.setRequestId(request.getRequestId());
        details.setVehicleBrand(request.getVehicleBrand());
        details.setVehicleModel(request.getVehicleModel());
        details.setVehicleYear(request.getVehicleYear());
        details.setVehicleType(request.getVehicleType());
        details.setRegistrationNumber(request.getVehicleRegistration());
        details.setServiceType(request.getServiceType());
        details.setStatus(request.getStatus().name());
        details.setAdditionalDescription(request.getAdditionalDescription());
        details.setRequestDate(request.getCreatedAt());
        
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId().intValue()).orElse(null);
        }
        
        if (user != null) {
            details.setCustomerName(user.getFirstName() + " " + user.getLastName());
            details.setCustomerEmail(user.getEmail());
            details.setCustomerPhone(user.getPhoneNumber());
            details.setMembershipStatus(user.getMembershipType().name());
        }
        
        ServiceAdvisorProfile advisor = request.getServiceAdvisor();
        if (advisor != null && advisor.getUser() != null) {
            details.setServiceAdvisor(advisor.getUser().getFirstName() + " " + advisor.getUser().getLastName());
        }
        
        // Get current tracking information
        ServiceTracking tracking = getOrCreateServiceTracking(request, advisor);
        if (tracking != null) {
            details.setLaborMinutes(tracking.getLaborMinutes());
            details.setLaborCost(tracking.getLaborCost());
            details.setTotalMaterialCost(tracking.getTotalMaterialCost());
            details.setWorkDescription(tracking.getWorkDescription());
            details.setLastStatusUpdate(tracking.getUpdatedAt());
        }
        
        // Get materials used
        List<MaterialUsage> materialsUsed = materialUsageRepository
                .findByServiceRequest_RequestIdOrderByUsedAtDesc(requestId);
        
        if (!materialsUsed.isEmpty()) {
            Map<String, Object> currentBill = new HashMap<>();
            
            List<Map<String, Object>> materials = materialsUsed.stream()
                .filter(material -> material.getInventoryItem() != null)
                .map(material -> {
                    Map<String, Object> item = new HashMap<>();
                    InventoryItem inventoryItem = material.getInventoryItem();
                    
                    item.put("itemId", inventoryItem.getItemId());
                    item.put("name", inventoryItem.getName());
                    item.put("quantity", material.getQuantity());
                    item.put("unitPrice", inventoryItem.getUnitPrice());
                    
                    return item;
                }).collect(Collectors.toList());
            
            currentBill.put("materials", materials);
            
            // Extract labor charges from tracking
            if (tracking != null && tracking.getLaborMinutes() != null && tracking.getLaborCost() != null) {
                List<Map<String, Object>> laborCharges = new ArrayList<>();
                
                if (tracking.getLaborMinutes() > 0) {
                    Map<String, Object> laborCharge = new HashMap<>();
                    
                    double hours = tracking.getLaborMinutes() / 60.0;
                    
                    BigDecimal hourDecimal = BigDecimal.valueOf(hours)
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    
                    BigDecimal ratePerHour;
                    if (hourDecimal.compareTo(BigDecimal.ZERO) > 0) {
                        ratePerHour = tracking.getLaborCost()
                                .divide(hourDecimal, 2, java.math.RoundingMode.HALF_UP);
                    } else {
                        ratePerHour = BigDecimal.ZERO;
                    }
                    
                    laborCharge.put("description", "Labor Charge");
                    laborCharge.put("hours", hourDecimal);
                    laborCharge.put("ratePerHour", ratePerHour);
                    
                    laborCharges.add(laborCharge);
                }
                
                currentBill.put("laborCharges", laborCharges);
                
                // Calculate totals
                BigDecimal partsSubtotal = tracking.getTotalMaterialCost() != null ? 
                        tracking.getTotalMaterialCost() : BigDecimal.ZERO;
                BigDecimal laborSubtotal = tracking.getLaborCost() != null ? 
                        tracking.getLaborCost() : BigDecimal.ZERO;
                BigDecimal subtotal = partsSubtotal.add(laborSubtotal);
                BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.07))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal total = subtotal.add(tax);
                
                currentBill.put("partsSubtotal", partsSubtotal);
                currentBill.put("laborSubtotal", laborSubtotal);
                currentBill.put("subtotal", subtotal);
                currentBill.put("tax", tax);
                currentBill.put("total", total);
                currentBill.put("notes", tracking.getWorkDescription());
            }
            
            details.setCurrentBill(currentBill);
        }
        
        return details;
    }

    /**
     * Get all available inventory items
     */
    public List<Map<String, Object>> getAvailableInventoryItems(String token) {
        getAdvisorIdFromToken(token); // Just verify the token is valid
        
        return inventoryRepository.findAllByOrderByNameAsc().stream()
                .map(item -> {
                    Map<String, Object> inventoryItem = new HashMap<>();
                    inventoryItem.put("itemId", item.getItemId());
                    inventoryItem.put("name", item.getName());
                    inventoryItem.put("category", item.getCategory());
                    inventoryItem.put("currentStock", item.getCurrentStock());
                    inventoryItem.put("unitPrice", item.getUnitPrice());
                    
                    // Calculate stock status if not already done
                    if (item.getStockStatus() == null) {
                        item.calculateDerivedFields();
                    }
                    
                    inventoryItem.put("stockStatus", item.getStockStatus());
                    return inventoryItem;
                })
                .collect(Collectors.toList());
    }

    /**
     * Update the status of a service request
     */
    @Transactional
    public Map<String, Object> updateServiceStatus(Integer requestId, ServiceStatusUpdateDTO statusUpdate, String token) {
        Integer advisorId = getAdvisorIdFromToken(token);
        ServiceRequest request = findServiceRequestWithPermissionCheck(requestId, advisorId);
        ServiceAdvisorProfile advisor = request.getServiceAdvisor();
        
        ServiceRequest.Status newStatus;
        try {
            newStatus = ServiceRequest.Status.valueOf(statusUpdate.getStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusUpdate.getStatus());
        }
        
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());
        serviceRequestRepository.save(request);
        
        ServiceTracking tracking = getOrCreateServiceTracking(request, advisor);
        tracking.setStatus(newStatus);
        tracking.setUpdatedAt(LocalDateTime.now());
        
        if (statusUpdate.getNotes() != null && !statusUpdate.getNotes().isEmpty()) {
            tracking.setWorkDescription(statusUpdate.getNotes());
        }
        
        serviceTrackingRepository.save(tracking);
        
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", request.getRequestId());
        result.put("status", request.getStatus().name());
        result.put("message", "Status updated successfully");
        
        return result;
    }

    /**
     * Update labor charges for a service request
     */
    @Transactional
    public Map<String, Object> updateLaborCharges(Integer requestId, List<LaborChargeDTO> laborCharges, String token) {
        // Get the service advisor ID from the token
        Integer advisorId = getAdvisorIdFromToken(token);
        
        // Find the service request and verify permissions
        ServiceRequest request = findServiceRequestWithPermissionCheck(requestId, advisorId);
        ServiceAdvisorProfile advisor = request.getServiceAdvisor();
        
        // Initialize totals
        int totalMinutes = 0;
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        
        // Process labor charges
        if (laborCharges != null && !laborCharges.isEmpty()) {
            for (LaborChargeDTO charge : laborCharges) {
                if (charge == null) continue;
                
                // Get hours and rate values, default to zero if null
                BigDecimal hours = charge.getHours() != null ? charge.getHours() : BigDecimal.ZERO;
                BigDecimal rate = charge.getRate() != null ? charge.getRate() : BigDecimal.ZERO;
                
                // Skip invalid entries
                if (hours.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                
                // Convert hours to minutes
                int minutes = hours.multiply(BigDecimal.valueOf(60))
                        .setScale(0, java.math.RoundingMode.HALF_UP)
                        .intValue();
                
                // Calculate cost
                BigDecimal cost = hours.multiply(rate)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                
                // Add to totals
                totalMinutes += minutes;
                totalLaborCost = totalLaborCost.add(cost);
            }
        }
        
        // Get or create service tracking record
        ServiceTracking tracking = null;
        List<ServiceTracking> existingTrackings = serviceTrackingRepository
                .findByServiceRequest_RequestIdOrderByUpdatedAtDesc(requestId);
        
        if (!existingTrackings.isEmpty()) {
            tracking = existingTrackings.get(0);
        } else {
            // Create new tracking record
            tracking = new ServiceTracking();
            tracking.setServiceRequest(request);
            tracking.setServiceAdvisor(advisor);
            tracking.setStatus(request.getStatus());
            tracking.setTotalMaterialCost(BigDecimal.ZERO);
        }
        
        // Update labor information
        tracking.setLaborMinutes(totalMinutes);
        tracking.setLaborCost(totalLaborCost);
        tracking.setUpdatedAt(LocalDateTime.now());
        
        // Save the tracking record
        tracking = serviceTrackingRepository.save(tracking);
        
        // Force flush to ensure immediate write to database
        serviceTrackingRepository.flush();
        
        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", request.getRequestId());
        result.put("trackingId", tracking.getTrackingId());
        result.put("laborMinutes", tracking.getLaborMinutes());
        result.put("laborCost", tracking.getLaborCost());
        result.put("message", "Labor charges updated successfully");
        
        return result;
    }

    /**
     * Get labor charges for a service request
     */
    public Map<String, Object> getLaborCharges(Integer requestId, String token) {
        Integer advisorId = getAdvisorIdFromToken(token);
        ServiceRequest request = findServiceRequestWithPermissionCheck(requestId, advisorId);
        
        List<ServiceTracking> trackings = serviceTrackingRepository
                .findByServiceRequest_RequestIdOrderByUpdatedAtDesc(requestId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", requestId);
        
        if (!trackings.isEmpty()) {
            ServiceTracking tracking = trackings.get(0);
            
            result.put("trackingId", tracking.getTrackingId());
            result.put("laborMinutes", tracking.getLaborMinutes());
            result.put("laborCost", tracking.getLaborCost());
            
            // Convert minutes to hours for display
            if (tracking.getLaborMinutes() != null && tracking.getLaborMinutes() > 0) {
                double hours = tracking.getLaborMinutes() / 60.0;
                BigDecimal hourDecimal = BigDecimal.valueOf(hours)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                result.put("laborHours", hourDecimal);
                
                // Calculate hourly rate if possible
                if (tracking.getLaborCost() != null && tracking.getLaborCost().compareTo(BigDecimal.ZERO) > 0 
                        && hourDecimal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal hourlyRate = tracking.getLaborCost()
                            .divide(hourDecimal, 2, java.math.RoundingMode.HALF_UP);
                    result.put("hourlyRate", hourlyRate);
                } else {
                    result.put("hourlyRate", BigDecimal.ZERO);
                }
            } else {
                result.put("laborHours", BigDecimal.ZERO);
                result.put("hourlyRate", BigDecimal.ZERO);
            }
        } else {
            result.put("trackingId", null);
            result.put("laborMinutes", 0);
            result.put("laborCost", BigDecimal.ZERO);
            result.put("laborHours", BigDecimal.ZERO);
            result.put("hourlyRate", BigDecimal.ZERO);
        }
        
        return result;
    }

    /**
     * Update inventory items for a service request
     */
    @Transactional
    public Map<String, Object> updateInventoryItems(Integer requestId, MaterialRequestDTO materialRequest, String token) {
        Integer advisorId = getAdvisorIdFromToken(token);
        ServiceRequest request = findServiceRequestWithPermissionCheck(requestId, advisorId);
        
        // Handle replacing existing items
        if (materialRequest.isReplaceExisting()) {
            List<MaterialUsage> existingUsages = materialUsageRepository
                    .findByServiceRequest_RequestIdOrderByUsedAtDesc(requestId);
            
            for (MaterialUsage usage : existingUsages) {
                InventoryItem item = usage.getInventoryItem();
                
                // Return items to inventory
                if (item != null && usage.getQuantity() != null) {
                    item.setCurrentStock(item.getCurrentStock().add(usage.getQuantity()));
                    inventoryRepository.save(item);
                }
            }
            
            materialUsageRepository.deleteAll(existingUsages);
        }
        
        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        
        // Process new inventory items
        if (materialRequest.getItems() != null && !materialRequest.getItems().isEmpty()) {
            for (MaterialItemDTO itemDTO : materialRequest.getItems()) {
                if (itemDTO == null || itemDTO.getItemId() == null) continue;
                
                // Find the inventory item
                InventoryItem item = inventoryRepository.findById(itemDTO.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Inventory item not found: " + itemDTO.getItemId()));
                
                // Validate quantity
                if (itemDTO.getQuantity() == null || itemDTO.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                
                // Check if we have enough stock
                if (item.getCurrentStock().compareTo(itemDTO.getQuantity()) < 0) {
                    throw new IllegalArgumentException(
                            "Not enough stock for item: " + item.getName() + 
                            ". Available: " + item.getCurrentStock() + 
                            ", Requested: " + itemDTO.getQuantity());
                }
                
                // Reduce inventory
                item.setCurrentStock(item.getCurrentStock().subtract(itemDTO.getQuantity()));
                inventoryRepository.save(item);
                
                // Record usage
                MaterialUsage usage = MaterialUsage.builder()
                        .inventoryItem(item)
                        .serviceRequest(request)
                        .quantity(itemDTO.getQuantity())
                        .usedAt(LocalDateTime.now())
                        .build();
                
                materialUsageRepository.save(usage);
                
                // Add to total cost
                BigDecimal itemCost = item.getUnitPrice().multiply(itemDTO.getQuantity())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                totalMaterialCost = totalMaterialCost.add(itemCost);
            }
        }
        
        // Update service tracking
        ServiceTracking tracking = getOrCreateServiceTracking(request, request.getServiceAdvisor());
        tracking.setTotalMaterialCost(totalMaterialCost);
        tracking.setUpdatedAt(LocalDateTime.now());
        serviceTrackingRepository.save(tracking);
        
        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", request.getRequestId());
        result.put("totalMaterialCost", totalMaterialCost);
        result.put("message", "Inventory items updated successfully");
        
        return result;
    }

    /**
     * Get advisor ID from JWT token
     */
    private Integer getAdvisorIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AccessDeniedException("Invalid token"));
        
        if (user.getRole() != User.Role.serviceAdvisor) {
            throw new AccessDeniedException("Only service advisors can access this resource");
        }
        
        ServiceAdvisorProfile profile = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Service advisor profile not found"));
        
        return profile.getAdvisorId();
    }

    /**
     * Find service request and check permissions
     */
    private ServiceRequest findServiceRequestWithPermissionCheck(Integer requestId, Integer advisorId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service request not found: " + requestId));
        
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisorId)) {
            throw new AccessDeniedException("You don't have permission to access this service request");
        }
        
        return request;
    }

    /**
     * Get or create service tracking record
     */
    private ServiceTracking getOrCreateServiceTracking(ServiceRequest request, ServiceAdvisorProfile advisor) {
        List<ServiceTracking> trackings = serviceTrackingRepository
                .findByServiceRequest_RequestIdOrderByUpdatedAtDesc(request.getRequestId());
        
        if (!trackings.isEmpty()) {
            return trackings.get(0);
        }
        
        // Create new tracking record
        ServiceTracking tracking = ServiceTracking.builder()
                .serviceRequest(request)
                .serviceAdvisor(advisor)
                .status(request.getStatus())
                .laborMinutes(0)
                .laborCost(BigDecimal.ZERO)
                .totalMaterialCost(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
        
        return serviceTrackingRepository.save(tracking);
    }
}