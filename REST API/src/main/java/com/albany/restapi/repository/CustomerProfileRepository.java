package com.albany.restapi.repository;

import com.albany.restapi.model.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Integer> {

    @Query("SELECT cp FROM CustomerProfile cp JOIN cp.user u WHERE u.isActive = true")
    List<CustomerProfile> findAllActive();

    // Find customer profile by user ID
    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.user.userId = :userId")
    CustomerProfile findByUserId(@Param("userId") Integer userId);

    // Find by user ID but return Optional
    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.user.userId = :userId")
    Optional<CustomerProfile> findOptionalByUserId(@Param("userId") Integer userId);

    // Find customer profile by email
    @Query("SELECT cp FROM CustomerProfile cp JOIN cp.user u WHERE u.email = :email")
    Optional<CustomerProfile> findByEmail(@Param("email") String email);

    // Find by membership status
    List<CustomerProfile> findByMembershipStatus(String membershipStatus);

    // Find by city
    List<CustomerProfile> findByCity(String city);

    // Find by state
    List<CustomerProfile> findByState(String state);

    // Find by postal code
    List<CustomerProfile> findByPostalCode(String postalCode);

    // Find customers with services greater than a count
    List<CustomerProfile> findByTotalServicesGreaterThan(Integer count);
}