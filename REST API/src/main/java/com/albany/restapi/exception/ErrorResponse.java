package com.albany.restapi.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data               // Provides getters, setters, toString, equals, hashCode
@Builder            // Generates builder pattern
@NoArgsConstructor  // Generates no-args constructor
@AllArgsConstructor // Generates all-args constructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;

    // Optional: If you want more control over the builder
    public static class ErrorResponseBuilder {
        // You can add custom builder methods here if needed
        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
    }
}