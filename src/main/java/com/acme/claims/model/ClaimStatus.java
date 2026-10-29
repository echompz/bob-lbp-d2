package com.acme.claims.model;

/**
 * Lifecycle states of an insurance claim.
 *
 * <pre>
 *   SUBMITTED ----> UNDER_REVIEW ----> APPROVED ----> PAID
 *       |               |   |              ^
 *       |               |   +--> ESCALATED-+
 *       |               |            |
 *       +---------------+------------+----> REJECTED
 * </pre>
 */
public enum ClaimStatus {

    SUBMITTED,
    UNDER_REVIEW,
    ESCALATED,
    APPROVED,
    REJECTED,
    PAID;

    public boolean canTransitionTo(ClaimStatus target) {
        if (target == null) {
            return false;
        }
        switch (this) {
            case SUBMITTED:
                return target == UNDER_REVIEW || target == ESCALATED || target == REJECTED;
            case UNDER_REVIEW:
                return target == APPROVED || target == REJECTED || target == ESCALATED;
            case ESCALATED:
                return target == UNDER_REVIEW || target == APPROVED || target == REJECTED;
            case APPROVED:
                return target == PAID;
            case REJECTED:
            case PAID:
                return false;
            default:
                return false;
        }
    }

    public boolean isTerminal() {
        return this == REJECTED || this == PAID;
    }

    public boolean isOpen() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == ESCALATED;
    }
}
