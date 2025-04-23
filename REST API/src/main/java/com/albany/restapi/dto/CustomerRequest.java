package com.albany.restapi.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "First name must contain only alphabetic characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "Last name must contain only alphabetic characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit Indian mobile number")
    private String phoneNumber;

    @Size(max = 200, message = "Street address must be less than 200 characters")
    private String street;

    @Size(max = 100, message = "City must be less than 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s]*$", message = "City must contain only alphabetic characters")
    private String city;

    @Size(max = 100, message = "State must be less than 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s]*$", message = "State must contain only alphabetic characters")
    private String state;

    @Pattern(regexp = "^\\d{6}$", message = "Postal code must be a 6-digit number")
    private String postalCode;

    @NotNull(message = "Membership status is required")
    private String membershipStatus;
}