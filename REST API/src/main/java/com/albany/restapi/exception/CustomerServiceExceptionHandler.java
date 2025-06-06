package com.albany.restapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception handler for customer service-related exceptions
 */
@RestControllerAdvice(basePackages = "com.albany.restapi.controller.customer")
public class CustomerServiceExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceExceptionHandler.class);
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.error("Illegal argument exception: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle customer not found exceptions
     */
    @ExceptionHandler(CustomerExceptions.CustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCustomerNotFoundException(CustomerExceptions.CustomerNotFoundException ex, WebRequest request) {
        logger.error("Customer not found exception: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle duplicate email exceptions
     */
    @ExceptionHandler(CustomerExceptions.DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmailException(CustomerExceptions.DuplicateEmailException ex, WebRequest request) {
        logger.error("Duplicate email exception: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(CustomerExceptions.ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(CustomerExceptions.ValidationException ex, WebRequest request) {
        logger.error("Validation exception: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex, WebRequest request) {
        logger.error("Unexpected exception: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "An unexpected error occurred: " + ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}