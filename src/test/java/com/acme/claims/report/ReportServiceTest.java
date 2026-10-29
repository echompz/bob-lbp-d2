package com.acme.claims.report;

import com.acme.claims.TestData;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.Policy;
import com.acme.claims.model.PolicyType;
import com.acme.claims.service.ReportService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReportServiceTest {

    private ReportService reportService;
    private List<Claim> claims;

    @Before
    public void setUp() {
        reportService = new ReportService();
        Policy policy = TestData.policy(PolicyType.HOME, "300000", "1000", 300);

        claims = new ArrayList<Claim>();
        Claim paid = TestData.claim(policy, "9800.00", 30);
        paid.setClaimNumber("CLM-B");
        paid.setStatus(ClaimStatus.PAID);
        paid.setApprovedAmount(new BigDecimal("8800.00"));
        paid.setFraudScore(Integer.valueOf(10));
        claims.add(paid);

        Claim open = TestData.claim(policy, "200.50", 2);
        open.setClaimNumber("CLM-A");
        claims.add(open);
    }

    @Test
    public void buildsTotals() {
        ClaimSummaryReport report = reportService.buildClaimSummary(claims);

        assertEquals(2, report.getTotalClaims());
        assertEquals(new BigDecimal("10000.50"), report.getTotalClaimed());
        assertEquals(new BigDecimal("8800.00"), report.getTotalApproved());
        assertEquals(ClaimStatus.values().length, report.getStatusTotals().size());
        assertEquals("CLM-A", report.getClaims().get(0).getClaimNumber());
    }

    @Test
    public void marshalsToXml() {
        String xml = reportService.toXml(reportService.buildClaimSummary(claims));

        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("<claimSummaryReport version=\"1.0\">"));
        assertTrue(xml.contains("claimNumber=\"CLM-B\""));
        assertTrue(xml.contains("<status name=\"PAID\" count=\"1\" amount=\"9800.00\"/>"));
    }

    @Test
    public void rendersCsv() {
        String csv = reportService.toCsv(reportService.buildClaimSummary(claims));
        String[] lines = csv.split("\n");

        assertEquals(3, lines.length);
        assertTrue(lines[0].startsWith("claimNumber,policyNumber"));
        assertTrue(lines[2].startsWith("CLM-B,POL-TEST-0001,HOME,\"Test Customer\",PAID,9800.00,8800.00,10,"));
    }
}
