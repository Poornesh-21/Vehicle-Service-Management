package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Invoices")
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer invoiceId;
    
    private Integer requestId;
    
    private Integer paymentId;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal taxes;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal netAmount;
    
    private LocalDateTime invoiceDate;
    
    private boolean isDownloadable;
    
    @PrePersist
    protected void onCreate() {
        invoiceDate = LocalDateTime.now();
    }
    
    public void setDownloadable(boolean downloadable) {
        this.isDownloadable = downloadable;
    }
}