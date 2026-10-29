# Modernization catalog (answer key)

Every deliberate legacy item in ClaimTrack: where it is, when it breaks, how to fix it, and how to prove the fix.
Facilitators can use it to check what participants (and Bob) found during assessment. Participants should try
the assessment **before** reading this file.

Locations are given as `file › symbol` rather than line numbers, so they stay valid as the code changes. All paths
are relative to `src/main/java/com/acme/claims/` unless they start with `src/`, `pom.xml` or `scripts/`.

## Legend

**Severity**

| Level | Meaning |
|---|---|
| 🔴 Blocker | The build fails, or the app fails to start or serve requests on the target stack |
| 🟠 Major | Builds, but behaviour changes or a feature silently breaks; or removal is scheduled |
| 🟡 Minor | Deprecated API or warning; works today |
| 🔵 Improvement | Idiomatic or modern-Java upgrade, or a latent bug that the upgrade is a good moment to fix |

**Phase:** where the item is normally handled. **P1** = Java 17 + Boot 2.7, **P2** = Boot 3 + Java 21,
**P3** = idiomatic Java. See [WORKSHOP_GUIDE.md](WORKSHOP_GUIDE.md).

## Summary

| Area | 🔴 | 🟠 | 🟡 | 🔵 |
|---|---|---|---|---|
| A. JDK APIs removed or changed | 4 | 2 | 4 | – |
| B. Build | 2 | – | 1 | – |
| C. Spring Boot 1.5 → 2.7 | 2 | 3 | 2 | – |
| D. Spring Data 1 → 2 | 1 | – | – | – |
| E. Spring Security 4 → 5 → 6 | 3 | – | 1 | 1 |
| F. API docs (Springfox) | 1 | – | – | – |
| G. Bean Validation | – | 1 | 1 | – |
| H. Tests | 3 | 1 | 1 | – |
| I. Boot 3: jakarta, Hibernate 6 | 2 | 1 | – | – |
| J. Idiomatic Java | – | – | – | 13 |
| K. Latent bugs | – | 6 | – | – |

---

## A. JDK APIs that were removed or changed

### A1 🔴 JAXB removed from the JDK (`javax.xml.bind`), P1

- **Where:** `report/ClaimSummaryReport`, `report/ClaimReportLine`, `report/StatusTotal`, `report/DateAdapter`,
  `service/ReportService › toXml`, `service/DocumentService › checksum` (`DatatypeConverter`).
- **Breaks:** Java 11 (JEP 320). It is a compile error: 49 errors across these 6 files on JDK 21.
- **Fix (P1, still `javax`):** add the API and a runtime explicitly.
  ```xml
  <dependency>
      <groupId>javax.xml.bind</groupId>
      <artifactId>jaxb-api</artifactId>
      <version>2.3.1</version>
  </dependency>
  <dependency>
      <groupId>org.glassfish.jaxb</groupId>
      <artifactId>jaxb-runtime</artifactId>  <!-- version managed by Boot 2.7 -->
  </dependency>
  ```
- **Fix (P2):** `jakarta.xml.bind:jakarta.xml.bind-api` + `org.glassfish.jaxb:jaxb-runtime` (both managed by
  Boot 3), and change the imports to `jakarta.xml.bind.*`.
- **`DatatypeConverter.printHexBinary`** in `DocumentService` is better replaced than re-added:
  `HexFormat.of().withUpperCase().formatHex(bytes)` (Java 17). **Keep upper case.** Stored checksums are upper-case
  hex, and `DocumentServiceTest` asserts `5D41402ABC4B2A76B9719D911017C592`.
- **Verify:** `ReportServiceTest`, the smoke test's `report-xml` step, and a diff of
  `smoke-results/baseline/report-xml.txt` against the modernized capture.

### A2 🔴 `sun.misc.BASE64Encoder` / `BASE64Decoder`, P1

- **Where:** `service/DocumentService › encode`, `› decode`.
- **Breaks:** Java 9. It is internal API and no longer exists, so it doesn't compile.
- **Fix:** `java.util.Base64`. **Trap:** `BASE64Encoder` inserts a line break every 76 characters. Documents
  already stored by the legacy app therefore contain newlines, and `Base64.getDecoder()` **rejects** them with
  `IllegalArgumentException`.
  - Decode with `Base64.getMimeDecoder()`. It accepts both the old and the new format.
  - Encode with `Base64.getEncoder()`, or with `getMimeEncoder()` if you need byte-identical storage.
- **Verify:** `DocumentServiceTest` and the smoke test's document upload and download steps (the checksum is checked
  on download). Teaching point: the in-memory H2 database hides this. A real database with existing rows wouldn't.

### A3 🔴 Nashorn JavaScript engine removed, P1. **Runtime-only break.**

- **Where:** `service/fraud/ScriptedRuleEvaluator › init` (`getEngineByName("nashorn")`), and the rules in
  `src/main/resources/fraud-rules.properties`.
- **Breaks:** Java 15 (JEP 372). **It still compiles**, because `javax.script` is still in the JDK. The app fails
  at **startup**: `IllegalStateException: JavaScript engine 'nashorn' is not available on this JVM`, and
  `ScriptedRuleEvaluatorTest` fails.
- **Fix options:**
  1. **Rewrite the three rules as Java `FraudRule` classes** (`HIGH_COVERAGE_RATIO` +20, `ROUND_AMOUNT` +10,
     `TRAVEL_HIGH_VALUE` +15). This is the recommended option: there are only three expressions and no new dependency.
     It removes a script-injection surface too.
  2. Add the standalone engine `org.openjdk.nashorn:nashorn-core:15.4` (Java 11+). The code stays unchanged.
  3. GraalJS (`org.graalvm.polyglot`). This is a larger change.
- **Verify:** `FraudDetectionServiceTest` and `ScriptedRuleEvaluatorTest` (rewrite them for option 1). In
  `ClaimApiIntegrationTest` the travel claim must still be `ESCALATED`, and so must the smoke test's suspicious claim.
  **The scores must be identical.** Note the arithmetic: `ROUND_AMOUNT` uses JavaScript `%` on a double.

### A4 🔴 JVM refuses Spring 4.3's CGLIB reflection, P1

- **Where:** the whole application. Spring 4.3 calls `ClassLoader.defineClass` reflectively.
- **Breaks:** Java 16+ (JEP 396/403, strong encapsulation). The Java 8 jar fails at startup on JDK 21 with
  `InaccessibleObjectException: ... module java.base does not "opens java.lang" to unnamed module`.
- **Fix:** upgrade the framework (Section C). Don't paper over it with `--add-opens`.

### A5 🟠 `javax.annotation.PostConstruct` / `PreDestroy` removed from the JDK, P1 / P2

- **Where:** `scheduler/ClaimEscalationScheduler`, `service/NotificationService`, `service/FraudDetectionService`,
  `service/fraud/ScriptedRuleEvaluator`.
- **Breaks:** Java 11 removed them from the JDK (JEP 320). They still compile here only because Tomcat's
  `tomcat-annotations-api` is on the classpath. In Boot 3 they must become `jakarta.annotation.*`.
- **Fix:** in P2 change to `jakarta.annotation.PostConstruct` / `PreDestroy`. Many of these uses disappear in
  P3 anyway (see J6, J7).

### A6 🟠 Platform default charset becomes UTF-8, P1 (Java 18+)

- **Where:** `service/ClaimImportService › importClaims` (`new InputStreamReader(input)`), `service/ReportService ›
  exportXmlToFile` (`new FileWriter(...)`).
- **Breaks:** Java 18 (JEP 400) changes the default from the OS charset to UTF-8. On Linux and macOS nothing changes;
  **on Windows servers the behaviour changes**.
- **Fix:** always pass a charset: `new InputStreamReader(in, StandardCharsets.UTF_8)`,
  `Files.newBufferedWriter(path, UTF_8)`.

### A7 🟡 `Class.newInstance()`, P1

- **Where:** `service/FraudDetectionService › init`.
- **Deprecated:** Java 9 (it propagates checked exceptions unchecked).
- **Fix:** `clazz.getDeclaredConstructor().newInstance()`. Better (P3): make each rule a Spring `@Component`
  and inject `List<FraudRule>`. The `claimtrack.fraud.rule-classes` property then goes away.

### A8 🟡 Boxed-primitive constructors (`new Integer`, `new Long`, `new Double`), P1

- **Where:** `service/FraudDetectionService`, `service/ClaimServiceImpl › submitClaim`, `service/ReportService ›
  buildClaimSummary`, `config/DataLoader`, `config/RequestTimingInterceptor › preHandle`, `util/MoneyUtils ›
  percentage`, `util/ClaimNumberGenerator`.
- **Deprecated:** Java 9; deprecated for removal since Java 16 (JEP 390).
- **Fix:** `Integer.valueOf(...)` or plain autoboxing.

### A9 🟡 `Object.finalize()`, P1 / P3

- **Where:** `util/TempFileHandle › finalize`.
- **Deprecated:** Java 9; deprecated for removal since Java 18 (JEP 421).
- **Fix:** see bug K5. This file is a lifecycle bug as well as a deprecation.

### A10 🟡 `BigDecimal.ROUND_HALF_UP` int constants, and `java.util.Observable`, P1

- **Where:** `util/MoneyUtils › round`, `service/PremiumCalculator`, `service/ClaimServiceImpl › calculatePayout`;
  and `service/NotificationService` (`extends Observable`), `service/notification/*Observer`.
- **Deprecated:** Java 9.
- **Fix:** `RoundingMode.HALF_UP`. For `Observable`, replace with Spring events: inject
  `ApplicationEventPublisher`, publish `ClaimEvent`, and listen with `@EventListener` (P3, see J8).

---

## B. Build

### B1 🔴 Compiler level and `-XDignore.symbol.file`, P1

- **Where:** `pom.xml` (`java.version` 1.8, `maven.compiler.source/target`, and `compilerArgs`).
- **Fix:** set `<java.version>17</java.version>` (then 21 in P2) and use `<maven.compiler.release>`. Delete
  `-XDignore.symbol.file`: it existed only to let `sun.misc` compile.

### B2 🔴 jacoco 0.8.2 can't instrument modern class files, P1

- **Where:** `pom.xml › jacoco.version`.
- **Breaks:** Java 17 class files need jacoco 0.8.7+, and Java 21 needs 0.8.11+. With older versions the agent fails
  and tests error out with `Unsupported class file major version`.
- **Fix:** `<jacoco.version>0.8.12</jacoco.version>`.

### B3 🟡 `commons-lang` 2.6, P1 / P3

- **Where:** `service/ClaimServiceImpl`, `service/ClaimImportService`, `service/FraudDetectionService`
  (`org.apache.commons.lang.StringUtils`).
- **Fix:** `org.apache.commons:commons-lang3` (it's managed by Boot) and change the package to
  `org.apache.commons.lang3`. Or drop the library and use `String.join`, `String.isBlank()` and `strip()` (Java 11+).

---

## C. Spring Boot 1.5 → 2.7

> Tip: during P1, add `org.springframework.boot:spring-boot-properties-migrator` (runtime scope). It logs every
> renamed property at startup. Remove it before you finish.

### C1 🔴 Renamed properties, P1

`src/main/resources/application.properties`:

| Boot 1.5 | Boot 2.x |
|---|---|
| `server.context-path` | `server.servlet.context-path` |
| `spring.http.multipart.max-file-size` | `spring.servlet.multipart.max-file-size` |
| `spring.http.multipart.max-request-size` | `spring.servlet.multipart.max-request-size` |

If you miss these, the app serves from `/` instead of `/claimtrack`, and the multipart limit falls back to 1 MB.
Neither is an error.

### C2 🔴 Actuator moves to `/actuator` and is locked down, P1

- **Where:** `application.properties` (`management.security.enabled`, `endpoints.*.sensitive`, both removed),
  `config/SecurityConfig` (it permits `/health` and `/info`), `scripts/smoke-test.sh` (`HEALTH_PATH`),
  `web/ClaimApiIntegrationTest` (`/health`).
- **Fix:**
  ```properties
  management.endpoints.web.exposure.include=health,info
  management.info.env.enabled=true   # Boot 2.6+: info.* properties are hidden by default
  ```
  Then permit `/actuator/health` and `/actuator/info` in security, update the test, and run the smoke test with
  `HEALTH_PATH=/actuator/health`.

### C3 🟠 `WebMvcConfigurerAdapter` / `HandlerInterceptorAdapter` deprecated, P1

- **Where:** `config/WebConfig`, `config/RequestTimingInterceptor`.
- **Fix:** `implements WebMvcConfigurer` (Spring 5.0, which has default methods) and `implements HandlerInterceptor`
  (deprecated since 5.3).

### C4 🟠 Hibernate 5.0 → 5.6, H2 1.4 → 2.x, P1

- IDs use `GenerationType.IDENTITY`, so Boot 2's `new_generator_mappings` default doesn't affect this app. Point it
  out as something to check in real apps.
- H2 2.x reserves more keywords. The ClaimTrack column names are safe, but run with `spring.jpa.show-sql=true`
  once and look for DDL errors.

### C5 🟠 Jackson date defaults, P1 (check, no change expected)

- Boot 2 sets `WRITE_DATES_AS_TIMESTAMPS=false`. ClaimTrack DTOs expose dates as pre-formatted `String`s
  (`dd/MM/yyyy`), so the JSON doesn't change. **Have participants confirm this by diffing the smoke captures**
  rather than assuming it.

### C6 🟡 `@RequestMapping(method = ...)` style, P1 / P3

- **Where:** all of `web/*Controller`.
- **Fix:** `@GetMapping`, `@PostMapping`. This is cosmetic, but it makes the security matchers easier to review.

### C7 🟡 Trailing-slash matching, P2 (Spring 6)

- Spring 6 no longer matches `/api/claims/` to `/api/claims`. The smoke test doesn't use trailing slashes, but
  mention it for any client that does.

---

## D. Spring Data JPA 1.x → 2.x

### D1 🔴 CRUD repository method changes, P1

| Spring Data 1.x | 2.x | Where |
|---|---|---|
| `findOne(id)` returns `T` or `null` | `findById(id)` returns `Optional<T>` | `ClaimServiceImpl › getClaim`, `CustomerService › getCustomer`, `DocumentService › attachDocument`, `› getDocument` |
| `exists(id)` | `existsById(id)` | `DocumentService › getDocuments`, `PolicyService` |
| `save(Iterable)` | `saveAll(Iterable)` | `config/DataLoader` |
| `new Sort(Direction, prop)` | `Sort.by(Direction, prop)` | `ClaimServiceImpl › getAllClaims`, `PolicyService` |

Idiomatic form: `claimRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Claim", id))`.
Also check the Mockito stubs in `ClaimServiceImplTest`: `when(repo.findOne(..))` must become
`when(repo.findById(..)).thenReturn(Optional.of(..))`.

---

## E. Spring Security 4 → 5 → 6

### E1 🔴 Passwords must have an encoder (Security 5), P1. **Runtime-only break.**

- **Where:** `config/SecurityConfig › configure(AuthenticationManagerBuilder)` (`.password("adjuster123")`).
- **Breaks:** It compiles and starts, but **every login fails** with
  `IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"`. Every API call returns 401
  or 500.
- **Fix:** the quick fix is `{noop}adjuster123`. The proper fix is BCrypt:
  `PasswordEncoderFactories.createDelegatingPasswordEncoder()` with `{bcrypt}` hashes (see E5).

### E2 🔴 `WebSecurityConfigurerAdapter` removed (Security 6), P2

- **Where:** `config/SecurityConfig`. It is deprecated in Security 5.7 (Boot 2.7) and removed in 6.0.
- **Fix:** expose a `SecurityFilterChain` `@Bean`, plus an `InMemoryUserDetailsManager` (or `UserDetailsService`)
  `@Bean`. Using the lambda DSL in P1 already makes P2 trivial.

### E3 🔴 `authorizeRequests` / `antMatchers` → `authorizeHttpRequests` / `requestMatchers`, P2

- **Where:** `config/SecurityConfig › configure(HttpSecurity)`.
- **Trap:** the app has **two servlets** (`DispatcherServlet` and the H2 console). From Spring Security 6.1.2,
  `requestMatchers("/h2-console/**")` fails at startup with *"This method cannot decide whether these patterns are
  Spring MVC patterns or not"*. Use `PathRequest.toH2Console()` (Boot) or `AntPathRequestMatcher.antMatcher(...)`.
- **Keep the rule order.** The `POST /api/claims/*/decision` and `/pay` rules for MANAGER must stay above the
  general `/api/**` rule. `ClaimApiIntegrationTest › fullClaimLifecycle` (adjuster gets 403 on decision) and
  `› reportsRequireManagerOrAuditor` catch this, and so do the smoke test's `decide-403` and `report-403`.

### E4 🟡 `.and()` chaining, P2

- Deprecated in Security 6.1. The lambda DSL replaces it: `http.csrf(csrf -> csrf.disable())`,
  `headers(h -> h.frameOptions(f -> f.sameOrigin()))`, `httpBasic(b -> b.realmName("ClaimTrack"))`.

### E5 🔵 Hard-coded plain-text credentials, P3

- Hash the passwords with BCrypt and move them out of source, for example into properties or environment variables.
  This is a demo app, but it's the right habit to show.

---

## F. API documentation

### F1 🔴 Springfox 2.9.2 breaks on Boot 2.6+ and doesn't support Boot 3, P1

- **Where:** `pom.xml` (`springfox-swagger2`, `springfox-swagger-ui`), `config/SwaggerConfig`, `@Api` /
  `@ApiOperation` / `@ApiParam` on all `web/*Controller` classes, and the permitted paths in `SecurityConfig`.
- **Breaks:** On Boot 2.6+ startup fails with `NullPointerException ... documentationPluginsBootstrapper`, because
  the path-matching strategy changed. Springfox is unmaintained and has no Boot 3 / `jakarta` support.
- **Workaround (P1 only):** `spring.mvc.pathmatch.matching-strategy=ant_path_matcher`.
- **Fix:** springdoc-openapi: `org.springdoc:springdoc-openapi-ui:1.7.0` on Boot 2.7, or
  `springdoc-openapi-starter-webmvc-ui:2.x` on Boot 3. Delete `SwaggerConfig` (or turn it into an `OpenAPI` bean),
  then map the annotations:

  | Springfox | springdoc (OpenAPI 3) |
  |---|---|
  | `@Api(tags = ...)` | `@Tag(name = ...)` |
  | `@ApiOperation(value = ...)` | `@Operation(summary = ...)` |
  | `@ApiParam` | `@Parameter` |

  Security must permit `/v3/api-docs/**`, `/swagger-ui/**` and `/swagger-ui.html`.

---

## G. Bean Validation

### G1 🟠 Validation silently disappears on Boot 2.3+, P1

- **Where:** `pom.xml`, `@Valid` in `ClaimController › submitClaim` and `› decide`, `PolicyController › quote`.
- **Breaks:** Boot 2.3 removed validation from `spring-boot-starter-web`. Without
  `spring-boot-starter-validation`, `@Valid` **does nothing and no error is raised**. Requests that should get 400
  are processed instead.
- **Fix:** add `spring-boot-starter-validation`.
- **Verify:** the smoke test's `quote-invalid` step and the integration test `quoteBelowMinimumCoverageIsRejected`
  both send a premium quote with `coverageAmount` 500, which is below `@DecimalMin("1000")`. **Only Bean Validation
  rejects it**, because the service would quote it. Every other 400 check is also caught by service-level
  validation, so they would keep passing.

### G2 🟡 `org.hibernate.validator.constraints.NotBlank`, P1 / P2

- **Where:** `web/dto/ClaimRequest`, `web/dto/ClaimDecisionRequest`, `web/dto/PremiumQuoteRequest`.
- **Fix:** standard `javax.validation.constraints.NotBlank` (Bean Validation 2.0) in P1, then
  `jakarta.validation.constraints.NotBlank` in P2.

---

## H. Tests

### H1 🔴 JUnit 4 is no longer included (Boot 2.4+), P1

- **Where:** all of `src/test/java` (`org.junit.Test`, `@RunWith`, `@Before`, `org.junit.Assert`).
- **Breaks:** `spring-boot-starter-test` 2.4+ ships JUnit 5 only, so the JUnit 4 imports don't compile. If someone
  adds `junit-vintage-engine` as a stopgap, the tests run on the vintage engine. That's fine for one step, but not
  as the end state.
- **Fix:** migrate to JUnit Jupiter:

  | JUnit 4 | JUnit 5 |
  |---|---|
  | `@RunWith(SpringRunner.class)` + `@SpringBootTest` | `@SpringBootTest` (the extension is included) |
  | `@RunWith(MockitoJUnitRunner.class)` | `@ExtendWith(MockitoExtension.class)` |
  | `@Before` / `@After` | `@BeforeEach` / `@AfterEach` |
  | `@Test(expected = X.class)` | `assertThrows(X.class, () -> ...)` |
  | `org.junit.Assert.*` | `org.junit.jupiter.api.Assertions.*` (note: the message is the **last** argument) |
  | `public` test classes and methods | package-private is fine |

### H2 🔴 Mockito 1.10 APIs removed (Mockito 4 in Boot 2.7, 5 in Boot 3), P1

- **Where:** `service/ClaimServiceImplTest`, `service/ClaimImportServiceTest`, `service/fraud/FraudDetectionServiceTest`.

  | Mockito 1 | Mockito 4/5 |
  |---|---|
  | `org.mockito.runners.MockitoJUnitRunner` | `org.mockito.junit.jupiter.MockitoExtension` |
  | `org.mockito.Matchers.*` | `org.mockito.ArgumentMatchers.*` |
  | `anyListOf(Claim.class)` | `anyList()` |
  | `anyObject()` | `any()` |

### H3 🔴 Strict stubbing: `UnnecessaryStubbingException`, P1. **Deliberate.**

- **Where:** `service/ClaimServiceImplTest › setUp`. It stubs repository and fraud-service calls that only some tests
  use.
- **Breaks:** `MockitoExtension` uses strict stubs by default. Tests that don't use a setUp stub **fail** with
  `UnnecessaryStubbingException`.
- **Fix:** move each stub into the tests that need it. That's the right fix, and it's a good discussion point. A
  narrowly scoped `lenient()` also works. Don't use `@MockitoSettings(strictness = LENIENT)` on the whole class.

### H4 🟠 `MockMvcRequestBuilders.fileUpload`, P1

- **Where:** `web/ClaimApiIntegrationTest` (document upload and CSV import).
- **Fix:** `multipart(...)`. `fileUpload` was deprecated in Spring 5.0 and removed in 6.

### H5 🟡 `SpringRunner` + `@SpringBootTest(properties = ...)`, P1

- `@SpringBootTest` itself carries over unchanged. Only the runner goes. Keep
  `claimtrack.escalation.enabled=false` so the timer doesn't interfere with assertions.

---

## I. Spring Boot 3: the jakarta namespace and Hibernate 6

### I1 🔴 `javax.*` → `jakarta.*`, P2

| Package | Where |
|---|---|
| `javax.persistence` | all of `model/*` |
| `javax.validation` | `web/dto/*`, controllers (`@Valid`) |
| `javax.servlet` | `config/RequestTimingInterceptor`, `web/GlobalExceptionHandler` |
| `javax.annotation` | see A5 |
| `javax.xml.bind` | see A1 |

**Don't touch** `javax.script` (it's part of the JDK, and leaves with Nashorn anyway) or `javax.crypto` and other
JDK packages. A blind find-and-replace of `javax.` → `jakarta.` is the classic mistake here. Ask participants to
spot whether Bob (or they) did that.

### I2 🔴 Java 17 baseline, P2

- Boot 3 requires Java 17+. Target 21 (the current LTS) in `pom.xml`.

### I3 🟠 Hibernate 6 and `@Temporal`, P2 / P3

- **Where:** `model/Claim`, `model/Policy`, `model/Customer`, `model/ClaimNote`, `model/ClaimDocument`.
- `@Temporal` with `java.util.Date` still works in Hibernate 6 but is legacy. Map `LocalDate` (incident date, policy
  dates, date of birth) and `Instant` or `LocalDateTime` (filed, updated, uploaded) directly, and drop `@Temporal`.
  This connects with J1.

---

## J. Idiomatic modern Java (P3)

| # | Item | Where | Modern form |
|---|---|---|---|
| J1 | `java.util.Date` / `Calendar` / `SimpleDateFormat` | `util/DateUtils`, all entities, DTO mappers, `ClaimNumberGenerator`, `report/DateAdapter` | `LocalDate`, `Instant`, `DateTimeFormatter` (immutable and thread-safe), `ChronoUnit.DAYS.between`, `Period.between`. Fixes K1, K2 and K3 |
| J2 | Anonymous inner classes | `FraudDetectionService` (`Callable`, `Comparator`), `ReportService` (`Comparator`), `DataLoader` (`CommandLineRunner`), `ClaimEscalationScheduler` (`TimerTask`) | Lambdas, `Comparator.comparing(Claim::getFiledDate).reversed()` |
| J3 | Index loops and manual accumulation | `ReportService › buildClaimSummary`, `FraudDetectionService` | Streams: `Collectors.groupingBy(Claim::getStatus, () -> new EnumMap<>(ClaimStatus.class), ...)` |
| J4 | `Hashtable`, `Vector`, `StringBuffer`, `Enumeration`, raw types | `util/LegacyCache`, `fraud/FraudAssessment`, `ClaimNumberGenerator`, `ReportService › toCsv`, `Customer › getFullName`, `ScriptedRuleEvaluator › loadRules` | `ConcurrentHashMap`, `ArrayList` / `List.copyOf`, `StringBuilder` / `String.join`, generics |
| J5 | Manual `try`/`finally` close | `ClaimImportService › importClaims`, `ScriptedRuleEvaluator › loadRules`, `ReportService › exportXmlToFile` | try-with-resources. Fixes K4 |
| J6 | `java.util.Timer` | `scheduler/ClaimEscalationScheduler` | `@EnableScheduling` + `@Scheduled(fixedRateString = "${claimtrack.escalation.interval-ms}")`. A `Timer` thread dies on an uncaught `Error` |
| J7 | Hand-made `ExecutorService` never shut down | `FraudDetectionService` (`Executors.newFixedThreadPool(4)`) | Inject Spring's `ThreadPoolTaskExecutor` (lifecycle-managed), or on Java 21 `Executors.newVirtualThreadPerTaskExecutor()` in try-with-resources |
| J8 | `Observable` / `Observer` | `NotificationService`, `notification/*Observer` | `ApplicationEventPublisher` + `@EventListener` (optionally `@TransactionalEventListener`) |
| J9 | Classic `switch` statements | `model/ClaimStatus › canTransitionTo`, `service/PremiumCalculator` | Switch expressions (Java 14), or an `EnumSet` transition map |
| J10 | Mutable DTO classes with getters and setters | `web/dto/*` | `record` (Java 16). Jackson and Bean Validation both support records in Boot 3 |
| J11 | `instanceof` + cast | `notification/*Observer`, `ScriptedRuleEvaluator` | Pattern matching: `if (arg instanceof ClaimEvent event)` (Java 16) |
| J12 | Field injection (`@Autowired` on fields) | every service and controller | Constructor injection with `final` fields (and then `ReflectionTestUtils` in tests can go) |
| J13 | Logging by string concatenation, `printStackTrace`, `System.out` | `FraudDetectionService`, `ScriptedRuleEvaluator`, `ClaimEscalationScheduler`, `notification/AuditLogObserver` | SLF4J placeholders `log.info("Loaded {} rules", n)`, `log.warn("...", e)` |

Also worth pointing out: the escaped, concatenated JSON strings in `ClaimApiIntegrationTest` are a good fit for
**text blocks** (Java 15). And `new BigDecimal(double)` in `PremiumCalculator › baseRate`, `› ageFactor` and
`› claimsFactor` should become `BigDecimal.valueOf(double)` or a string constant. `new BigDecimal(0.1)` is
`0.1000000000000000055511151231257827…`. **Run `PremiumCalculatorTest` before and after**: if a premium changes by a
cent, that is a behaviour change to discuss, not a refactor.

---

## K. Latent bugs hidden in the legacy idioms

These work in the demo, which is what makes them good teaching material. Modernizing the idiom removes the bug.
Ask participants to explain *why* each one is a bug.

### K1 🟠 Shared static `SimpleDateFormat` is not thread-safe

- **Where:** `util/DateUtils › DATE_FORMAT`, `› DATETIME_FORMAT`. These are used from request threads, the fraud
  thread pool and the timer thread.
- **Symptom:** under concurrency, parsing returns wrong dates or throws `NumberFormatException` /
  `ArrayIndexOutOfBoundsException`. It's very hard to reproduce in a demo.
- **Fix:** `DateTimeFormatter` (J1).

### K2 🟠 `DateUtils › daysBetween` is wrong across daylight-saving changes

- It computes `millis / (24h)` and truncates. A 23-hour DST day counts as 0, which shifts the policy-age and
  late-reporting fraud rules and the 365-day reporting limit by one day.
- **Fix:** `ChronoUnit.DAYS.between(startDate, endDate)` on `LocalDate`.

### K3 🟠 `DateUtils › calculateAge` is off by one around leap years

- It compares `DAY_OF_YEAR`. After Feb 28, the same calendar date has a different day-of-year in leap and non-leap
  years, so for a few days a year some birthdays are counted early or late. That changes the
  `PremiumCalculator` age band.
- **Fix:** `Period.between(dateOfBirth, LocalDate.now()).getYears()`.

### K4 🟠 `ReportService › exportXmlToFile` leaks a file handle

- `writer.close()` isn't in a `finally` block, so if `write` throws the handle leaks.
- **Fix:** try-with-resources (J5), or simply `Files.writeString(path, xml, UTF_8)`.

### K5 🟠 `TempFileHandle` deletes the file when the GC feels like it

- The handle deletes its file in `finalize()`. `exportXmlToFile` returns the **path** and drops the handle, so the
  exported file can disappear at any GC before the "nightly batch" picks it up.
- **Fix:** let the caller own the lifecycle: `Files.createTempFile` and explicit deletion. If you need a safety
  net, use `java.lang.ref.Cleaner`, but not for a file someone else is meant to read.

### K6 🟠 `ClaimImportService` drops empty CSV fields

- `StringTokenizer` treats `,,` as a single delimiter, so an empty field disappears and the later columns shift
  left. For `POL-AUTO-1001,,1200,Hail damage` the error is *"Expected 4 columns but found 3"* instead of *"incident
  date is missing"*. If the description contains a comma, the count check passes and `1200` is parsed as the
  incident date. Quoted fields that contain commas aren't supported either.
- **Fix:** `line.split(",", -1)` with a column count check, or a CSV library such as Commons CSV.
- **Verify:** add a test with an empty incident date. Today it reports the wrong reason.

### Also worth discussing (not bugs today)

- **`LegacyCache` double-checked locking without `volatile`** (`util/LegacyCache › getInstance`) is broken under the
  Java Memory Model: another thread may see a partially constructed instance. Replace it with a Spring bean or the
  holder idiom, and replace `Hashtable` with `ConcurrentHashMap` (or `@Cacheable` + Caffeine).
- **MD5 document checksums** (`DocumentService › checksum`) catch accidental corruption, but not tampering. Moving to
  SHA-256 is a **data migration**, because stored checksums would no longer match. It's a good example of a change
  that is *not* a pure refactor.
- **`ClaimNumberGenerator`** keeps its counter in a static `int`. Numbers restart at 1 after every restart and aren't
  unique across instances. Use a database sequence.

---

## Quick reference: "it compiled, but…"

These are the items that a successful `mvn compile` **won't** reveal. They are why the workshop insists on tests
plus the smoke test.

| Item | What you see |
|---|---|
| A3 Nashorn | App fails at startup |
| A2 Base64 MIME | Decoding failures on old data only |
| C1 context path | Every URL returns 404 (the app is now at `/`) |
| C2 actuator | `/health` returns 404 |
| E1 password encoder | Every authenticated call fails |
| F1 Springfox | App fails at startup on Boot 2.6+ |
| G1 validation | Invalid requests are accepted |
| H3 strict stubs | Unit tests fail with `UnnecessaryStubbingException` |
