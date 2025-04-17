package com.albany.restapi.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\d+", message = "Phone number should only contain digits")
    private String phoneNumber;
    
    private String street;
    private String city;
    private String state;
    private String postalCode;
    
    @NotNull(message = "Membership status is required")
    private String membershipStatus;
}