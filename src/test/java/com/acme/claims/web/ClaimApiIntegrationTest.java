package com.acme.claims.web;

import com.acme.claims.util.DateUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Calendar;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests against the seeded in-memory database.
 * Tests never modify seeded claims - each mutating test files its own claim.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "claimtrack.escalation.enabled=false")
@AutoConfigureMockMvc
public class ClaimApiIntegrationTest {

    private static final RequestPostProcessor ADJUSTER = httpBasic("adjuster", "adjuster123");
    private static final RequestPostProcessor MANAGER = httpBasic("manager", "manager123");
    private static final RequestPostProcessor AUDITOR = httpBasic("auditor", "auditor123");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
    }

    @Test
    public void healthIsPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    public void listsSeededClaims() throws Exception {
        mvc.perform(get("/api/claims").with(ADJUSTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(8)));
        mvc.perform(get("/api/claims").param("status", "PAID").with(AUDITOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimNumber", is("CLM-SEED-00005")));
    }

    @Test
    public void unknownClaimIs404() throws Exception {
        mvc.perform(get("/api/claims/99999").with(ADJUSTER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Claim not found: 99999")));
    }

    @Test
    public void fullClaimLifecycle() throws Exception {
        Long id = submit("POL-AUTO-1001", "Scratched door in car park", "1800.50", 4);

        mvc.perform(post("/api/claims/" + id + "/review").with(ADJUSTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UNDER_REVIEW")))
                .andExpect(jsonPath("$.assignedAdjuster", is("adjuster")));

        String approve = "{\"decision\":\"APPROVE\",\"comment\":\"Photos confirm damage\"}";
        mvc.perform(post("/api/claims/" + id + "/decision").with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON).content(approve))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/claims/" + id + "/decision").with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON).content(approve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvedAmount", is(1300.50)));

        mvc.perform(post("/api/claims/" + id + "/pay").with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.notes", hasSize(4)));

        mvc.perform(post("/api/claims/" + id + "/pay").with(MANAGER))
                .andExpect(status().isConflict());
    }

    @Test
    public void suspiciousClaimIsAutoEscalated() throws Exception {
        Long id = submit("POL-TRVL-4001", "Luggage and camera equipment stolen abroad", "14000", 3);

        mvc.perform(get("/api/claims/" + id).with(ADJUSTER))
                .andExpect(jsonPath("$.status", is("ESCALATED")))
                .andExpect(jsonPath("$.fraudScore", greaterThanOrEqualTo(70)));
        mvc.perform(get("/api/claims/" + id + "/fraud-assessment").with(ADJUSTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel", is("HIGH")));
    }

    @Test
    public void validationErrorsAreReported() throws Exception {
        String badRequest = "{\"policyNumber\":\"POL-AUTO-1001\",\"description\":\"x\",\"claimAmount\":10,"
                + "\"incidentDate\":\"2024-01-01\"}";
        mvc.perform(post("/api/claims").with(ADJUSTER).contentType(MediaType.APPLICATION_JSON).content(badRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("incidentDate")));

        String lapsedPolicy = claimJson("POL-HOME-2002", "Leaking roof", "500.00", 2);
        mvc.perform(post("/api/claims").with(ADJUSTER).contentType(MediaType.APPLICATION_JSON).content(lapsedPolicy))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("not in force")));
    }

    @Test
    public void documentUploadAndDownload() throws Exception {
        Long id = submit("POL-HLTH-3001", "Dental emergency", "640.25", 1);
        MockMultipartFile file = new MockMultipartFile("file", "receipt.txt", "text/plain",
                "Dental clinic receipt: 640.25".getBytes());

        MvcResult upload = mvc.perform(fileUpload("/api/claims/" + id + "/documents").file(file).with(ADJUSTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sizeInBytes", is(29)))
                .andReturn();
        JsonNode doc = objectMapper.readTree(upload.getResponse().getContentAsString());

        mvc.perform(get("/api/claims/documents/" + doc.get("id").asLong() + "/content").with(ADJUSTER))
                .andExpect(status().isOk())
                .andExpect(content().string("Dental clinic receipt: 640.25"));
        mvc.perform(get("/api/claims/" + id + "/documents").with(ADJUSTER))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void reportsRequireManagerOrAuditor() throws Exception {
        mvc.perform(get("/api/reports/claims-summary/xml").with(ADJUSTER)).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/claims-summary/xml").with(AUDITOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<claimSummaryReport")));
        mvc.perform(get("/api/reports/claims-summary/csv").with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CLM-SEED-00001")));
    }

    @Test
    public void premiumQuote() throws Exception {
        Calendar dob = Calendar.getInstance();
        dob.add(Calendar.YEAR, -30);
        dob.add(Calendar.MONTH, -2);
        String quote = "{\"policyType\":\"AUTO\",\"dateOfBirth\":\"" + DateUtils.formatDate(dob.getTime())
                + "\",\"coverageAmount\":20000,\"priorClaims\":0}";
        mvc.perform(post("/api/policies/quote").with(ADJUSTER).contentType(MediaType.APPLICATION_JSON).content(quote))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age", is(30)))
                .andExpect(jsonPath("$.annualPremium", is(800.00)))
                .andExpect(jsonPath("$.monthlyPremium", is(66.67)));
    }

    /**
     * Only Bean Validation rejects this request - the service itself would happily quote it.
     * If validation silently stops working after an upgrade, this test is the one that notices.
     */
    @Test
    public void quoteBelowMinimumCoverageIsRejected() throws Exception {
        String quote = "{\"policyType\":\"AUTO\",\"dateOfBirth\":\"01/01/1990\",\"coverageAmount\":500,\"priorClaims\":0}";
        mvc.perform(post("/api/policies/quote").with(ADJUSTER).contentType(MediaType.APPLICATION_JSON).content(quote))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("coverageAmount")));
    }

    @Test
    public void customersAndPolicies() throws Exception {
        mvc.perform(get("/api/customers").with(ADJUSTER)).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/policies/POL-LIFE-5001").with(ADJUSTER))
                .andExpect(jsonPath("$.customerName", is("Liam Chen")));
    }

    private Long submit(String policy, String description, String amount, int incidentDaysAgo) throws Exception {
        MvcResult result = mvc.perform(post("/api/claims").with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(claimJson(policy, description, amount, incidentDaysAgo)))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.valueOf(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong());
    }

    private static String claimJson(String policy, String description, String amount, int incidentDaysAgo) {
        return "{\"policyNumber\":\"" + policy + "\",\"description\":\"" + description + "\",\"claimAmount\":"
                + amount + ",\"incidentDate\":\"" + DateUtils.formatDate(DateUtils.daysAgo(incidentDaysAgo)) + "\"}";
    }
}
