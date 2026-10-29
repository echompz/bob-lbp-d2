package com.acme.claims.report;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio-level claims report, exported as XML for the downstream finance system.
 */
@XmlRootElement(name = "claimSummaryReport")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClaimSummaryReport {

    @XmlAttribute
    private String version = "1.0";

    private String generatedAt;

    private int totalClaims;

    private BigDecimal totalClaimed;

    private BigDecimal totalApproved;

    @XmlElementWrapper(name = "statusTotals")
    @XmlElement(name = "status")
    private List<StatusTotal> statusTotals = new ArrayList<StatusTotal>();

    @XmlElementWrapper(name = "claims")
    @XmlElement(name = "claim")
    private List<ClaimReportLine> claims = new ArrayList<ClaimReportLine>();

    public String getVersion() {
        return version;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(int totalClaims) {
        this.totalClaims = totalClaims;
    }

    public BigDecimal getTotalClaimed() {
        return totalClaimed;
    }

    public void setTotalClaimed(BigDecimal totalClaimed) {
        this.totalClaimed = totalClaimed;
    }

    public BigDecimal getTotalApproved() {
        return totalApproved;
    }

    public void setTotalApproved(BigDecimal totalApproved) {
        this.totalApproved = totalApproved;
    }

    public List<StatusTotal> getStatusTotals() {
        return statusTotals;
    }

    public List<ClaimReportLine> getClaims() {
        return claims;
    }
}
