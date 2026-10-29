package com.acme.claims.util;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LegacyCacheTest {

    @After
    public void clear() {
        LegacyCache.getInstance().clear();
    }

    @Test
    public void isASingleton() {
        assertSame(LegacyCache.getInstance(), LegacyCache.getInstance());
    }

    @Test
    public void evictsByPrefix() {
        LegacyCache cache = LegacyCache.getInstance();
        cache.put("quote:a", "1");
        cache.put("quote:b", "2");
        cache.put("policy:c", "3");
        cache.evictByPrefix("quote:");
        assertFalse(cache.contains("quote:a"));
        assertTrue(cache.contains("policy:c"));
        assertEquals(1, cache.size());
    }
}
