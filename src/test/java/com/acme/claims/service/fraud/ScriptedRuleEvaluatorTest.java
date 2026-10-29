package com.acme.claims.service.fraud;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScriptedRuleEvaluatorTest {

    private ScriptedRuleEvaluator evaluator;

    @Before
    public void setUp() {
        evaluator = new ScriptedRuleEvaluator();
        evaluator.init();
    }

    @Test
    public void loadsRulesFromClasspath() {
        assertEquals(3, evaluator.getRules().size());
        // sorted by name
        assertEquals("HIGH_COVERAGE_RATIO", evaluator.getRules().get(0).getName());
    }

    @Test
    public void ordinaryClaimTriggersNothing() {
        assertTrue(evaluator.evaluate(vars(1250.75, 25000, "AUTO")).isEmpty());
    }

    @Test
    public void highValueRoundTravelClaimTriggersAllThree() {
        List<String> names = names(evaluator.evaluate(vars(14000, 15000, "TRAVEL")));
        assertEquals(3, names.size());
        assertTrue(names.contains("HIGH_COVERAGE_RATIO"));
        assertTrue(names.contains("ROUND_AMOUNT"));
        assertTrue(names.contains("TRAVEL_HIGH_VALUE"));
    }

    @Test
    public void roundAmountOnly() {
        List<String> names = names(evaluator.evaluate(vars(6000, 100000, "HOME")));
        assertEquals(1, names.size());
        assertEquals("ROUND_AMOUNT", names.get(0));
    }

    private static Map<String, Object> vars(double amount, double limit, String type) {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("claimAmount", new Double(amount));
        vars.put("coverageLimit", new Double(limit));
        vars.put("policyType", type);
        vars.put("policyAgeDays", new Integer(200));
        vars.put("priorClaimCount", new Integer(0));
        return vars;
    }

    private static List<String> names(List<ScriptedRuleEvaluator.ScriptedRule> rules) {
        List<String> names = new ArrayList<String>();
        for (ScriptedRuleEvaluator.ScriptedRule rule : rules) {
            names.add(rule.getName());
        }
        return names;
    }
}
