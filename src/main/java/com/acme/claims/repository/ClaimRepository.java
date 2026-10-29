package com.acme.claims.repository;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Claim findByClaimNumber(String claimNumber);

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findByPolicyId(Long policyId);

    long countByPolicyCustomerId(Long customerId);

    @Query("select c from Claim c where c.status in :statuses and c.filedDate < :cutoff")
    List<Claim> findStaleClaims(@Param("statuses") Collection<ClaimStatus> statuses,
                                @Param("cutoff") Date cutoff);
}
