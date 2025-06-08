package com.albany.restapi.repository;

import com.albany.restapi.model.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerProfile, Integer> {
    // Change to return a list instead of assuming unique results
    List<CustomerProfile> findByUser_UserId(Integer userId);

    // Keep the optional version for specific cases
    default Optional<CustomerProfile> findFirstByUser_UserId(Integer userId) {
        List<CustomerProfile> profiles = findByUser_UserId(userId);
        return profiles.isEmpty() ? Optional.empty() : Optional.of(profiles.get(0));
    }

    @Query("SELECT c FROM CustomerProfile c JOIN c.user u WHERE u.isActive = true ORDER BY u.firstName, u.lastName")
    List<CustomerProfile> findAllActiveCustomers();
}