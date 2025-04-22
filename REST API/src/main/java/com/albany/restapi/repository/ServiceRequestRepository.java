package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {

    // Find by customer ID (through vehicle)
    List<ServiceRequest> findByVehicle_Customer_CustomerId(Integer customerId);

    // Find by user ID (through vehicle's customer's user)
    @Query("SELECT sr FROM ServiceRequest sr JOIN sr.vehicle v JOIN v.customer c JOIN c.user u WHERE u.userId = :userId")
    List<ServiceRequest> findByVehicle_Customer_User_UserId(@Param("userId") Integer userId);

    // Find by service advisor ID
    List<ServiceRequest> findByServiceAdvisor_AdvisorId(Integer advisorId);

    // Find by status
    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.status = :status")
    List<ServiceRequest> findByStatus(@Param("status") ServiceRequest.Status status);

    // Count by status
    @Query("SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.status = :status")
    long countByStatus(@Param("status") ServiceRequest.Status status);

    // Find all service requests that are not in the specified status
    List<ServiceRequest> findByStatusNot(ServiceRequest.Status status);

    // Find service requests by vehicle registration number
    List<ServiceRequest> findByVehicle_RegistrationNumber(String registrationNumber);

    // Find service requests by customer name (case insensitive, partial match)
    @Query("SELECT sr FROM ServiceRequest sr JOIN sr.vehicle v JOIN v.customer c JOIN c.user u " +
            "WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :customerName, '%'))")
    List<ServiceRequest> findByCustomerName(@Param("customerName") String customerName);
}