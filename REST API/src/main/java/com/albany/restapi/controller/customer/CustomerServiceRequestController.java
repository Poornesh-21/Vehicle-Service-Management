package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.ServiceRequestRepository;
import com.albany.restapi.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
public class CustomerServiceRequestController {
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    
    public CustomerServiceRequestController(ServiceRequestRepository serviceRequestRepository,
                                           VehicleRepository vehicleRepository,
                                           CustomerRepository customerRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }
    
    /**
     * Create a new service request
     */
    @PostMapping("/service-requests")
    public ResponseEntity<?> createServiceRequest(@RequestBody ServiceRequestDTO requestDTO,
                                                Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Find the vehicle
            Vehicle vehicle = null;
            if (requestDTO.getVehicleId() != null) {
                Optional<Vehicle> vehicleOpt = vehicleRepository.findById(requestDTO.getVehicleId());
                if (vehicleOpt.isPresent()) {
                    vehicle = vehicleOpt.get();
                    
                    // Verify that the vehicle belongs to this user
                    CustomerProfile customer = customerRepository.findByUser_UserId(user.getUserId())
                        .orElseThrow(() -> new IllegalStateException("Customer profile not found"));
                        
                    if (!vehicle.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Vehicle does not belong to this customer"
                        ));
                    }
                }
            }
            
            // Create service request
            ServiceRequest serviceRequest = ServiceRequest.builder()
                .userId(user.getUserId().longValue())
                .vehicle(vehicle)
                .vehicleType(requestDTO.getVehicleType() != null ? 
                    requestDTO.getVehicleType() : (vehicle != null ? vehicle.getCategory().name() : "Car"))
                .vehicleBrand(requestDTO.getVehicleBrand() != null ? 
                    requestDTO.getVehicleBrand() : (vehicle != null ? vehicle.getBrand() : "Unknown"))
                .vehicleModel(requestDTO.getVehicleModel() != null ? 
                    requestDTO.getVehicleModel() : (vehicle != null ? vehicle.getModel() : "Unknown"))
                .vehicleRegistration(requestDTO.getVehicleRegistration() != null ? 
                    requestDTO.getVehicleRegistration() : (vehicle != null ? vehicle.getRegistrationNumber() : "Unknown"))
                .vehicleYear(requestDTO.getVehicleYear() != null ? 
                    requestDTO.getVehicleYear() : (vehicle != null ? vehicle.getYear() : null))
                .serviceType(requestDTO.getServiceType())
                .serviceDescription("Customer requested: " + requestDTO.getServiceType())
                .additionalDescription(requestDTO.getAdditionalDescription())
                .deliveryDate(requestDTO.getDeliveryDate() != null ? 
                    requestDTO.getDeliveryDate() : LocalDate.now().plusDays(3))
                .status(ServiceRequest.Status.Received)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            // Save service request
            ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
            
            // Map to DTO and return
            ServiceRequestDTO responseDTO = mapToDTO(savedRequest);
            
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to create service request: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Map ServiceRequest entity to DTO
     */
    private ServiceRequestDTO mapToDTO(ServiceRequest request) {
        ServiceRequestDTO dto = ServiceRequestDTO.builder()
            .requestId(request.getRequestId())
            .userId(request.getUserId())
            .vehicleId(request.getVehicle() != null ? request.getVehicle().getVehicleId() : null)
            .vehicleType(request.getVehicleType())
            .vehicleBrand(request.getVehicleBrand())
            .vehicleModel(request.getVehicleModel())
            .vehicleRegistration(request.getVehicleRegistration())
            .registrationNumber(request.getVehicleRegistration()) // For compatibility
            .vehicleYear(request.getVehicleYear())
            .serviceType(request.getServiceType())
            .serviceDescription(request.getServiceDescription())
            .additionalDescription(request.getAdditionalDescription())
            .deliveryDate(request.getDeliveryDate())
            .status(request.getStatus().name())
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .build();
            
        // Add formatted dates
        if (request.getDeliveryDate() != null) {
            dto.setFormattedDeliveryDate(request.getDeliveryDate().toString());
        }
        
        if (request.getCreatedAt() != null) {
            dto.setFormattedCreatedDate(request.getCreatedAt().toString());
        }
        
        return dto;
    }
}