package com.acme.claims.service;

import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.exception.ResourceNotFoundException;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.Policy;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.repository.PolicyRepository;
import com.acme.claims.service.fraud.FraudAssessment;
import com.acme.claims.service.notification.ClaimEvent;
import com.acme.claims.util.ClaimNumberGenerator;
import com.acme.claims.util.DateUtils;
import com.acme.claims.util.MoneyUtils;
import com.acme.claims.web.dto.ClaimDecisionRequest;
import com.acme.claims.web.dto.ClaimRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ClaimServiceImpl implements ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimServiceImpl.class);

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private NotificationService notificationService;

    @Value("${claimtrack.fraud.escalation-threshold:60}")
    private int escalationThreshold;

    @Value("${claimtrack.claims.max-reporting-days:365}")
    private int maxReportingDays;

    public Claim submitClaim(ClaimRequest request) throws ClaimValidationException {
        Policy policy = policyRepository.findByPolicyNumber(request.getPolicyNumber());
        if (policy == null) {
            throw new ClaimValidationException("Unknown policy number: " + request.getPolicyNumber());
        }

        Date incidentDate;
        try {
            incidentDate = DateUtils.parseDate(request.getIncidentDate());
        } catch (ParseException e) {
            throw new ClaimValidationException("Invalid incident date '" + request.getIncidentDate()
                    + "', expected " + DateUtils.DATE_PATTERN);
        }

        List<String> errors = new ArrayList<String>();
        if (DateUtils.isInFuture(incidentDate)) {
            errors.add("Incident date cannot be in the future");
        }
        if (!policy.isInForce(incidentDate)) {
            errors.add("Policy " + policy.getPolicyNumber() + " was not in force on " + DateUtils.formatDate(incidentDate));
        }
        if (DateUtils.daysBetween(incidentDate, new Date()) > maxReportingDays) {
            errors.add("Claims must be reported within " + maxReportingDays + " days of the incident");
        }
        BigDecimal amount = request.getClaimAmount();
        if (!MoneyUtils.isPositive(amount)) {
            errors.add("Claim amount must be positive");
        } else if (amount.compareTo(policy.getCoverageLimit()) > 0) {
            errors.add("Claim amount " + MoneyUtils.format(amount) + " exceeds coverage limit of "
                    + MoneyUtils.format(policy.getCoverageLimit()));
        }
        if (StringUtils.isBlank(request.getDescription())) {
            errors.add("Description is required");
        }
        if (!errors.isEmpty()) {
            throw new ClaimValidationException(errors);
        }

        Date now = new Date();
        Claim claim = new Claim();
        claim.setClaimNumber(ClaimNumberGenerator.nextClaimNumber());
        claim.setPolicy(policy);
        claim.setDescription(StringUtils.trim(request.getDescription()));
        claim.setClaimAmount(MoneyUtils.round(amount));
        claim.setIncidentDate(incidentDate);
        claim.setFiledDate(now);
        claim.setLastUpdated(now);
        claim.setStatus(ClaimStatus.SUBMITTED);

        FraudAssessment assessment = fraudDetectionService.assess(claim);
        claim.setFraudScore(new Integer(assessment.getScore()));
        claim.addNote("system", "Claim submitted. Fraud score " + assessment.getScore()
                + " (" + assessment.getRiskLevel() + ")");

        String eventType = ClaimEvent.SUBMITTED;
        if (assessment.getScore() >= escalationThreshold) {
            claim.changeStatus(ClaimStatus.ESCALATED);
            claim.addNote("system", "Auto-escalated for fraud review: " + StringUtils.join(assessment.getTriggeredRules(), ", "));
            eventType = ClaimEvent.ESCALATED;
        }

        Claim saved = claimRepository.save(claim);
        log.info("Submitted claim " + saved.getClaimNumber() + " for policy " + policy.getPolicyNumber()
                + " status=" + saved.getStatus());
        notificationService.publish(new ClaimEvent(eventType, saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Claim getClaim(Long id) {
        Claim claim = claimRepository.findOne(id);
        if (claim == null) {
            throw new ResourceNotFoundException("Claim", id);
        }
        return claim;
    }

    @Transactional(readOnly = true)
    public List<Claim> getAllClaims() {
        return claimRepository.findAll(new Sort(Sort.Direction.DESC, "filedDate"));
    }

    @Transactional(readOnly = true)
    public List<Claim> getClaimsByStatus(ClaimStatus status) {
        return claimRepository.findByStatus(status);
    }

    public Claim startReview(Long id, String adjuster) {
        Claim claim = getClaim(id);
        claim.changeStatus(ClaimStatus.UNDER_REVIEW);
        claim.setAssignedAdjuster(adjuster);
        claim.addNote(adjuster, "Review started");
        Claim saved = claimRepository.save(claim);
        notificationService.publish(new ClaimEvent(ClaimEvent.REVIEW_STARTED, saved));
        return saved;
    }

    public Claim decide(Long id, ClaimDecisionRequest request, String decidedBy) throws ClaimValidationException {
        Claim claim = getClaim(id);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW && claim.getStatus() != ClaimStatus.ESCALATED) {
            throw new ClaimValidationException("Claim " + claim.getClaimNumber()
                    + " must be UNDER_REVIEW or ESCALATED before a decision, but is " + claim.getStatus());
        }

        String decision = request.getDecision();
        String eventType;
        if (ClaimDecisionRequest.APPROVE.equals(decision)) {
            BigDecimal payout = request.getApprovedAmount();
            if (payout == null) {
                payout = calculatePayout(claim);
            }
            if (payout.compareTo(claim.getClaimAmount()) > 0) {
                throw new ClaimValidationException("Approved amount cannot exceed the claimed amount of "
                        + MoneyUtils.format(claim.getClaimAmount()));
            }
            if (payout.signum() <= 0) {
                throw new ClaimValidationException("Approved amount must be positive - the claim may be below the deductible");
            }
            claim.setApprovedAmount(MoneyUtils.round(payout));
            claim.changeStatus(ClaimStatus.APPROVED);
            eventType = ClaimEvent.APPROVED;
        } else if (ClaimDecisionRequest.REJECT.equals(decision)) {
            claim.setApprovedAmount(MoneyUtils.ZERO);
            claim.changeStatus(ClaimStatus.REJECTED);
            eventType = ClaimEvent.REJECTED;
        } else {
            throw new ClaimValidationException("Unknown decision: " + decision);
        }

        String comment = StringUtils.isNotBlank(request.getComment()) ? request.getComment() : "(no comment)";
        claim.addNote(decidedBy, decision + ": " + comment);
        Claim saved = claimRepository.save(claim);
        notificationService.publish(new ClaimEvent(eventType, saved));
        return saved;
    }

    public Claim markPaid(Long id, String paidBy) {
        Claim claim = getClaim(id);
        claim.changeStatus(ClaimStatus.PAID);
        claim.addNote(paidBy, "Payment of " + MoneyUtils.format(claim.getApprovedAmount()) + " released");
        Claim saved = claimRepository.save(claim);
        notificationService.publish(new ClaimEvent(ClaimEvent.PAID, saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public FraudAssessment getFraudAssessment(Long id) {
        return fraudDetectionService.assess(getClaim(id));
    }

    public int escalateStaleClaims(int staleDays) {
        Date cutoff = DateUtils.daysAgo(staleDays);
        List<Claim> stale = claimRepository.findStaleClaims(
                Arrays.asList(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW), cutoff);
        for (int i = 0; i < stale.size(); i++) {
            Claim claim = stale.get(i);
            claim.changeStatus(ClaimStatus.ESCALATED);
            claim.addNote("system", "Escalated - no decision within " + staleDays + " days");
        }
        claimRepository.save(stale);
        for (Claim claim : stale) {
            notificationService.publish(new ClaimEvent(ClaimEvent.ESCALATED, claim));
        }
        return stale.size();
    }

    /**
     * Payout = claimed amount - deductible, capped at the coverage limit and never negative.
     */
    BigDecimal calculatePayout(Claim claim) {
        Policy policy = claim.getPolicy();
        BigDecimal payout = claim.getClaimAmount().subtract(policy.getDeductible());
        payout = MoneyUtils.min(payout, policy.getCoverageLimit());
        if (payout.signum() < 0) {
            payout = BigDecimal.ZERO;
        }
        return payout.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
