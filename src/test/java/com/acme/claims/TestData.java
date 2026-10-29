package com.acme.claims;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.Customer;
import com.acme.claims.model.Policy;
import com.acme.claims.model.PolicyType;
import com.acme.claims.util.DateUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * Builders for test fixtures.
 */
public final class TestData {

    private TestData() {
    }

    public static Customer customer(Long id) {
        Calendar dob = Calendar.getInstance();
        dob.add(Calendar.YEAR, -35);
        Customer customer = new Customer("Test", "Customer", "test" + id + "@example.com", "+1-555-0000",
                "1 Test Street", dob.getTime());
        customer.setId(id);
        return customer;
    }

    public static Policy policy(PolicyType type, String coverage, String deductible, int startedDaysAgo) {
        Policy policy = new Policy("POL-TEST-0001", customer(Long.valueOf(1)), type, new BigDecimal(coverage),
                new BigDecimal(deductible), new BigDecimal("500.00"),
                DateUtils.startOfDay(DateUtils.daysAgo(startedDaysAgo)),
                DateUtils.startOfDay(DateUtils.addDays(new Date(), 365)));
        policy.setId(Long.valueOf(1));
        return policy;
    }

    public static Claim claim(Policy policy, String amount, int incidentDaysAgo) {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-TEST-00001");
        claim.setPolicy(policy);
        claim.setDescription("Test claim");
        claim.setClaimAmount(new BigDecimal(amount));
        claim.setIncidentDate(DateUtils.startOfDay(DateUtils.daysAgo(incidentDaysAgo)));
        claim.setFiledDate(new Date());
        claim.setLastUpdated(new Date());
        claim.setStatus(ClaimStatus.SUBMITTED);
        return claim;
    }
}
