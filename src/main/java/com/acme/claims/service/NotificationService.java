package com.acme.claims.service;

import com.acme.claims.service.notification.AuditLogObserver;
import com.acme.claims.service.notification.ClaimEvent;
import com.acme.claims.service.notification.EmailNotificationObserver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Observable;

/**
 * Publishes claim lifecycle events to registered observers.
 */
@Service
public class NotificationService extends Observable {

    private final AuditLogObserver auditLog = new AuditLogObserver();

    @PostConstruct
    public void registerDefaultObservers() {
        addObserver(auditLog);
        addObserver(new EmailNotificationObserver());
    }

    public void publish(ClaimEvent event) {
        setChanged();
        notifyObservers(event);
    }

    public List<ClaimEvent> getRecentEvents() {
        return auditLog.getRecentEvents();
    }
}
