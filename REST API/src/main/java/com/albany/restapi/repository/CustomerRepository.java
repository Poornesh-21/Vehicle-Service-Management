package com.albany.restapi.repository;

import com.albany.restapi.model.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerProfile, Integer> {
    Optional<CustomerProfile> findByUser_UserId(Integer userId);

    // Added method to safely handle multiple results
    List<CustomerProfile> findAllByUser_UserId(Integer userId);

    @Query("SELECT c FROM CustomerProfile c JOIN c.user u WHERE u.isActive = true ORDER BY u.firstName, u.lastName")
    List<CustomerProfile> findAllActiveCustomers();
}