package com.acme.claims.scheduler;

import com.acme.claims.service.ClaimService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Periodically escalates claims that have been open for too long.
 */
@Component
public class ClaimEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClaimEscalationScheduler.class);

    @Autowired
    private ClaimService claimService;

    @Value("${claimtrack.escalation.enabled:true}")
    private boolean enabled;

    @Value("${claimtrack.escalation.interval-ms:300000}")
    private long intervalMs;

    @Value("${claimtrack.escalation.stale-days:14}")
    private int staleDays;

    private Timer timer;

    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("Claim escalation job is disabled");
            return;
        }
        timer = new Timer("claim-escalation", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    int escalated = claimService.escalateStaleClaims(staleDays);
                    if (escalated > 0) {
                        log.info("Escalation job escalated " + escalated + " stale claim(s)");
                    }
                } catch (Exception e) {
                    log.error("Escalation job failed", e);
                }
            }
        }, intervalMs, intervalMs);
        log.info("Claim escalation job scheduled every " + intervalMs + " ms (stale after " + staleDays + " days)");
    }

    @PreDestroy
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
