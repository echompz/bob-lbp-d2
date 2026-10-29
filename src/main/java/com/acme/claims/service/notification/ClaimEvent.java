package com.acme.claims.service.notification;

import com.acme.claims.model.Claim;

import java.util.Date;

public class ClaimEvent {

    public static final String SUBMITTED = "SUBMITTED";
    public static final String REVIEW_STARTED = "REVIEW_STARTED";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String PAID = "PAID";
    public static final String ESCALATED = "ESCALATED";
    public static final String DOCUMENT_ADDED = "DOCUMENT_ADDED";

    private final String type;
    private final String claimNumber;
    private final String status;
    private final String customerEmail;
    private final String customerName;
    private final Date occurredAt;

    public ClaimEvent(String type, Claim claim) {
        this.type = type;
        this.claimNumber = claim.getClaimNumber();
        this.status = claim.getStatus() != null ? claim.getStatus().name() : null;
        if (claim.getPolicy() != null && claim.getPolicy().getCustomer() != null) {
            this.customerEmail = claim.getPolicy().getCustomer().getEmail();
            this.customerName = claim.getPolicy().getCustomer().getFullName();
        } else {
            this.customerEmail = null;
            this.customerName = null;
        }
        this.occurredAt = new Date();
    }

    public String getType() {
        return type;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "ClaimEvent[" + type + " " + claimNumber + " status=" + status + "]";
    }
}
