package com.albany.restapi.repository;

import com.albany.restapi.model.Invoice;
import com.albany.restapi.model.Payment;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.model.ServiceTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceTrackingRepository extends JpaRepository<ServiceTracking, Integer> {
    
    List<ServiceTracking> findByRequestId(Integer requestId);
    
    List<ServiceTracking> findByStatus(ServiceRequest.Status status);
    
    @Query("SELECT SUM(st.laborCost) FROM ServiceTracking st WHERE st.requestId = :requestId")
    Optional<BigDecimal> findTotalLaborCostByRequestId(@Param("requestId") Integer requestId);
    
    @Query("SELECT SUM(st.laborMinutes) FROM ServiceTracking st WHERE st.requestId = :requestId")
    Integer sumLaborMinutesByRequestId(@Param("requestId") Integer requestId);
    
    @Query("SELECT CAST(MAX(st.updatedAt) AS LocalDate) FROM ServiceTracking st WHERE st.requestId = :requestId AND st.status = :status")
    Optional<LocalDate> findLastUpdateDateByRequestIdAndStatus(
            @Param("requestId") Integer requestId, 
            @Param("status") ServiceRequest.Status status);
}