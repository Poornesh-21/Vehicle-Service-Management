package com.albany.restapi.repository;

import com.albany.restapi.model.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Integer> {

    @Query("SELECT cp FROM CustomerProfile cp JOIN cp.user u WHERE u.isActive = true")
    List<CustomerProfile> findAllActive();

    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.user.userId = :userId")
    CustomerProfile findByUserId(@Param("userId") Integer userId);
}