package com.acme.claims.service;

import com.acme.claims.model.Claim;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.service.fraud.FraudAssessment;
import com.acme.claims.service.fraud.FraudContext;
import com.acme.claims.service.fraud.FraudRule;
import com.acme.claims.service.fraud.ScriptedRuleEvaluator;
import com.acme.claims.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Scores claims for fraud risk using a mix of compiled Java rules (loaded
 * reflectively) and business-editable scripted rules.
 */
@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ScriptedRuleEvaluator scriptedRuleEvaluator;

    @Value("${claimtrack.fraud.rule-classes}")
    private String ruleClassNames;

    private List rules = new ArrayList();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @PostConstruct
    public void init() {
        rules.clear();
        String[] classNames = StringUtils.split(ruleClassNames, ",");
        for (int i = 0; i < classNames.length; i++) {
            String className = StringUtils.trim(classNames[i]);
            try {
                Class clazz = Class.forName(className);
                FraudRule rule = (FraudRule) clazz.newInstance();
                rules.add(rule);
            } catch (ClassNotFoundException e) {
                log.error("Fraud rule class not found: " + className);
            } catch (InstantiationException e) {
                log.error("Cannot instantiate fraud rule: " + className);
            } catch (IllegalAccessException e) {
                log.error("Cannot access fraud rule: " + className);
            }
        }
        log.info("Registered " + rules.size() + " Java fraud rules");
    }

    public FraudAssessment assess(Claim claim) {
        int priorClaims = 0;
        if (claim.getPolicy() != null && claim.getPolicy().getCustomer() != null
                && claim.getPolicy().getCustomer().getId() != null) {
            priorClaims = (int) claimRepository.countByPolicyCustomerId(claim.getPolicy().getCustomer().getId());
            if (claim.getId() != null && priorClaims > 0) {
                // the claim being assessed is already persisted - don't count it as "prior"
                priorClaims--;
            }
        }
        return assess(claim, priorClaims);
    }

    public FraudAssessment assess(final Claim claim, int priorClaimCount) {
        final FraudContext context = new FraudContext(priorClaimCount, new Date());
        final FraudAssessment assessment = new FraudAssessment(claim.getClaimNumber());

        // 1. Java rules - evaluated in parallel
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
        final List<FraudRule> ruleSnapshot = new ArrayList<FraudRule>();
        for (Iterator it = rules.iterator(); it.hasNext(); ) {
            final FraudRule rule = (FraudRule) it.next();
            ruleSnapshot.add(rule);
            futures.add(executor.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return new Integer(rule.evaluate(claim, context));
                }
            }));
        }
        for (int i = 0; i < futures.size(); i++) {
            try {
                Integer points = futures.get(i).get(5, TimeUnit.SECONDS);
                if (points.intValue() > 0) {
                    assessment.addTriggeredRule(ruleSnapshot.get(i).getName(), points.intValue());
                }
            } catch (Exception e) {
                log.error("Fraud rule " + ruleSnapshot.get(i).getName() + " failed", e);
            }
        }

        // 2. Scripted rules
        if (scriptedRuleEvaluator != null) {
            List<ScriptedRuleEvaluator.ScriptedRule> fired = scriptedRuleEvaluator.evaluate(buildBindings(claim, context));
            for (ScriptedRuleEvaluator.ScriptedRule rule : fired) {
                assessment.addTriggeredRule(rule.getName(), rule.getScore());
            }
        }

        // highest impact first
        Collections.sort(assessment.getTriggeredRules(), new Comparator<String>() {
            public int compare(String a, String b) {
                return extractPoints(b) - extractPoints(a);
            }
        });

        assessment.finish();
        return assessment;
    }

    private Map<String, Object> buildBindings(Claim claim, FraudContext context) {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("claimAmount", claim.getClaimAmount() != null ? claim.getClaimAmount().doubleValue() : 0d);
        double limit = 0d;
        String policyType = "";
        int policyAgeDays = 0;
        if (claim.getPolicy() != null) {
            limit = claim.getPolicy().getCoverageLimit() != null ? claim.getPolicy().getCoverageLimit().doubleValue() : 0d;
            policyType = claim.getPolicy().getPolicyType() != null ? claim.getPolicy().getPolicyType().name() : "";
            if (claim.getPolicy().getStartDate() != null && claim.getIncidentDate() != null) {
                policyAgeDays = DateUtils.daysBetween(claim.getPolicy().getStartDate(), claim.getIncidentDate());
            }
        }
        vars.put("coverageLimit", new Double(limit));
        vars.put("policyType", policyType);
        vars.put("policyAgeDays", new Integer(policyAgeDays));
        vars.put("priorClaimCount", new Integer(context.getPriorClaimCount()));
        return vars;
    }

    private static int extractPoints(String triggered) {
        int idx = triggered.lastIndexOf("(+");
        if (idx < 0) {
            return 0;
        }
        return Integer.parseInt(triggered.substring(idx + 2, triggered.length() - 1));
    }

    // --- setters used by unit tests -------------------------------------------------

    public void setRuleClassNames(String ruleClassNames) {
        this.ruleClassNames = ruleClassNames;
    }

    public void setScriptedRuleEvaluator(ScriptedRuleEvaluator scriptedRuleEvaluator) {
        this.scriptedRuleEvaluator = scriptedRuleEvaluator;
    }

    public void setClaimRepository(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public int getRuleCount() {
        return rules.size();
    }
}
