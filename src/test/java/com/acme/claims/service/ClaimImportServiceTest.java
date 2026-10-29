package com.acme.claims.service;

import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.model.Claim;
import com.acme.claims.web.dto.ClaimRequest;
import com.acme.claims.web.dto.ImportResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClaimImportServiceTest {

    @Mock
    private ClaimService claimService;

    @InjectMocks
    private ClaimImportService importService;

    @Test
    public void importsValidRowsAndReportsBadOnes() throws Exception {
        when(claimService.submitClaim(any(ClaimRequest.class))).thenAnswer(new Answer<Claim>() {
            private int counter = 0;

            public Claim answer(InvocationOnMock invocation) throws Throwable {
                ClaimRequest request = (ClaimRequest) invocation.getArguments()[0];
                if ("POL-BAD".equals(request.getPolicyNumber())) {
                    throw new ClaimValidationException("Unknown policy number: POL-BAD");
                }
                Claim claim = new Claim();
                claim.setClaimNumber("CLM-IMPORT-" + (++counter));
                return claim;
            }
        });

        String csv = "policyNumber,incidentDate,claimAmount,description\n"
                + "POL-AUTO-1001,02/03/2025,1250.00,Side mirror damaged\n"
                + "\n"
                + "POL-HOME-2001,05/03/2025,800.00,Fence blown down, garden shed dented\n"
                + "POL-BAD,05/03/2025,100.00,Unknown policy\n"
                + "POL-AUTO-1001,05/03/2025,abc,Bad amount\n"
                + "POL-AUTO-1001,05/03/2025\n";

        ImportResult result = importService.importClaims(new ByteArrayInputStream(csv.getBytes()));

        assertEquals(2, result.getImported());
        assertEquals(3, result.getFailed());
        assertTrue(result.getErrors().get(0).startsWith("Line 5:"));
        assertTrue(result.getErrors().get(1).contains("Invalid claim amount"));
        assertTrue(result.getErrors().get(2).contains("Expected 4 columns"));

        ArgumentCaptor<ClaimRequest> captor = ArgumentCaptor.forClass(ClaimRequest.class);
        verify(claimService, times(3)).submitClaim(captor.capture());
        assertEquals("Fence blown down, garden shed dented", captor.getAllValues().get(1).getDescription());
        assertEquals(new BigDecimal("800.00"), captor.getAllValues().get(1).getClaimAmount());
    }
}
