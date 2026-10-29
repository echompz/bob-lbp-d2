package com.acme.claims.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "claims")
public class Claim implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String claimNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Column(length = 2000)
    private String description;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal claimAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    private Integer fraudScore;

    private String assignedAdjuster;

    @Temporal(TemporalType.DATE)
    private Date incidentDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date filedDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<ClaimNote> notes = new ArrayList<ClaimNote>();

    public Claim() {
    }

    public void addNote(String author, String noteText) {
        ClaimNote note = new ClaimNote(this, author, noteText);
        notes.add(note);
    }

    public void changeStatus(ClaimStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Illegal status transition " + status + " -> " + newStatus
                    + " for claim " + claimNumber);
        }
        this.status = newStatus;
        this.lastUpdated = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
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

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public Integer getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Integer fraudScore) {
        this.fraudScore = fraudScore;
    }

    public String getAssignedAdjuster() {
        return assignedAdjuster;
    }

    public void setAssignedAdjuster(String assignedAdjuster) {
        this.assignedAdjuster = assignedAdjuster;
    }

    public Date getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(Date incidentDate) {
        this.incidentDate = incidentDate;
    }

    public Date getFiledDate() {
        return filedDate;
    }

    public void setFiledDate(Date filedDate) {
        this.filedDate = filedDate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<ClaimNote> getNotes() {
        return notes;
    }

    public void setNotes(List<ClaimNote> notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Claim other = (Claim) o;
        return claimNumber != null ? claimNumber.equals(other.claimNumber) : other.claimNumber == null;
    }

    @Override
    public int hashCode() {
        return claimNumber != null ? claimNumber.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Claim{" + claimNumber + ", status=" + status + ", amount=" + claimAmount + "}";
    }
}
