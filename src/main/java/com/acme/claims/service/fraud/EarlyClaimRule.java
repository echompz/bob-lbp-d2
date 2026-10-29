package com.acme.claims.service.fraud;

import com.acme.claims.model.Claim;
import com.acme.claims.util.DateUtils;

/**
 * Flags incidents that happen very shortly after the policy started.
 */
public class EarlyClaimRule implements FraudRule {

    private static final int EARLY_WINDOW_DAYS = 30;

    public String getName() {
        return "EARLY_CLAIM";
    }

    public int evaluate(Claim claim, FraudContext context) {
        if (claim.getPolicy() == null || claim.getPolicy().getStartDate() == null || claim.getIncidentDate() == null) {
            return 0;
        }
        int days = DateUtils.daysBetween(claim.getPolicy().getStartDate(), claim.getIncidentDate());
        if (days >= 0 && days <= EARLY_WINDOW_DAYS) {
            return 25;
        }
        return 0;
    }
}
