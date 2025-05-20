package com.albany.restapi.exception;

public class InventoryExceptions {

    public static class InventoryItemNotFoundException extends RuntimeException {
        public InventoryItemNotFoundException(Integer id) {
            super("Inventory item not found with id: " + id);
        }
    }
    
    public static class DuplicateItemNameException extends RuntimeException {
        public DuplicateItemNameException(String name) {
            super("Inventory item with name already exists: " + name);
        }
    }
    
    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String itemName, Integer itemId, Double requested, Double available) {
            super(String.format("Insufficient stock for item '%s' (ID: %d). Requested: %.2f, Available: %.2f",
                    itemName, itemId, requested, available));
        }
    }
    
    public static class ItemInUseException extends RuntimeException {
        public ItemInUseException(Integer id) {
            super("Cannot delete inventory item with id: " + id + " because it is referenced by service requests");
        }
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}