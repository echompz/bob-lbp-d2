package com.acme.claims.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generates human readable claim numbers such as {@code CLM-20240315-00042}.
 */
public final class ClaimNumberGenerator {

    private static int counter = 0;

    private ClaimNumberGenerator() {
    }

    public static synchronized String nextClaimNumber() {
        counter++;
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        StringBuffer sb = new StringBuffer("CLM-");
        sb.append(datePart);
        sb.append("-");
        sb.append(String.format("%05d", new Integer(counter)));
        return sb.toString();
    }
}
