package com.albany.restapi.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    
    public enum Role {
        admin,
        serviceAdvisor,
        customer
    }
    
    public enum MembershipType {
        STANDARD,
        PREMIUM
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    
    @Column(unique = true)
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(nullable = false)
    private boolean isActive;
    private LocalDateTime createdAt;

    private String street;
    private String city;
    private String state;
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipType membershipType;
    private LocalDateTime membershipStartDate;
    private LocalDateTime membershipEndDate;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (membershipType == null) {
            membershipType = MembershipType.STANDARD;
        }
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return isActive;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return isActive;
    }
    
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}