package com.acme.claims.web;

import com.acme.claims.service.NotificationService;
import com.acme.claims.service.notification.ClaimEvent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Api(tags = "Audit", description = "Recent claim lifecycle events (MANAGER / AUDITOR)")
public class AuditController {

    @Autowired
    private NotificationService notificationService;

    @ApiOperation("Most recent claim events (newest first, max 100)")
    @RequestMapping(value = "/events", method = RequestMethod.GET)
    public List<ClaimEvent> recentEvents() {
        return notificationService.getRecentEvents();
    }
}
