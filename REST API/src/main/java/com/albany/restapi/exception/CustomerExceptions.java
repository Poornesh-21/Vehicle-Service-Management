package com.albany.restapi.exception;

public class CustomerExceptions {

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(Integer id) {
            super("Customer not found with id: " + id);
        }
    }
    
    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email) {
            super("Email already exists: " + email);
        }
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}