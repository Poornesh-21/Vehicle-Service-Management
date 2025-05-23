package com.albany.restapi.dto;

import lombok.Data;

@Data
public class ServiceStatusUpdateDTO {
    private String status;
    private String notes;
    private boolean notifyCustomer;
}