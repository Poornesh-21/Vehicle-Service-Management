package com.albany.restapi.service;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceAdvisorProfileRepository serviceAdvisorRepository;
    private final UserRepository userRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ServiceTrackingRepository serviceTrackingRepository;
    private final BillService billService;
    private final EmailService emailService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.07"); // 7% tax rate

    /**
     * Get all vehicles assigned to a specific service advisor
     */
    public List<VehicleInServiceDTO> getAssignedVehicles(String serviceAdvisorEmail) {
        // Find service advisor
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found"));

        // Find all service requests assigned to this advisor
        List<ServiceRequest> requests = serviceRequestRepository.findByServiceAdvisor_AdvisorId(advisor.getAdvisorId());

        // Map to DTOs
        return requests.stream()
                .map(this::mapToVehicleInServiceDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all new service requests that need to be assigned
     */
    public List<ServiceRequestDTO> getNewServiceRequests() {
        // Find unassigned service requests with status "Received"
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(ServiceRequest.Status.Received);

        // Map to DTOs
        return requests.stream()
                .map(this::mapToServiceRequestDTO)
                .collect(Collectors.toList());
    }

    /**
     * Add inventory items to a service request
     */
    @Transactional
    public ServiceBillDTO addInventoryItems(Integer requestId, List<MaterialItemDTO> items, String serviceAdvisorEmail) {
        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find service advisor
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found"));

        // Ensure the service request is assigned to this advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new RuntimeException("Service request is not assigned to this advisor");
        }

        // Process each inventory item
        for (MaterialItemDTO itemDto : items) {
            // Find inventory item
            InventoryItem inventoryItem = inventoryItemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new RuntimeException("Inventory item not found: " + itemDto.getItemId()));

            // Check if we have enough stock
            if (inventoryItem.getCurrentStock().compareTo(itemDto.getQuantity()) < 0) {
                throw new RuntimeException("Not enough stock for item: " + inventoryItem.getName());
            }

            // Create material usage record
            MaterialUsage usage = new MaterialUsage();
            usage.setServiceRequest(request);
            usage.setInventoryItem(inventoryItem);
            usage.setQuantity(itemDto.getQuantity());
            usage.setUsedAt(LocalDateTime.now());

            // Save usage
            materialUsageRepository.save(usage);

            // Reduce inventory stock
            inventoryItem.setCurrentStock(inventoryItem.getCurrentStock().subtract(itemDto.getQuantity()));
            inventoryItemRepository.save(inventoryItem);

            // Add status tracking entry
            addServiceTracking(
                    request,
                    "Added inventory item: " + inventoryItem.getName() + " x " + itemDto.getQuantity(),
                    request.getStatus(),
                    advisor
            );
        }

        // Return updated bill
        return getCurrentBill(requestId, serviceAdvisorEmail);
    }

    /**
     * Add labor charges to a service request
     */
    @Transactional
    public ServiceBillDTO addLaborCharges(Integer requestId, List<LaborChargeDTO> laborCharges, String serviceAdvisorEmail) {
        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find service advisor
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found"));

        // Ensure the service request is assigned to this advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new RuntimeException("Service request is not assigned to this advisor");
        }

        // Process each labor charge
        for (LaborChargeDTO chargeDto : laborCharges) {
            // Create service tracking record with labor details
            ServiceTracking tracking = new ServiceTracking();
            tracking.setRequestId(request.getRequestId());
            tracking.setWorkDescription(chargeDto.getDescription());
            tracking.setStatus(request.getStatus());
            tracking.setServiceAdvisor(advisor);

            // Convert hours to minutes
            int laborMinutes = (int) (chargeDto.getHours().doubleValue() * 60);
            tracking.setLaborMinutes(laborMinutes);

            // Calculate labor cost
            BigDecimal laborCost = chargeDto.getHours().multiply(chargeDto.getRatePerHour());
            tracking.setLaborCost(laborCost);

            // Save tracking
            serviceTrackingRepository.save(tracking);
        }

        // Return updated bill
        return getCurrentBill(requestId, serviceAdvisorEmail);
    }

    /**
     * Update service request status
     */
    @Transactional
    public ServiceRequestDTO updateServiceStatus(
            Integer requestId,
            ServiceRequest.Status newStatus,
            String notes,
            Boolean notifyCustomer,
            String serviceAdvisorEmail) {

        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find service advisor
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found"));

        // Ensure the service request is assigned to this advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new RuntimeException("Service request is not assigned to this advisor");
        }

        // Update status
        ServiceRequest.Status oldStatus = request.getStatus();
        request.setStatus(newStatus);
        request = serviceRequestRepository.save(request);

        // Create status update record
        String statusDescription = "Status updated from " + oldStatus + " to " + newStatus;
        if (notes != null && !notes.trim().isEmpty()) {
            statusDescription += ". Notes: " + notes;
        }

        ServiceTracking tracking = addServiceTracking(request, statusDescription, newStatus, advisor);

        // If status is now completed, update customer's last service date
        if (newStatus == ServiceRequest.Status.Completed) {
            CustomerProfile customer = request.getVehicle().getCustomer();
            customer.setLastServiceDate(LocalDateTime.now().toLocalDate());
            customer.setTotalServices(customer.getTotalServices() + 1);

            // This would require adding CustomerProfileRepository to the service
            // customerProfileRepository.save(customer);
        }

        // Notify customer if requested
        if (notifyCustomer) {
            try {
                sendStatusUpdateEmail(request, newStatus);
            } catch (Exception e) {
                log.error("Failed to send status update email: {}", e.getMessage(), e);
                // Continue processing even if email fails
            }
        }

        // Return updated request
        return mapToServiceRequestDTO(request);
    }

    /**
     * Generate bill for a service request
     */
    @Transactional
    public BillResponseDTO generateBill(Integer requestId, BillRequestDTO billRequest, String serviceAdvisorEmail) {
        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find service advisor
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found"));

        // Ensure the service request is assigned to this advisor
        if (request.getServiceAdvisor() == null || !request.getServiceAdvisor().getAdvisorId().equals(advisor.getAdvisorId())) {
            throw new RuntimeException("Service request is not assigned to this advisor");
        }

        // Generate bill
        BillResponseDTO bill = billService.generateBill(requestId, billRequest);

        // Add service tracking entry
        addServiceTracking(
                request,
                "Generated bill for service. Bill ID: " + bill.getBillId(),
                request.getStatus(),
                advisor
        );

        return bill;
    }

    /**
     * Get service request details including history and current bill
     */
    public ServiceRequestDetailDTO getServiceRequestDetails(Integer requestId, String serviceAdvisorEmail) {
        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        // Find service advisor
        User user = userRepository.findByEmail(serviceAdvisorEmail)
                .orElseThrow(() -> new RuntimeException("Service advisor not found"));

        ServiceAdvisorProfile advisor = serviceAdvisorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Service advisor profile not found"));

        // Build response
        ServiceRequestDetailDTO detailDTO = new ServiceRequestDetailDTO();

        // Request information
        detailDTO.setRequestId(request.getRequestId());
        detailDTO.setServiceType(request.getServiceType());
        detailDTO.setAdditionalDescription(request.getAdditionalDescription());
        detailDTO.setStatus(request.getStatus().name());
        detailDTO.setRequestDate(request.getCreatedAt().toLocalDate());
        detailDTO.setEstimatedCompletionDate(request.getDeliveryDate());

        // Vehicle information
        Vehicle vehicle = request.getVehicle();
        detailDTO.setVehicleId(vehicle.getVehicleId());
        detailDTO.setVehicleBrand(vehicle.getBrand());
        detailDTO.setVehicleModel(vehicle.getModel());
        detailDTO.setRegistrationNumber(vehicle.getRegistrationNumber());
        detailDTO.setVehicleType(vehicle.getCategory() != null ? vehicle.getCategory().name() : null);
        detailDTO.setVehicleYear(vehicle.getYear());

        // Customer information
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();
        detailDTO.setCustomerId(customer.getCustomerId());
        detailDTO.setCustomerName(customerUser.getFirstName() + " " + customerUser.getLastName());
        detailDTO.setCustomerEmail(customerUser.getEmail());
        detailDTO.setCustomerPhone(customerUser.getPhoneNumber());
        detailDTO.setMembershipStatus(customer.getMembershipStatus());

        // Service advisor information
        if (request.getServiceAdvisor() != null) {
            ServiceAdvisorProfile serviceAdvisor = request.getServiceAdvisor();
            User advisorUser = serviceAdvisor.getUser();
            detailDTO.setServiceAdvisorId(serviceAdvisor.getAdvisorId());
            detailDTO.setServiceAdvisorName(advisorUser.getFirstName() + " " + advisorUser.getLastName());
        }

        // Service history
        List<ServiceTracking> trackingList = serviceTrackingRepository.findByRequestId(requestId);
        List<ServiceHistoryDTO> history = trackingList.stream()
                .map(this::mapToServiceHistoryDTO)
                .collect(Collectors.toList());
        detailDTO.setServiceHistory(history);

        // Current bill
        detailDTO.setCurrentBill(getCurrentBill(requestId, serviceAdvisorEmail));

        return detailDTO;
    }

    /**
     * Get current bill for a service request
     */
    public ServiceBillDTO getCurrentBill(Integer requestId, String serviceAdvisorEmail) {
        // Find service request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();

        // Build bill
        ServiceBillDTO billDTO = new ServiceBillDTO();
        billDTO.setRequestId(requestId);
        billDTO.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());
        billDTO.setRegistrationNumber(vehicle.getRegistrationNumber());
        billDTO.setCustomerName(customerUser.getFirstName() + " " + customerUser.getLastName());
        billDTO.setCustomerEmail(customerUser.getEmail());

        // Get material usages
        List<MaterialUsage> materialUsages = materialUsageRepository.findByServiceRequest_RequestId(requestId);
        List<MaterialItemDTO> materials = materialUsages.stream()
                .map(this::mapToMaterialItemDTO)
                .collect(Collectors.toList());
        billDTO.setMaterials(materials);

        // Calculate parts subtotal
        BigDecimal partsSubtotal = materials.stream()
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        billDTO.setPartsSubtotal(partsSubtotal);

        // Get labor charges
        List<ServiceTracking> trackingWithLabor = serviceTrackingRepository.findByRequestId(requestId).stream()
                .filter(tracking -> tracking.getLaborCost() != null && tracking.getLaborCost().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        List<LaborChargeDTO> laborCharges = trackingWithLabor.stream()
                .map(this::mapToLaborChargeDTO)
                .collect(Collectors.toList());
        billDTO.setLaborCharges(laborCharges);

        // Calculate labor subtotal
        BigDecimal laborSubtotal = laborCharges.stream()
                .map(LaborChargeDTO::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        billDTO.setLaborSubtotal(laborSubtotal);

        // Calculate totals
        BigDecimal subtotal = partsSubtotal.add(laborSubtotal);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        billDTO.setSubtotal(subtotal);
        billDTO.setTax(tax);
        billDTO.setTotal(total);

        // Check if bill has been generated
        // For a real implementation, we would check if a bill exists in a bills table
        billDTO.setHasGeneratedBill(false);
        billDTO.setBillId(null);

        return billDTO;
    }

    /**
     * Add service tracking entry
     */
    private ServiceTracking addServiceTracking(
            ServiceRequest request,
            String workDescription,
            ServiceRequest.Status status,
            ServiceAdvisorProfile advisor) {

        ServiceTracking tracking = new ServiceTracking();
        tracking.setRequestId(request.getRequestId());
        tracking.setWorkDescription(workDescription);
        tracking.setStatus(status);
        tracking.setServiceAdvisor(advisor);

        return serviceTrackingRepository.save(tracking);
    }

    /**
     * Send status update email to customer
     */
    private void sendStatusUpdateEmail(ServiceRequest request, ServiceRequest.Status newStatus) {
        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();

        String subject = "Albany Service - Vehicle Service Status Update";

        String content = "Dear " + customerUser.getFirstName() + " " + customerUser.getLastName() + ",\n\n" +
                "Your vehicle service status has been updated.\n\n" +
                "Vehicle: " + vehicle.getBrand() + " " + vehicle.getModel() + " (" + vehicle.getRegistrationNumber() + ")\n" +
                "Service Type: " + request.getServiceType() + "\n" +
                "New Status: " + newStatus + "\n\n" +
                "If you have any questions, please contact our service center.\n\n" +
                "Best regards,\n" +
                "Albany Service Team";

        emailService.sendSimpleEmail(customerUser.getEmail(), subject, content);
    }

    /**
     * Map ServiceRequest to VehicleInServiceDTO
     */
    private VehicleInServiceDTO mapToVehicleInServiceDTO(ServiceRequest request) {
        Vehicle vehicle = request.getVehicle();
        CustomerProfile customer = vehicle.getCustomer();
        User customerUser = customer.getUser();

        // Get service advisor name if assigned
        String serviceAdvisorName = "Not Assigned";
        String serviceAdvisorId = "N/A";

        if (request.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = request.getServiceAdvisor();
            User advisorUser = advisor.getUser();
            serviceAdvisorName = advisorUser.getFirstName() + " " + advisorUser.getLastName();
            serviceAdvisorId = advisor.getFormattedId();
        }

        return VehicleInServiceDTO.builder()
                .requestId(request.getRequestId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel())
                .registrationNumber(vehicle.getRegistrationNumber())
                .serviceAdvisorName(serviceAdvisorName)
                .serviceAdvisorId(serviceAdvisorId)
                .status(request.getStatus().name())
                .startDate(request.getCreatedAt().toLocalDate())
                .estimatedCompletionDate(request.getDeliveryDate())
                .category(vehicle.getCategory().name())
                .customerName(customerUser.getFirstName() + " " + customerUser.getLastName())
                .customerEmail(customerUser.getEmail())
                .membershipStatus(customer.getMembershipStatus())
                .serviceType(request.getServiceType())
                .additionalDescription(request.getAdditionalDescription())
                .build();
    }

    /**
     * Map ServiceRequest to ServiceRequestDTO
     */
    private ServiceRequestDTO mapToServiceRequestDTO(ServiceRequest serviceRequest) {
        ServiceRequestDTO dto = new ServiceRequestDTO();

        dto.setRequestId(serviceRequest.getRequestId());
        dto.setServiceType(serviceRequest.getServiceType());
        dto.setDeliveryDate(serviceRequest.getDeliveryDate());
        dto.setAdditionalDescription(serviceRequest.getAdditionalDescription());
        dto.setServiceDescription(serviceRequest.getServiceDescription());

        // Ensure status is always set properly
        if (serviceRequest.getStatus() != null) {
            dto.setStatus(serviceRequest.getStatus().name());
        } else {
            dto.setStatus("Received"); // Default to Received if null
        }

        // Set vehicle details
        if (serviceRequest.getVehicle() != null) {
            Vehicle vehicle = serviceRequest.getVehicle();
            dto.setVehicleId(vehicle.getVehicleId());
            dto.setVehicleBrand(vehicle.getBrand());
            dto.setVehicleModel(vehicle.getModel());
            dto.setRegistrationNumber(vehicle.getRegistrationNumber());
            if (vehicle.getCategory() != null) {
                dto.setVehicleType(vehicle.getCategory().name());
            }

            // Set customer info
            if (vehicle.getCustomer() != null) {
                CustomerProfile customer = vehicle.getCustomer();
                dto.setCustomerId(customer.getCustomerId());
                dto.setMembershipStatus(customer.getMembershipStatus());

                if (customer.getUser() != null) {
                    User user = customer.getUser();
                    dto.setCustomerName(user.getFirstName() + " " + user.getLastName());
                    dto.setCustomerEmail(user.getEmail());
                }
            }
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

    /**
     * Map ServiceTracking to ServiceHistoryDTO
     */
    private ServiceHistoryDTO mapToServiceHistoryDTO(ServiceTracking tracking) {
        ServiceHistoryDTO dto = new ServiceHistoryDTO();
        dto.setTrackingId(tracking.getTrackingId());
        dto.setStatus(tracking.getStatus().name());
        dto.setTimestamp(tracking.getUpdatedAt());
        dto.setWorkDescription(tracking.getWorkDescription());

        // Set service advisor info if available
        if (tracking.getServiceAdvisor() != null) {
            ServiceAdvisorProfile advisor = tracking.getServiceAdvisor();
            User advisorUser = advisor.getUser();

            dto.setServiceAdvisorId(advisor.getAdvisorId());
            dto.setServiceAdvisorName(advisorUser.getFirstName() + " " + advisorUser.getLastName());
            dto.setUpdatedBy(advisorUser.getFirstName() + " " + advisorUser.getLastName());
        } else {
            dto.setUpdatedBy("System");
        }

        return dto;
    }

    /**
     * Map MaterialUsage to MaterialItemDTO
     */
    private MaterialItemDTO mapToMaterialItemDTO(MaterialUsage usage) {
        InventoryItem item = usage.getInventoryItem();

        return MaterialItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .quantity(usage.getQuantity())
                .unitPrice(item.getUnitPrice())
                .total(item.getUnitPrice().multiply(usage.getQuantity()))
                .build();
    }

    /**
     * Map ServiceTracking to LaborChargeDTO
     */
    private LaborChargeDTO mapToLaborChargeDTO(ServiceTracking tracking) {
        // Convert labor minutes to hours
        BigDecimal hours = new BigDecimal(tracking.getLaborMinutes()).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);

        // Calculate rate per hour
        BigDecimal ratePerHour = BigDecimal.ZERO;
        if (hours.compareTo(BigDecimal.ZERO) > 0) {
            ratePerHour = tracking.getLaborCost().divide(hours, 2, RoundingMode.HALF_UP);
        }

        return LaborChargeDTO.builder()
                .description(tracking.getWorkDescription())
                .hours(hours)
                .ratePerHour(ratePerHour)
                .total(tracking.getLaborCost())
                .build();
    }
}