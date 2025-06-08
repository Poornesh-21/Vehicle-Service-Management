package com.albany.restapi.repository;

import com.albany.restapi.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    // Method to find all invoices for a request ID
    List<Invoice> findAllByRequestId(Integer requestId);

    // Safe implementation of the original method for backward compatibility
    default Optional<Invoice> findByRequestId(Integer requestId) {
        List<Invoice> invoices = findAllByRequestId(requestId);
        return invoices.isEmpty() ? Optional.empty() : Optional.of(invoices.get(0));
    }
}