package com.acme.claims.web.dto;

import com.acme.claims.model.PolicyType;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PremiumQuoteRequest {

    @NotNull
    private PolicyType policyType;

    /** dd/MM/yyyy */
    @NotBlank
    private String dateOfBirth;

    @NotNull
    @DecimalMin("1000")
    private BigDecimal coverageAmount;

    @Min(0)
    private int priorClaims;

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(BigDecimal coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public int getPriorClaims() {
        return priorClaims;
    }

    public void setPriorClaims(int priorClaims) {
        this.priorClaims = priorClaims;
    }
}
