package com.acme.claims.service.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Evaluates business-editable fraud rules written as JavaScript expressions
 * (see {@code fraud-rules.properties}) using the JDK's built-in Nashorn engine.
 */
@Component
public class ScriptedRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ScriptedRuleEvaluator.class);

    private static final String RULES_FILE = "fraud-rules.properties";

    private ScriptEngine engine;

    private List<ScriptedRule> rules = new ArrayList<ScriptedRule>();

    @PostConstruct
    public void init() {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("nashorn");
        if (engine == null) {
            throw new IllegalStateException("JavaScript engine 'nashorn' is not available on this JVM");
        }
        rules = loadRules();
        log.info("Loaded " + rules.size() + " scripted fraud rules using " + engine.getFactory().getEngineName());
    }

    private List<ScriptedRule> loadRules() {
        List<ScriptedRule> loaded = new ArrayList<ScriptedRule>();
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(RULES_FILE);
            if (in == null) {
                log.warn("No " + RULES_FILE + " found on classpath - scripted rules disabled");
                return loaded;
            }
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            return loaded;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        List<String> names = new ArrayList<String>();
        Enumeration keys = props.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.startsWith("rule.") && key.endsWith(".expression")) {
                names.add(key.substring("rule.".length(), key.length() - ".expression".length()));
            }
        }
        Collections.sort(names);

        for (Iterator<String> it = names.iterator(); it.hasNext(); ) {
            String name = it.next();
            String expression = props.getProperty("rule." + name + ".expression");
            int score = Integer.parseInt(props.getProperty("rule." + name + ".score", "10"));
            loaded.add(new ScriptedRule(name, expression, score));
        }
        return loaded;
    }

    /**
     * Evaluates every scripted rule against the supplied variables.
     *
     * @return the rules that fired
     */
    public synchronized List<ScriptedRule> evaluate(Map<String, Object> variables) {
        List<ScriptedRule> fired = new ArrayList<ScriptedRule>();
        for (int i = 0; i < rules.size(); i++) {
            ScriptedRule rule = rules.get(i);
            Bindings bindings = engine.createBindings();
            bindings.putAll(variables);
            try {
                Object result = engine.eval(rule.getExpression(), bindings);
                if (result instanceof Boolean && ((Boolean) result).booleanValue()) {
                    fired.add(rule);
                }
            } catch (ScriptException e) {
                log.error("Scripted rule " + rule.getName() + " failed: " + e.getMessage());
            }
        }
        return fired;
    }

    public List<ScriptedRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public static class ScriptedRule {

        private final String name;
        private final String expression;
        private final int score;

        public ScriptedRule(String name, String expression, int score) {
            this.name = name;
            this.expression = expression;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public String getExpression() {
            return expression;
        }

        public int getScore() {
            return score;
        }
    }
}
