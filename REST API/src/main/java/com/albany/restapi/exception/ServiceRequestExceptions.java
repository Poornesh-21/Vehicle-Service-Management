package com.albany.restapi.exception;

public class ServiceRequestExceptions {

    public static class ServiceRequestNotFoundException extends RuntimeException {
        public ServiceRequestNotFoundException(Integer id) {
            super("Service request not found with id: " + id);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class ServiceAdvisorAssignmentException extends RuntimeException {
        public ServiceAdvisorAssignmentException(String message) {
            super(message);
        }
    }
}