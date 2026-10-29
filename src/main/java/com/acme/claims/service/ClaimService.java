package com.acme.claims.service;

import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.service.fraud.FraudAssessment;
import com.acme.claims.web.dto.ClaimDecisionRequest;
import com.acme.claims.web.dto.ClaimRequest;

import java.util.List;

public interface ClaimService {

    Claim submitClaim(ClaimRequest request) throws ClaimValidationException;

    Claim getClaim(Long id);

    List<Claim> getAllClaims();

    List<Claim> getClaimsByStatus(ClaimStatus status);

    Claim startReview(Long id, String adjuster);

    Claim decide(Long id, ClaimDecisionRequest decision, String decidedBy) throws ClaimValidationException;

    Claim markPaid(Long id, String paidBy);

    FraudAssessment getFraudAssessment(Long id);

    /**
     * Escalates open claims that have not been decided within {@code staleDays}.
     *
     * @return number of claims escalated
     */
    int escalateStaleClaims(int staleDays);
}
