package com.acme.claims.service;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentServiceTest {

    @Test
    public void base64RoundTrip() {
        byte[] original = "Police report #4471 - rear-end collision".getBytes();
        String encoded = DocumentService.encode(original);
        assertTrue(Arrays.equals(original, DocumentService.decode(encoded)));
    }

    @Test
    public void encodesKnownValue() {
        assertEquals("aGVsbG8=", DocumentService.encode("hello".getBytes()));
    }

    @Test
    public void md5ChecksumIsUppercaseHex() {
        assertEquals("5D41402ABC4B2A76B9719D911017C592", DocumentService.checksum("hello".getBytes()));
    }
}
