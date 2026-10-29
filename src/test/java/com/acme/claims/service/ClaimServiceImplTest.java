package com.acme.claims.service;

import com.acme.claims.TestData;
import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.exception.ResourceNotFoundException;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.Policy;
import com.acme.claims.model.PolicyType;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.repository.PolicyRepository;
import com.acme.claims.service.fraud.FraudAssessment;
import com.acme.claims.service.notification.ClaimEvent;
import com.acme.claims.util.DateUtils;
import com.acme.claims.web.dto.ClaimDecisionRequest;
import com.acme.claims.web.dto.ClaimRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private Policy policy;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(claimService, "escalationThreshold", 60);
        ReflectionTestUtils.setField(claimService, "maxReportingDays", 365);

        policy = TestData.policy(PolicyType.AUTO, "25000", "500", 300);
        when(policyRepository.findByPolicyNumber("POL-TEST-0001")).thenReturn(policy);
        when(claimRepository.save(any(Claim.class))).thenAnswer(new Answer<Claim>() {
            public Claim answer(InvocationOnMock invocation) throws Throwable {
                return (Claim) invocation.getArguments()[0];
            }
        });
    }

    @Test
    public void submitsValidClaim() throws Exception {
        when(fraudDetectionService.assess(any(Claim.class))).thenReturn(assessment(10));

        Claim claim = claimService.submitClaim(request("1500.00", 5));

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertEquals(new BigDecimal("1500.00"), claim.getClaimAmount());
        assertEquals(Integer.valueOf(10), claim.getFraudScore());
        assertTrue(claim.getClaimNumber().startsWith("CLM-"));
        assertEquals(1, claim.getNotes().size());

        ArgumentCaptor<ClaimEvent> event = ArgumentCaptor.forClass(ClaimEvent.class);
        verify(notificationService).publish(event.capture());
        assertEquals(ClaimEvent.SUBMITTED, event.getValue().getType());
    }

    @Test
    public void highFraudScoreAutoEscalates() throws Exception {
        when(fraudDetectionService.assess(any(Claim.class))).thenReturn(assessment(75));

        Claim claim = claimService.submitClaim(request("1500.00", 5));

        assertEquals(ClaimStatus.ESCALATED, claim.getStatus());
        assertEquals(2, claim.getNotes().size());
    }

    @Test
    public void rejectsUnknownPolicy() {
        try {
            claimService.submitClaim(new ClaimRequest("POL-NOPE", "desc", new BigDecimal("10"), "01/01/2024"));
            fail("expected ClaimValidationException");
        } catch (ClaimValidationException e) {
            assertTrue(e.getMessage().contains("Unknown policy"));
        }
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    public void collectsAllViolations() {
        try {
            // above the coverage limit and dated in the future
            claimService.submitClaim(new ClaimRequest("POL-TEST-0001", "desc", new BigDecimal("30000"),
                    DateUtils.formatDate(DateUtils.addDays(new Date(), 400))));
            fail("expected ClaimValidationException");
        } catch (ClaimValidationException e) {
            assertTrue(e.getViolations().size() >= 3);
        }
    }

    @Test(expected = ClaimValidationException.class)
    public void rejectsBadDateFormat() throws Exception {
        claimService.submitClaim(new ClaimRequest("POL-TEST-0001", "desc", new BigDecimal("10"), "2024-01-01"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getClaimThrowsWhenMissing() {
        when(claimRepository.findOne(Long.valueOf(42))).thenReturn(null);
        claimService.getClaim(Long.valueOf(42));
    }

    @Test
    public void approveDefaultsToClaimMinusDeductible() throws Exception {
        Claim claim = existingClaim(ClaimStatus.UNDER_REVIEW, "4200.00");

        Claim decided = claimService.decide(claim.getId(),
                new ClaimDecisionRequest(ClaimDecisionRequest.APPROVE, null, "Looks fine"), "manager");

        assertEquals(ClaimStatus.APPROVED, decided.getStatus());
        assertEquals(new BigDecimal("3700.00"), decided.getApprovedAmount());
    }

    @Test(expected = ClaimValidationException.class)
    public void cannotApproveMoreThanClaimed() throws Exception {
        Claim claim = existingClaim(ClaimStatus.UNDER_REVIEW, "4200.00");
        claimService.decide(claim.getId(),
                new ClaimDecisionRequest(ClaimDecisionRequest.APPROVE, new BigDecimal("5000"), null), "manager");
    }

    @Test(expected = ClaimValidationException.class)
    public void cannotDecideSubmittedClaim() throws Exception {
        Claim claim = existingClaim(ClaimStatus.SUBMITTED, "4200.00");
        claimService.decide(claim.getId(),
                new ClaimDecisionRequest(ClaimDecisionRequest.REJECT, null, null), "manager");
    }

    @Test
    public void rejectSetsZeroPayout() throws Exception {
        Claim claim = existingClaim(ClaimStatus.ESCALATED, "4200.00");

        Claim decided = claimService.decide(claim.getId(),
                new ClaimDecisionRequest(ClaimDecisionRequest.REJECT, null, "Fraud confirmed"), "manager");

        assertEquals(ClaimStatus.REJECTED, decided.getStatus());
        assertEquals(new BigDecimal("0.00"), decided.getApprovedAmount());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotPayUnapprovedClaim() {
        Claim claim = existingClaim(ClaimStatus.UNDER_REVIEW, "4200.00");
        claimService.markPaid(claim.getId(), "manager");
    }

    @Test
    public void payoutIsCappedAtCoverageAndNeverNegative() {
        Claim big = TestData.claim(policy, "24000.00", 1);
        policy.setCoverageLimit(new BigDecimal("20000"));
        assertEquals(new BigDecimal("20000.00"), claimService.calculatePayout(big));

        Claim small = TestData.claim(policy, "300.00", 1);
        assertEquals(new BigDecimal("0.00"), claimService.calculatePayout(small));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void escalatesStaleClaims() {
        Claim stale = existingClaim(ClaimStatus.SUBMITTED, "100.00");
        when(claimRepository.findStaleClaims((Collection<ClaimStatus>) anyCollection(), any(Date.class)))
                .thenReturn(Arrays.asList(stale));

        int count = claimService.escalateStaleClaims(14);

        assertEquals(1, count);
        assertEquals(ClaimStatus.ESCALATED, stale.getStatus());
        verify(claimRepository).save(anyListOf(Claim.class));
        verify(notificationService).publish(any(ClaimEvent.class));
    }

    private Claim existingClaim(ClaimStatus status, String amount) {
        Claim claim = TestData.claim(policy, amount, 10);
        claim.setId(Long.valueOf(7));
        claim.setStatus(status);
        when(claimRepository.findOne(eq(Long.valueOf(7)))).thenReturn(claim);
        return claim;
    }

    private static ClaimRequest request(String amount, int incidentDaysAgo) {
        return new ClaimRequest("POL-TEST-0001", "Bumper damaged", new BigDecimal(amount),
                DateUtils.formatDate(DateUtils.daysAgo(incidentDaysAgo)));
    }

    private static FraudAssessment assessment(int score) {
        FraudAssessment assessment = new FraudAssessment("CLM-TEST");
        if (score > 0) {
            assessment.addTriggeredRule("TEST_RULE", score);
        }
        assessment.finish();
        return assessment;
    }
}
