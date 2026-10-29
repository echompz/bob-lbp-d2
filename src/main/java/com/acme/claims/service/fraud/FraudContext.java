package com.acme.claims.service.fraud;

import java.util.Date;

/**
 * Extra information available to fraud rules that is not part of the claim itself.
 */
public class FraudContext {

    private final int priorClaimCount;
    private final Date evaluationDate;

    public FraudContext(int priorClaimCount, Date evaluationDate) {
        this.priorClaimCount = priorClaimCount;
        this.evaluationDate = evaluationDate;
    }

    public int getPriorClaimCount() {
        return priorClaimCount;
    }

    public Date getEvaluationDate() {
        return evaluationDate;
    }
}
