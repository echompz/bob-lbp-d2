package com.acme.claims.web.dto;

import java.math.BigDecimal;

public class PremiumQuoteResponse {

    private String policyType;
    private int age;
    private BigDecimal coverageAmount;
    private int priorClaims;
    private BigDecimal annualPremium;
    private BigDecimal monthlyPremium;
    private String quotedAt;

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
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

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }

    public void setAnnualPremium(BigDecimal annualPremium) {
        this.annualPremium = annualPremium;
    }

    public BigDecimal getMonthlyPremium() {
        return monthlyPremium;
    }

    public void setMonthlyPremium(BigDecimal monthlyPremium) {
        this.monthlyPremium = monthlyPremium;
    }

    public String getQuotedAt() {
        return quotedAt;
    }

    public void setQuotedAt(String quotedAt) {
        this.quotedAt = quotedAt;
    }
}
