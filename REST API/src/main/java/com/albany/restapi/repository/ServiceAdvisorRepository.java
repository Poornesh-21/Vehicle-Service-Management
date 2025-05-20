package com.albany.restapi.repository;

import com.albany.restapi.model.ServiceAdvisorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceAdvisorRepository extends JpaRepository<ServiceAdvisorProfile, Integer> {
    Optional<ServiceAdvisorProfile> findByUser_UserId(Integer userId);

    @Query("SELECT sa FROM ServiceAdvisorProfile sa JOIN sa.user u WHERE u.isActive = true ORDER BY u.firstName, u.lastName")
    List<ServiceAdvisorProfile> findAllActiveAdvisors();
}