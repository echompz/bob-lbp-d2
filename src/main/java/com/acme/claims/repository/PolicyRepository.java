package com.acme.claims.repository;

import com.acme.claims.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /** Returns {@code null} when no policy matches (Spring Data 1.x style, no Optional). */
    Policy findByPolicyNumber(String policyNumber);

    List<Policy> findByCustomerId(Long customerId);

    List<Policy> findByActiveTrue();
}
