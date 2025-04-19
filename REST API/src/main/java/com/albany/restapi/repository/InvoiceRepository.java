package com.albany.restapi.repository;

import com.albany.restapi.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    
    Optional<Invoice> findByRequestId(Integer requestId);
    
    boolean existsByRequestId(Integer requestId);
    
    List<Invoice> findByPaymentId(Integer paymentId);
    
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.isDownloadable = true")
    long countDownloadableInvoices();
}