package com.acme.claims.model;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClaimStatusTest {

    @Test
    public void submittedCanMoveToReviewEscalationOrRejection() {
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.UNDER_REVIEW));
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.ESCALATED));
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.REJECTED));
        assertFalse(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.APPROVED));
        assertFalse(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.PAID));
    }

    @Test
    public void onlyApprovedClaimsCanBePaid() {
        assertTrue(ClaimStatus.APPROVED.canTransitionTo(ClaimStatus.PAID));
        assertFalse(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.PAID));
        assertFalse(ClaimStatus.ESCALATED.canTransitionTo(ClaimStatus.PAID));
    }

    @Test
    public void terminalStatesAllowNoTransitions() {
        ClaimStatus[] all = ClaimStatus.values();
        for (int i = 0; i < all.length; i++) {
            assertFalse(ClaimStatus.PAID.canTransitionTo(all[i]));
            assertFalse(ClaimStatus.REJECTED.canTransitionTo(all[i]));
        }
        assertTrue(ClaimStatus.PAID.isTerminal());
        assertTrue(ClaimStatus.REJECTED.isTerminal());
        assertFalse(ClaimStatus.APPROVED.isTerminal());
    }

    @Test
    public void openStates() {
        assertTrue(ClaimStatus.SUBMITTED.isOpen());
        assertTrue(ClaimStatus.UNDER_REVIEW.isOpen());
        assertTrue(ClaimStatus.ESCALATED.isOpen());
        assertFalse(ClaimStatus.APPROVED.isOpen());
    }

    @Test
    public void changeStatusRecordsTransition() {
        Claim claim = new Claim();
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setClaimAmount(new BigDecimal("100.00"));
        claim.changeStatus(ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void changeStatusRejectsIllegalTransition() {
        Claim claim = new Claim();
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.changeStatus(ClaimStatus.PAID);
    }
}
