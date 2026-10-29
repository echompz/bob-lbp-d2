package com.acme.claims.service;

import com.acme.claims.model.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Calculates annual premiums.
 *
 * <pre>
 * premium = coverage x baseRate(type) x ageFactor(type, age) x claimsFactor(priorClaims)
 * </pre>
 * with a minimum premium of 100.00.
 */
@Component
public class PremiumCalculator {

    public static final BigDecimal MINIMUM_PREMIUM = new BigDecimal("100.00");

    private static final int MAX_SURCHARGED_CLAIMS = 5;

    public BigDecimal calculateAnnualPremium(PolicyType type, int age, BigDecimal coverage, int priorClaims) {
        if (type == null || coverage == null) {
            throw new IllegalArgumentException("Policy type and coverage are required");
        }
        if (age < 18 || age > 100) {
            throw new IllegalArgumentException("Applicant age must be between 18 and 100 but was " + age);
        }
        if (priorClaims < 0) {
            throw new IllegalArgumentException("Prior claims cannot be negative");
        }

        BigDecimal premium = coverage
                .multiply(baseRate(type))
                .multiply(ageFactor(type, age))
                .multiply(claimsFactor(priorClaims));

        if (premium.compareTo(MINIMUM_PREMIUM) < 0) {
            premium = MINIMUM_PREMIUM;
        }
        return premium.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal monthlyInstallment(BigDecimal annualPremium) {
        return annualPremium.divide(new BigDecimal(12), 2, BigDecimal.ROUND_HALF_UP);
    }

    BigDecimal baseRate(PolicyType type) {
        double rate;
        switch (type) {
            case AUTO:
                rate = 0.04;
                break;
            case HOME:
                rate = 0.003;
                break;
            case HEALTH:
                rate = 0.05;
                break;
            case LIFE:
                rate = 0.002;
                break;
            case TRAVEL:
                rate = 0.01;
                break;
            default:
                throw new IllegalArgumentException("Unsupported policy type " + type);
        }
        return new BigDecimal(rate);
    }

    BigDecimal ageFactor(PolicyType type, int age) {
        double factor;
        if (age < 25) {
            factor = (type == PolicyType.AUTO) ? 1.5 : 1.1;
        } else if (age < 40) {
            factor = 1.0;
        } else if (age < 60) {
            factor = 1.15;
        } else {
            factor = (type == PolicyType.LIFE || type == PolicyType.HEALTH) ? 2.0 : 1.4;
        }
        return new BigDecimal(factor);
    }

    BigDecimal claimsFactor(int priorClaims) {
        int surcharged = priorClaims > MAX_SURCHARGED_CLAIMS ? MAX_SURCHARGED_CLAIMS : priorClaims;
        return new BigDecimal(1 + surcharged * 0.10);
    }
}
