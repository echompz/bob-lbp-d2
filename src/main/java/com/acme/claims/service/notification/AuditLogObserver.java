package com.acme.claims.service.notification;

import com.acme.claims.util.DateUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Writes every claim event to the audit trail (stdout) and keeps the most recent ones in memory.
 */
public class AuditLogObserver implements Observer {

    private static final int MAX_EVENTS = 100;

    private final LinkedList<ClaimEvent> recentEvents = new LinkedList<ClaimEvent>();

    public void update(Observable source, Object arg) {
        if (!(arg instanceof ClaimEvent)) {
            return;
        }
        ClaimEvent event = (ClaimEvent) arg;
        System.out.println("[AUDIT] " + DateUtils.formatDateTime(event.getOccurredAt()) + " " + event);
        synchronized (recentEvents) {
            recentEvents.addFirst(event);
            if (recentEvents.size() > MAX_EVENTS) {
                recentEvents.removeLast();
            }
        }
    }

    public List<ClaimEvent> getRecentEvents() {
        synchronized (recentEvents) {
            return new LinkedList<ClaimEvent>(recentEvents);
        }
    }
}
