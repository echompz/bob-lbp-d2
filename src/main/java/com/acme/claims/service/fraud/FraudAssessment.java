package com.acme.claims.service.fraud;

import java.util.List;
import java.util.Vector;

/**
 * Result of running all fraud rules against a claim.
 */
public class FraudAssessment {

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    private String claimNumber;
    private int score;
    private String riskLevel;
    private List<String> triggeredRules = new Vector<String>();

    public FraudAssessment() {
    }

    public FraudAssessment(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public void addTriggeredRule(String ruleName, int points) {
        triggeredRules.add(ruleName + " (+" + points + ")");
        score += points;
    }

    public void finish() {
        if (score > 100) {
            score = 100;
        }
        if (score >= 60) {
            riskLevel = RISK_HIGH;
        } else if (score >= 30) {
            riskLevel = RISK_MEDIUM;
        } else {
            riskLevel = RISK_LOW;
        }
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(List<String> triggeredRules) {
        this.triggeredRules = triggeredRules;
    }
}
