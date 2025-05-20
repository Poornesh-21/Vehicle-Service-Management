package com.albany.mvc.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
}