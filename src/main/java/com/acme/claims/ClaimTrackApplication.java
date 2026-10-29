package com.acme.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ClaimTrack - Acme Insurance claims processing service.
 *
 * <p>Legacy baseline: Java 8 / Spring Boot 1.5.x. This application is
 * intentionally written in a pre-Java-11 style so that it can be used to
 * demonstrate an end-to-end Java modernization flow.</p>
 */
@SpringBootApplication
public class ClaimTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimTrackApplication.class, args);
    }
}
