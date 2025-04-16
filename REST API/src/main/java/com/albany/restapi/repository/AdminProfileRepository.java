package com.albany.restapi.repository;

import com.albany.restapi.model.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Integer> {
    
    Optional<AdminProfile> findByUser_UserId(Integer userId);
}