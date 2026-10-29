package com.acme.claims.web.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {

    private int totalRows;
    private int imported;
    private int failed;
    private List<String> importedClaimNumbers = new ArrayList<String>();
    private List<String> errors = new ArrayList<String>();

    public void recordSuccess(String claimNumber) {
        totalRows++;
        imported++;
        importedClaimNumbers.add(claimNumber);
    }

    public void recordFailure(int lineNumber, String reason) {
        totalRows++;
        failed++;
        errors.add("Line " + lineNumber + ": " + reason);
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImported() {
        return imported;
    }

    public int getFailed() {
        return failed;
    }

    public List<String> getImportedClaimNumbers() {
        return importedClaimNumbers;
    }

    public List<String> getErrors() {
        return errors;
    }
}
