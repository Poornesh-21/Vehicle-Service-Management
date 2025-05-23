package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    public enum PaymentMethod {
        Card,
        Net_Banking,
        UPI
    }
    
    public enum Status {
        Pending,
        Completed,
        Failed
    }
    
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
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    private LocalDateTime paymentTimestamp;
    
    @PrePersist
    public void prePersist() {
        if (paymentTimestamp == null) {
            paymentTimestamp = LocalDateTime.now();
        }
    }
}