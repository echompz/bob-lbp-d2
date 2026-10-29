# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0-LEGACY] - 2026-10-28

The first release of the legacy baseline for Java modernization workshops.

### Added

- ClaimTrack: an insurance claims service on Java 8 / Spring Boot 1.5.22, with customers, policies, premium quotes,
  the claim lifecycle, fraud scoring (Java and Nashorn rules), document attachments, a bulk CSV import, JAXB/CSV
  reports and an audit event feed.
- Seed data for 4 customers, 7 policies and sample claims in every status.
- A unit and integration test suite (JUnit 4, Mockito 1, Spring MockMvc) with JaCoCo coverage.
- `scripts/smoke-test.sh`: an end-to-end HTTP check that captures responses. `scripts/compare-smoke.sh`: a
  normalized before/after diff.
- Documentation: README, the architecture overview, the API reference, the modernization catalog (the answer key),
  the workshop guide and the prompts.
- A GitHub Actions workflow that builds, tests and smoke-tests on JDK 8.
