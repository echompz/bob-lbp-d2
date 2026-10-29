package com.acme.claims.config;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.Customer;
import com.acme.claims.model.Policy;
import com.acme.claims.model.PolicyType;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.repository.CustomerRepository;
import com.acme.claims.repository.PolicyRepository;
import com.acme.claims.service.PremiumCalculator;
import com.acme.claims.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Seeds the in-memory database with demo customers, policies and claims.
 * Dates are relative to "today" so the demo data never expires.
 */
@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner seedDatabase(final CustomerRepository customers,
                                          final PolicyRepository policies,
                                          final ClaimRepository claims,
                                          final PremiumCalculator premiumCalculator) {
        return new CommandLineRunner() {
            public void run(String... args) throws Exception {
                if (customers.count() > 0) {
                    return;
                }

                Customer maria = new Customer("Maria", "Gonzalez", "maria.gonzalez@example.com", "+1-555-0101",
                        "12 Elm Street, Springfield", DateUtils.parseDate("12/04/1985"));
                Customer james = new Customer("James", "Okafor", "james.okafor@example.com", "+1-555-0102",
                        "88 Harbour Road, Bayside", DateUtils.parseDate("03/09/1972"));
                Customer priya = new Customer("Priya", "Raman", "priya.raman@example.com", "+1-555-0103",
                        "5 Lotus Lane, Riverton", DateUtils.parseDate("21/11/1999"));
                Customer liam = new Customer("Liam", "Chen", "liam.chen@example.com", "+1-555-0104",
                        "41 Cedar Avenue, Lakeside", DateUtils.parseDate("07/02/1958"));
                customers.save(Arrays.asList(maria, james, priya, liam));

                Date start = DateUtils.startOfDay(DateUtils.daysAgo(365));
                Date end = DateUtils.startOfDay(DateUtils.addDays(new Date(), 365));

                Policy mariaAuto = policy("POL-AUTO-1001", maria, PolicyType.AUTO, "25000", "500", start, end, premiumCalculator);
                Policy mariaHome = policy("POL-HOME-2001", maria, PolicyType.HOME, "350000", "1000", start, end, premiumCalculator);
                Policy jamesAuto = policy("POL-AUTO-1002", james, PolicyType.AUTO, "40000", "750", start, end, premiumCalculator);
                Policy priyaHealth = policy("POL-HLTH-3001", priya, PolicyType.HEALTH, "100000", "250", start, end, premiumCalculator);
                Policy priyaTravel = policy("POL-TRVL-4001", priya, PolicyType.TRAVEL, "15000", "100",
                        DateUtils.startOfDay(DateUtils.daysAgo(20)), DateUtils.startOfDay(DateUtils.addDays(new Date(), 160)),
                        premiumCalculator);
                Policy liamLife = policy("POL-LIFE-5001", liam, PolicyType.LIFE, "500000", "0", start, end, premiumCalculator);
                Policy liamHome = policy("POL-HOME-2002", liam, PolicyType.HOME, "280000", "1500", start, end, premiumCalculator);
                liamHome.setActive(false); // lapsed - used to demo validation errors
                policies.save(Arrays.asList(mariaAuto, mariaHome, jamesAuto, priyaHealth, priyaTravel, liamLife, liamHome));

                List<Claim> seed = new ArrayList<Claim>();
                seed.add(claim("CLM-SEED-00001", mariaAuto, "Rear-ended at traffic lights, bumper and tail lights damaged",
                        "4200.00", null, ClaimStatus.SUBMITTED, 3, 2, 0, null));
                seed.add(claim("CLM-SEED-00002", mariaHome, "Water damage to kitchen from burst pipe",
                        "18750.00", null, ClaimStatus.UNDER_REVIEW, 30, 25, 10, "adjuster"));
                seed.add(claim("CLM-SEED-00003", jamesAuto, "Vehicle stolen from driveway overnight",
                        "38000.00", null, ClaimStatus.ESCALATED, 40, 20, 65, "adjuster"));
                seed.add(claim("CLM-SEED-00004", priyaHealth, "Emergency appendectomy and two-night hospital stay",
                        "12400.50", "12150.50", ClaimStatus.APPROVED, 60, 58, 0, "adjuster"));
                seed.add(claim("CLM-SEED-00005", mariaHome, "Storm damage to roof tiles and gutters",
                        "9800.00", "8800.00", ClaimStatus.PAID, 200, 195, 10, "adjuster"));
                seed.add(claim("CLM-SEED-00006", mariaAuto, "Windscreen chip from road debris",
                        "350.00", "0.00", ClaimStatus.REJECTED, 90, 88, 0, "adjuster"));
                seed.add(claim("CLM-SEED-00007", priyaHealth, "Physiotherapy sessions after sports injury",
                        "2400.00", null, ClaimStatus.SUBMITTED, 25, 21, 0, null));
                seed.add(claim("CLM-SEED-00008", jamesAuto, "Hail damage to bonnet and roof",
                        "5600.00", null, ClaimStatus.UNDER_REVIEW, 10, 9, 10, "adjuster"));
                claims.save(seed);

                log.info("Seeded " + customers.count() + " customers, " + policies.count() + " policies, "
                        + claims.count() + " claims");
            }
        };
    }

    private static Policy policy(String number, Customer customer, PolicyType type, String coverage, String deductible,
                                 Date start, Date end, PremiumCalculator calculator) {
        BigDecimal coverageAmount = new BigDecimal(coverage);
        BigDecimal premium = calculator.calculateAnnualPremium(type, DateUtils.calculateAge(customer.getDateOfBirth()),
                coverageAmount, 0);
        return new Policy(number, customer, type, coverageAmount, new BigDecimal(deductible), premium, start, end);
    }

    private static Claim claim(String number, Policy policy, String description, String amount, String approved,
                               ClaimStatus status, int incidentDaysAgo, int filedDaysAgo, int fraudScore,
                               String adjuster) {
        Claim claim = new Claim();
        claim.setClaimNumber(number);
        claim.setPolicy(policy);
        claim.setDescription(description);
        claim.setClaimAmount(new BigDecimal(amount));
        claim.setApprovedAmount(approved != null ? new BigDecimal(approved) : null);
        claim.setStatus(status);
        claim.setIncidentDate(DateUtils.startOfDay(DateUtils.daysAgo(incidentDaysAgo)));
        claim.setFiledDate(DateUtils.daysAgo(filedDaysAgo));
        claim.setLastUpdated(DateUtils.daysAgo(filedDaysAgo));
        claim.setFraudScore(new Integer(fraudScore));
        claim.setAssignedAdjuster(adjuster);
        claim.addNote("system", "Seeded demo data");
        return claim;
    }
}
