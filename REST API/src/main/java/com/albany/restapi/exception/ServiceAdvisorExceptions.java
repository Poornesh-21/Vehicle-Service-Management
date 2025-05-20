package com.albany.restapi.exception;

public class ServiceAdvisorExceptions {

    public static class ServiceAdvisorNotFoundException extends RuntimeException {
        public ServiceAdvisorNotFoundException(Integer id) {
            super("Service advisor not found with id: " + id);
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