package com.acme.claims.web.dto;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

public class ClaimDecisionRequest {

    public static final String APPROVE = "APPROVE";
    public static final String REJECT = "REJECT";

    @NotBlank
    @Pattern(regexp = "APPROVE|REJECT", message = "must be APPROVE or REJECT")
    private String decision;

    /** Optional - when omitted on approval the payout is calculated from the policy. */
    private BigDecimal approvedAmount;

    private String comment;

    public ClaimDecisionRequest() {
    }

    public ClaimDecisionRequest(String decision, BigDecimal approvedAmount, String comment) {
        this.decision = decision;
        this.approvedAmount = approvedAmount;
        this.comment = comment;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
