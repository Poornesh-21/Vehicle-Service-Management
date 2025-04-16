package com.albany.restapi.service;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ServiceAdvisorProfileRepository serviceAdvisorRepository;
    
    public List<ServiceRequestDTO> getAllServiceRequests() {
        return serviceRequestRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ServiceRequestDTO> getServiceRequestsByCustomer(Integer customerId) {
        return serviceRequestRepository.findByVehicle_Customer_User_UserId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ServiceRequestDTO> getServiceRequestsByAdvisor(Integer advisorId) {
        return serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ServiceRequestDTO> getServiceRequestsByStatus(ServiceRequest.Status status) {
        return serviceRequestRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public ServiceRequestDTO getServiceRequestById(Integer requestId) {
        return serviceRequestRepository.findById(requestId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
    }
    
    @Transactional
    public ServiceRequestDTO createServiceRequest(ServiceRequestDTO requestDTO) {
        // Get the vehicle
        Vehicle vehicle = vehicleRepository.findById(requestDTO.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        // Create service request
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setVehicle(vehicle);
        serviceRequest.setServiceType(requestDTO.getServiceType());
        serviceRequest.setDeliveryDate(requestDTO.getDeliveryDate());
        serviceRequest.setAdditionalDescription(requestDTO.getAdditionalDescription());
        serviceRequest.setStatus(ServiceRequest.Status.Received);
        
        // Save service request
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
        
        return convertToDTO(savedRequest);
    }
    
    @Transactional
    public ServiceRequestDTO assignServiceAdvisor(Integer requestId, Integer advisorId) {
        // Get the service request
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        
        // Get the service advisor
        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findById(advisorId)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));
        
        // Assign advisor
        serviceRequest.setServiceAdvisor(advisor);
        
        // Save and return
        ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
        return convertToDTO(updatedRequest);
    }
    
    @Transactional
    public ServiceRequestDTO updateServiceRequestStatus(Integer requestId, ServiceRequest.Status newStatus) {
        // Get the service request
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        
        // Update status
        serviceRequest.setStatus(newStatus);
        
        // Save and return
        ServiceRequest updatedRequest = serviceRequestRepository.save(serviceRequest);
        return convertToDTO(updatedRequest);
    }
    
    private ServiceRequestDTO convertToDTO(ServiceRequest serviceRequest) {
        ServiceRequestDTO dto = new ServiceRequestDTO();
        
        dto.setRequestId(serviceRequest.getRequestId());
        dto.setServiceType(serviceRequest.getServiceType());
        dto.setDeliveryDate(serviceRequest.getDeliveryDate());
        dto.setAdditionalDescription(serviceRequest.getAdditionalDescription());
        dto.setStatus(serviceRequest.getStatus());
        
        // Set vehicle info
        if (serviceRequest.getVehicle() != null) {
            Vehicle vehicle = serviceRequest.getVehicle();
            dto.setVehicleId(vehicle.getVehicleId());
            dto.setVehicleBrand(vehicle.getBrand());
            dto.setVehicleModel(vehicle.getModel());
            dto.setRegistrationNumber(vehicle.getRegistrationNumber());
            
            // Set customer info
            if (vehicle.getCustomer() != null && vehicle.getCustomer().getUser() != null) {
                User user = vehicle.getCustomer().getUser();
                dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                dto.setCustomerId(vehicle.getCustomer().getCustomerId());
            }
        }
        
        // Set admin info
        if (serviceRequest.getAdmin() != null) {
            dto.setAdminId(serviceRequest.getAdmin().getAdminId());
        }
        
        // Set service advisor info
        if (serviceRequest.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = serviceRequest.getServiceAdvisor();
            dto.setServiceAdvisorId(advisor.getAdvisorId());
            
            if (advisor.getUser() != null) {
                User user = advisor.getUser();
                dto.setServiceAdvisorName(user.getFirstName() + " " + user.getLastName());
            }
        }
        
        return dto;
    }
}