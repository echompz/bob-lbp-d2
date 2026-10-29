package com.acme.claims.service.fraud;

import com.acme.claims.model.Claim;

import java.math.BigDecimal;

public class HighValueClaimRule implements FraudRule {

    private static final BigDecimal VERY_HIGH = new BigDecimal("50000");
    private static final BigDecimal HIGH = new BigDecimal("25000");

    public String getName() {
        return "HIGH_VALUE_CLAIM";
    }

    public int evaluate(Claim claim, FraudContext context) {
        BigDecimal amount = claim.getClaimAmount();
        if (amount == null) {
            return 0;
        }
        if (amount.compareTo(VERY_HIGH) > 0) {
            return 30;
        } else if (amount.compareTo(HIGH) > 0) {
            return 15;
        }
        return 0;
    }
}
