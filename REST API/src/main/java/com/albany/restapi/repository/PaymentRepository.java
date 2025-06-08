package com.albany.restapi.repository;

import com.albany.restapi.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    // List-based method
    List<Payment> findAllByRequestId(Integer requestId);

    // Safe implementation for backward compatibility
    default Optional<Payment> findByRequestId(Integer requestId) {
        List<Payment> payments = findAllByRequestId(requestId);
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }
}