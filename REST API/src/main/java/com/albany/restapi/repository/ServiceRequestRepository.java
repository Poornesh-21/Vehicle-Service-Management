package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {

    List<ServiceRequest> findByVehicle_Customer_User_UserId(Integer userId);

    List<ServiceRequest> findByServiceAdvisor_AdvisorId(Integer advisorId);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.status = :status")
    List<ServiceRequest> findByStatus(@Param("status") ServiceRequest.Status status);

    @Query("SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.status = :status")
    long countByStatus(@Param("status") ServiceRequest.Status status);

    /**
     * Find all service requests that are not in the specified status
     */
    List<ServiceRequest> findByStatusNot(ServiceRequest.Status status);

    /**
     * Find service requests by vehicle registration number
     */
    List<ServiceRequest> findByVehicle_RegistrationNumber(String registrationNumber);

    /**
     * Find service requests by customer name (case insensitive, partial match)
     */
    @Query("SELECT sr FROM ServiceRequest sr JOIN sr.vehicle v JOIN v.customer c JOIN c.user u " +
            "WHERE CONCAT(u.firstName, ' ', u.lastName) LIKE %:customerName%")
    List<ServiceRequest> findByCustomerName(@Param("customerName") String customerName);
}