package com.acme.claims.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoneyUtilsTest {

    @Test
    public void roundsHalfUpToTwoPlaces() {
        assertEquals(new BigDecimal("10.13"), MoneyUtils.round(new BigDecimal("10.125")));
        assertEquals(new BigDecimal("10.12"), MoneyUtils.round(new BigDecimal("10.124")));
    }

    @Test
    public void formatsWithGrouping() {
        assertEquals("1,234,567.50", MoneyUtils.format(new BigDecimal("1234567.5")));
    }

    @Test
    public void positiveCheck() {
        assertTrue(MoneyUtils.isPositive(new BigDecimal("0.01")));
        assertFalse(MoneyUtils.isPositive(BigDecimal.ZERO));
        assertFalse(MoneyUtils.isPositive(null));
    }

    @Test
    public void minPicksSmaller() {
        assertEquals(new BigDecimal("5"), MoneyUtils.min(new BigDecimal("5"), new BigDecimal("7")));
    }
}
