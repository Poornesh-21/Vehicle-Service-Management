package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileUpdateDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber; // Added phone number field
    private String street;
    private String city;
    private String state;
    private String postalCode;
}