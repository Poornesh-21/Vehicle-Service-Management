package com.albany.restapi.repository;

import com.albany.restapi.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    
    Optional<Payment> findByRequestId(Integer requestId);
    
    boolean existsByRequestId(Integer requestId);
    
    List<Payment> findByCustomerId(Integer customerId);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'Completed'")
    Optional<BigDecimal> findTotalCompletedPayments();
}