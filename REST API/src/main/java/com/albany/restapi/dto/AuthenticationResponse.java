package com.albany.restapi.dto;

import com.albany.restapi.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private Integer userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}