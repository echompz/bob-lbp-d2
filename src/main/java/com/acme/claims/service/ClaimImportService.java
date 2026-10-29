package com.acme.claims.service;

import com.acme.claims.exception.ClaimValidationException;
import com.acme.claims.model.Claim;
import com.acme.claims.web.dto.ClaimRequest;
import com.acme.claims.web.dto.ImportResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
 * Bulk import of claims from the broker CSV feed.
 *
 * <p>Format (header row optional):</p>
 * <pre>
 * policyNumber,incidentDate,claimAmount,description
 * POL-AUTO-1001,02/03/2025,1250.00,Side mirror damaged in car park
 * </pre>
 */
@Service
public class ClaimImportService {

    private static final Logger log = LoggerFactory.getLogger(ClaimImportService.class);

    private static final int EXPECTED_COLUMNS = 4;

    @Autowired
    private ClaimService claimService;

    public ImportResult importClaims(InputStream input) throws IOException {
        ImportResult result = new ImportResult();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input));
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().length() == 0) {
                    continue;
                }
                if (lineNumber == 1 && line.toLowerCase().startsWith("policynumber")) {
                    continue;
                }

                StringTokenizer tokens = new StringTokenizer(line, ",");
                if (tokens.countTokens() < EXPECTED_COLUMNS) {
                    result.recordFailure(lineNumber, "Expected " + EXPECTED_COLUMNS + " columns but found "
                            + tokens.countTokens());
                    continue;
                }
                String policyNumber = tokens.nextToken().trim();
                String incidentDate = tokens.nextToken().trim();
                String amountText = tokens.nextToken().trim();
                StringBuffer description = new StringBuffer(tokens.nextToken().trim());
                while (tokens.hasMoreTokens()) {
                    description.append(',').append(tokens.nextToken());
                }

                try {
                    BigDecimal amount = new BigDecimal(amountText);
                    Claim claim = claimService.submitClaim(
                            new ClaimRequest(policyNumber, description.toString(), amount, incidentDate));
                    result.recordSuccess(claim.getClaimNumber());
                } catch (NumberFormatException e) {
                    result.recordFailure(lineNumber, "Invalid claim amount '" + amountText + "'");
                } catch (ClaimValidationException e) {
                    result.recordFailure(lineNumber, StringUtils.join(e.getViolations(), "; "));
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        log.info("Claim import finished: " + result.getImported() + " imported, " + result.getFailed() + " failed");
        return result;
    }
}
