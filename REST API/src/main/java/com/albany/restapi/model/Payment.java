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
@Table(name = "Payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;
    
    private Integer requestId;
    
    private Integer customerId;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private String transactionId;
    
    private LocalDateTime paymentTimestamp;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    @PrePersist
    protected void onCreate() {
        paymentTimestamp = LocalDateTime.now();
    }
    
    public enum PaymentMethod {
        UPI, Card, Net_Banking
    }
    
    public enum PaymentStatus {
        Pending, Completed, Failed
    }
}
