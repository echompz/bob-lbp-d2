package com.acme.claims.service.fraud;

import com.acme.claims.model.Claim;

/**
 * A pluggable fraud rule. Implementations are instantiated reflectively from the
 * class names configured in {@code claimtrack.fraud.rule-classes}, so every
 * implementation must have a public no-arg constructor.
 */
public interface FraudRule {

    String getName();

    /**
     * @return the score contribution (0 when the rule does not fire)
     */
    int evaluate(Claim claim, FraudContext context);
}
