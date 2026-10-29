package com.acme.claims.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "policies")
public class Policy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal coverageLimit;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal deductible;

    @Column(precision = 15, scale = 2)
    private BigDecimal annualPremium;

    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Temporal(TemporalType.DATE)
    private Date endDate;

    private boolean active = true;

    public Policy() {
    }

    public Policy(String policyNumber, Customer customer, PolicyType policyType, BigDecimal coverageLimit,
                  BigDecimal deductible, BigDecimal annualPremium, Date startDate, Date endDate) {
        this.policyNumber = policyNumber;
        this.customer = customer;
        this.policyType = policyType;
        this.coverageLimit = coverageLimit;
        this.deductible = deductible;
        this.annualPremium = annualPremium;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
    }

    /**
     * A policy is in force when it is active and the given date falls inside the policy period.
     */
    public boolean isInForce(Date date) {
        if (!active || date == null) {
            return false;
        }
        if (startDate != null && date.before(startDate)) {
            return false;
        }
        if (endDate != null && date.after(endDate)) {
            return false;
        }
        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(BigDecimal coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getAnnualPremium() {
        return annualPremium;
    }

    public void setAnnualPremium(BigDecimal annualPremium) {
        this.annualPremium = annualPremium;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Policy other = (Policy) o;
        return policyNumber != null ? policyNumber.equals(other.policyNumber) : other.policyNumber == null;
    }

    @Override
    public int hashCode() {
        return policyNumber != null ? policyNumber.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Policy{" + policyNumber + ", " + policyType + ", limit=" + coverageLimit + "}";
    }
}
