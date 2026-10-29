package com.acme.claims.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Checked exception raised when a claim violates a business rule.
 */
public class ClaimValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final List<String> violations;

    public ClaimValidationException(String message) {
        super(message);
        this.violations = new ArrayList<String>();
        this.violations.add(message);
    }

    public ClaimValidationException(List<String> violations) {
        super("Claim validation failed: " + violations);
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
