package com.acme.claims.service.fraud;

import com.acme.claims.model.Claim;
import com.acme.claims.util.DateUtils;

import java.util.Date;

/**
 * Flags claims reported long after the incident happened.
 */
public class LateReportingRule implements FraudRule {

    private static final int LATE_THRESHOLD_DAYS = 60;

    public String getName() {
        return "LATE_REPORTING";
    }

    public int evaluate(Claim claim, FraudContext context) {
        if (claim.getIncidentDate() == null) {
            return 0;
        }
        Date filed = claim.getFiledDate() != null ? claim.getFiledDate() : context.getEvaluationDate();
        int days = DateUtils.daysBetween(claim.getIncidentDate(), filed);
        if (days > LATE_THRESHOLD_DAYS) {
            return 20;
        }
        return 0;
    }
}
