package com.acme.claims.web.dto;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class ClaimRequest {

    @NotBlank
    private String policyNumber;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal claimAmount;

    /** Incident date in dd/MM/yyyy format. */
    @NotBlank
    @Pattern(regexp = "\\d{2}/\\d{2}/\\d{4}", message = "must be in dd/MM/yyyy format")
    private String incidentDate;

    public ClaimRequest() {
    }

    public ClaimRequest(String policyNumber, String description, BigDecimal claimAmount, String incidentDate) {
        this.policyNumber = policyNumber;
        this.description = description;
        this.claimAmount = claimAmount;
        this.incidentDate = incidentDate;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(BigDecimal claimAmount) {
        this.claimAmount = claimAmount;
    }

    public String getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(String incidentDate) {
        this.incidentDate = incidentDate;
    }
}
