package com.albany.restapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAuthResponse {
    private String token;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
    private String membershipType;
    private boolean success;
    private String message;
    private String redirectUrl;
}