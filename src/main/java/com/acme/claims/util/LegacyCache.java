package com.acme.claims.util;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Simple application-wide cache (hand-rolled singleton).
 */
public class LegacyCache {

    private static LegacyCache instance;

    private final Hashtable store = new Hashtable();

    private LegacyCache() {
    }

    public static LegacyCache getInstance() {
        if (instance == null) {
            synchronized (LegacyCache.class) {
                if (instance == null) {
                    instance = new LegacyCache();
                }
            }
        }
        return instance;
    }

    public void put(String key, Object value) {
        store.put(key, value);
    }

    public Object get(String key) {
        return store.get(key);
    }

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    public int size() {
        return store.size();
    }

    public void evictByPrefix(String prefix) {
        Enumeration keys = store.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.startsWith(prefix)) {
                store.remove(key);
            }
        }
    }

    public void clear() {
        store.clear();
    }
}
