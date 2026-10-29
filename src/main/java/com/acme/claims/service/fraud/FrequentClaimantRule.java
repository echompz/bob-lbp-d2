package com.acme.claims.service.fraud;

import com.acme.claims.model.Claim;

public class FrequentClaimantRule implements FraudRule {

    private static final int FREQUENT_THRESHOLD = 3;

    public String getName() {
        return "FREQUENT_CLAIMANT";
    }

    public int evaluate(Claim claim, FraudContext context) {
        if (context.getPriorClaimCount() >= FREQUENT_THRESHOLD) {
            return 30;
        }
        return 0;
    }
}
