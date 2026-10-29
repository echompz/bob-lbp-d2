package com.acme.claims.service.fraud;

import com.acme.claims.TestData;
import com.acme.claims.model.Claim;
import com.acme.claims.model.Policy;
import com.acme.claims.model.PolicyType;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.service.FraudDetectionService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FraudDetectionServiceTest {

    private static final String ALL_RULES =
            "com.acme.claims.service.fraud.HighValueClaimRule, com.acme.claims.service.fraud.EarlyClaimRule,"
            + "com.acme.claims.service.fraud.LateReportingRule,com.acme.claims.service.fraud.FrequentClaimantRule";

    private FraudDetectionService service;
    private ClaimRepository claimRepository;

    @Before
    public void setUp() {
        claimRepository = mock(ClaimRepository.class);
        ScriptedRuleEvaluator scripted = new ScriptedRuleEvaluator();
        scripted.init();

        service = new FraudDetectionService();
        service.setRuleClassNames(ALL_RULES);
        service.setScriptedRuleEvaluator(scripted);
        service.setClaimRepository(claimRepository);
        service.init();
    }

    @Test
    public void registersAllConfiguredRules() {
        assertEquals(4, service.getRuleCount());
    }

    @Test
    public void unknownRuleClassIsSkipped() {
        FraudDetectionService other = new FraudDetectionService();
        other.setRuleClassNames("com.acme.claims.service.fraud.HighValueClaimRule,com.acme.DoesNotExist");
        other.init();
        assertEquals(1, other.getRuleCount());
    }

    @Test
    public void lowRiskClaim() {
        Policy policy = TestData.policy(PolicyType.AUTO, "25000", "500", 300);
        Claim claim = TestData.claim(policy, "1250.75", 3);

        FraudAssessment assessment = service.assess(claim, 0);

        assertEquals(0, assessment.getScore());
        assertEquals(FraudAssessment.RISK_LOW, assessment.getRiskLevel());
        assertTrue(assessment.getTriggeredRules().isEmpty());
    }

    @Test
    public void highRiskClaimCombinesJavaAndScriptedRules() {
        // policy only 10 days old, 14000 of 15000 coverage, round amount, travel
        Policy policy = TestData.policy(PolicyType.TRAVEL, "15000", "100", 10);
        Claim claim = TestData.claim(policy, "14000.00", 2);

        FraudAssessment assessment = service.assess(claim, 0);

        // EARLY_CLAIM 25 + HIGH_COVERAGE_RATIO 20 + TRAVEL_HIGH_VALUE 15 + ROUND_AMOUNT 10
        assertEquals(70, assessment.getScore());
        assertEquals(FraudAssessment.RISK_HIGH, assessment.getRiskLevel());
        assertEquals(4, assessment.getTriggeredRules().size());
        assertEquals("EARLY_CLAIM (+25)", assessment.getTriggeredRules().get(0));
    }

    @Test
    public void scoreIsCappedAt100() {
        Policy policy = TestData.policy(PolicyType.TRAVEL, "60000", "100", 10);
        Claim claim = TestData.claim(policy, "58000.00", 2);

        FraudAssessment assessment = service.assess(claim, 5);

        assertEquals(100, assessment.getScore());
    }

    @Test
    public void priorClaimsAreLookedUpForPersistedClaims() {
        Policy policy = TestData.policy(PolicyType.AUTO, "25000", "500", 300);
        Claim claim = TestData.claim(policy, "1250.75", 3);
        claim.setId(Long.valueOf(99));
        // 4 claims on record including this one -> 3 prior -> FREQUENT_CLAIMANT
        when(claimRepository.countByPolicyCustomerId(anyLong())).thenReturn(4L);

        FraudAssessment assessment = service.assess(claim);

        assertEquals(30, assessment.getScore());
        assertEquals(FraudAssessment.RISK_MEDIUM, assessment.getRiskLevel());
        assertEquals("FREQUENT_CLAIMANT (+30)", assessment.getTriggeredRules().get(0));
    }

    @Test
    public void lateReportingIsFlagged() {
        Policy policy = TestData.policy(PolicyType.HOME, "300000", "1000", 300);
        Claim claim = TestData.claim(policy, "2345.67", 90);

        FraudAssessment assessment = service.assess(claim, 0);

        assertEquals(20, assessment.getScore());
        assertEquals("LATE_REPORTING (+20)", assessment.getTriggeredRules().get(0));
    }
}
