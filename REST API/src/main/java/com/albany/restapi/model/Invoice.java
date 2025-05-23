package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer invoiceId;
    
    private Integer requestId;
    private Integer paymentId;
    
    private LocalDateTime invoiceDate;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal netAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal taxes;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    private boolean isDownloadable;
}