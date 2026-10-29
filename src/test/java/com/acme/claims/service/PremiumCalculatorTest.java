package com.acme.claims.service;

import com.acme.claims.model.PolicyType;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class PremiumCalculatorTest {

    private PremiumCalculator calculator;

    @Before
    public void setUp() {
        calculator = new PremiumCalculator();
    }

    @Test
    public void standardAutoPremium() {
        assertEquals(new BigDecimal("800.00"),
                calculator.calculateAnnualPremium(PolicyType.AUTO, 30, new BigDecimal("20000"), 0));
    }

    @Test
    public void youngDriverWithPriorClaimsPaysMore() {
        // 20000 * 0.04 * 1.5 (age < 25) * 1.2 (2 prior claims)
        assertEquals(new BigDecimal("1440.00"),
                calculator.calculateAnnualPremium(PolicyType.AUTO, 22, new BigDecimal("20000"), 2));
    }

    @Test
    public void seniorLifePremium() {
        // 100000 * 0.002 * 2.0 (age >= 60, LIFE)
        assertEquals(new BigDecimal("400.00"),
                calculator.calculateAnnualPremium(PolicyType.LIFE, 65, new BigDecimal("100000"), 0));
    }

    @Test
    public void middleAgedHomePremium() {
        // 300000 * 0.003 * 1.15
        assertEquals(new BigDecimal("1035.00"),
                calculator.calculateAnnualPremium(PolicyType.HOME, 45, new BigDecimal("300000"), 0));
    }

    @Test
    public void minimumPremiumApplies() {
        assertEquals(new BigDecimal("100.00"),
                calculator.calculateAnnualPremium(PolicyType.TRAVEL, 30, new BigDecimal("5000"), 0));
    }

    @Test
    public void priorClaimsLoadingIsCapped() {
        BigDecimal five = calculator.calculateAnnualPremium(PolicyType.AUTO, 30, new BigDecimal("20000"), 5);
        BigDecimal ten = calculator.calculateAnnualPremium(PolicyType.AUTO, 30, new BigDecimal("20000"), 10);
        assertEquals(five, ten);
        assertEquals(new BigDecimal("1200.00"), five);
    }

    @Test
    public void monthlyInstallment() {
        assertEquals(new BigDecimal("66.67"), calculator.monthlyInstallment(new BigDecimal("800.00")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnderageApplicant() {
        calculator.calculateAnnualPremium(PolicyType.AUTO, 17, new BigDecimal("20000"), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingPolicyType() {
        calculator.calculateAnnualPremium(null, 30, new BigDecimal("20000"), 0);
    }
}
