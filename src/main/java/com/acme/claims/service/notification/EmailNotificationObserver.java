package com.acme.claims.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;

/**
 * Simulates customer e-mail notifications for decisions on a claim.
 */
public class EmailNotificationObserver implements Observer {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserver.class);

    public void update(Observable source, Object arg) {
        if (!(arg instanceof ClaimEvent)) {
            return;
        }
        ClaimEvent event = (ClaimEvent) arg;
        String type = event.getType();
        if (type.equals(ClaimEvent.APPROVED) || type.equals(ClaimEvent.REJECTED) || type.equals(ClaimEvent.PAID)) {
            String subject = "Update on your claim " + event.getClaimNumber();
            String body = "Dear " + event.getCustomerName() + ",\n\n"
                    + "The status of your claim " + event.getClaimNumber() + " is now " + event.getStatus() + ".\n\n"
                    + "Kind regards,\nAcme Insurance Claims Team";
            log.info("Sending e-mail to " + event.getCustomerEmail() + " subject='" + subject + "' (" + body.length() + " chars)");
        }
    }
}
