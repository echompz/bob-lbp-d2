# Workshop guide: modernizing ClaimTrack with IBM Bob

A step-by-step guide for facilitators and participants. You'll take ClaimTrack from **Java 8 / Spring Boot 1.5**
to **Java 21 / Spring Boot 3**, using IBM Bob to explore, plan and carry out the migration. At every step you'll
prove that the behaviour is unchanged.

- **Prompts** for each phase: [BOB_PROMPTS.md](BOB_PROMPTS.md)
- **Answer key** (facilitators): [MODERNIZATION_CATALOG.md](MODERNIZATION_CATALOG.md). Item IDs such as `A3` or
  `E1` refer to it.
- **App background:** [ARCHITECTURE.md](ARCHITECTURE.md), [API.md](API.md)

> **About Bob's features.** This guide describes *what* to ask Bob to do in each phase, not which menu or mode to
> use. Bob's interface and modernization features change between releases. Use the Bob workflow that's available
> to you (chat, planning, a guided Java upgrade if your edition has one), and keep the checkpoints as they are.
> The checkpoints are tool-independent.

---

## Learning objectives

By the end, participants can:

1. Use an AI assistant to understand an unfamiliar legacy codebase quickly.
2. Produce a migration assessment and a **staged** plan, rather than a big-bang rewrite.
3. Explain why the Java upgrade and the Spring Boot upgrade have to happen together.
4. Recognize changes that compile but break at runtime, and prove behaviour with tests and API checks.
5. Review AI-generated changes critically. Know when to accept, adjust or reject them.

## Agenda

| # | Phase | Full (≈4 h) | Condensed (≈90 min) |
|---|---|---|---|
| – | Setup and baseline | 15 min | 10 min (done before the session) |
| 0 | Explore the codebase | 20 min | 10 min |
| 1 | Assess and plan | 25 min | 15 min |
| 2 | Java 17 + Spring Boot 2.7 | 75 min | 30 min (show the prepared branch for the second half) |
| 3 | Spring Boot 3 + Java 21 | 45 min | 15 min (demo) |
| 4 | Idiomatic modern Java | 40 min | 5 min (show one or two items) |
| 5 | Verify, review and wrap up | 20 min | 5 min |

**Condensed format tip:** before the session, prepare and push the branches `checkpoint/phase-2-boot27` and
`checkpoint/phase-3-boot3` (see [Facilitator notes](#facilitator-notes)). Then you can jump forward if a step
overruns.

---

## Prerequisites

| Tool | Why |
|---|---|
| **JDK 8** | Build and run the baseline |
| **JDK 17** | Target for phase 2 |
| **JDK 21** | Target for phase 3, and the proof of failure in phase 1 |
| Git | Branch per phase |
| IBM Bob | Your assistant for the whole workshop |
| `curl`, `bash` | The smoke test (on Windows, use Git Bash or WSL) |

Maven is **not** needed: use `./mvnw` (or `mvnw.cmd` on Windows).

Tip: a JDK manager such as SDKMAN! (`sdk use java 17.x`) or `jenv` makes switching JDKs painless. Otherwise, set
`JAVA_HOME` per command:

```bash
JAVA_HOME=/path/to/jdk-17 ./mvnw clean verify
```

---

## Setup and baseline

```bash
git clone <your-fork-url> claimtrack && cd claimtrack
git switch -c modernize/java21

# 1. Build on Java 8. Expect "Tests run: 71, Failures: 0, Errors: 0" and BUILD SUCCESS.
java -version                    # 1.8.x
./mvnw clean verify

# 2. Start the app and capture the baseline API behaviour
./mvnw spring-boot:run           # in a second terminal, or: java -jar target/claimtrack-1.0.0-LEGACY.jar
./scripts/smoke-test.sh --capture baseline
```

✅ **Checkpoint.** The build is green on JDK 8. The smoke test prints `Failed: 0`. `smoke-results/baseline/`
contains one `.txt` file per API call.

Take a quick look around: Swagger UI at http://localhost:8080/claimtrack/swagger-ui.html (log in as
`adjuster` / `adjuster123`), and the H2 console at `/claimtrack/h2-console`.

---

## Phase 0: Explore the codebase

**Goal:** understand the app well enough to change it safely, without reading every file.

Ask Bob (prompts **0.1–0.4**) to:

- Summarize what the app does, the main domain concepts and the claim lifecycle.
- Explain how fraud scoring works end to end. A good answer mentions both the Java rules **and** the JavaScript
  rules in `fraud-rules.properties`. If Bob misses the scripted rules, that's a teaching moment: ask a follow-up.
- Map each REST endpoint to the service method it calls and the role it requires.
- Identify the test coverage: what is unit-tested, what is integration-tested, and what only the smoke test covers.

**Discussion:** what would *you* have needed a day to learn here? How do you check that Bob's summary is right?
(Spot-check two claims against the code.)

---

## Phase 1: Assess and plan

**Goal:** an inventory of everything that blocks or complicates the upgrade, and a staged plan.

### 1a. Prove that it's broken, so you're not guessing

```bash
JAVA_HOME=/path/to/jdk-21 ./mvnw -q -o compile   # 49 compile errors in 6 files (JAXB, sun.misc)
JAVA_HOME=/path/to/jdk-21 java -jar target/claimtrack-1.0.0-LEGACY.jar
#   → fails at startup: InaccessibleObjectException (Spring 4.3 CGLIB vs the module system)
```

(`-o` keeps Maven offline, using the dependencies you've already downloaded.)

### 1b. Assessment

Ask Bob (prompts **1.1–1.3**) for an assessment grouped by category, with severity, and a migration plan.
**Then compare it with the catalog.** A strong assessment finds at least:

| Must find | Catalog |
|---|---|
| JAXB removed (compile) | A1 |
| `sun.misc.BASE64*` (compile) | A2 |
| **Nashorn removed: compiles, but fails at runtime** | A3 |
| Spring Boot 1.5 is incompatible with Java 9+ | A4 |
| Spring Boot property renames, actuator moving to `/actuator` | C1, C2 |
| Spring Data `findOne` → `findById` | D1 |
| Password encoder required (runtime) | E1 |
| `WebSecurityConfigurerAdapter` removed in Boot 3 | E2 |
| Springfox incompatible | F1 |
| JUnit 4 / Mockito 1 test stack | H1–H3 |
| `javax` → `jakarta` in Boot 3 | I1 |

Bonus points: A6 (default charset), G1 (validation starter), H3 (strict stubs), and any of the K latent bugs.

### 1c. Agree on the plan

The recommended plan, and the reason for it:

```
Java 8 + Boot 1.5  ──►  Java 17 + Boot 2.7  ──►  Java 21 + Boot 3.x  ──►  idiomatic Java
                        (still javax.*)          (jakarta.*)
```

- **You can't upgrade Java alone.** Boot 1.5 / Spring 4.3 don't run on Java 17 (A4).
- **You shouldn't jump straight to Boot 3.** You'd be changing the JDK, the framework *and* the `jakarta`
  namespace at once, which makes failures hard to attribute. Boot 2.7 is the official stepping stone. Its
  deprecation warnings point at exactly what Boot 3 removes.
- **Idiomatic changes come last**, on a working, tested modern stack, and as separate commits.

**Discussion:** where did Bob's plan differ from this, and was its reasoning sound?

---

## Phase 2: Java 17 + Spring Boot 2.7

**Goal:** green build and smoke test on **JDK 17**, Spring Boot 2.7.x, still on `javax.*`.

Switch to JDK 17. Work in small steps and run `./mvnw clean verify` after each one. Commit after every green step.

### Recommended order

| Step | What | Catalog | What you'll see before it's fixed |
|---|---|---|---|
| 2.1 | `pom.xml`: Boot parent 2.7.x, `java.version` 17, delete `-XDignore.symbol.file`, jacoco 0.8.12. Add `spring-boot-starter-validation`, and temporarily `spring-boot-properties-migrator` | B1, B2, G1, C1 | Many compile errors. That's expected |
| 2.2 | JAXB: add `jaxb-api` + `jaxb-runtime`, and replace `DatatypeConverter` with `HexFormat` | A1 | `package javax.xml.bind does not exist` |
| 2.3 | `sun.misc.BASE64*` → `java.util.Base64` (MIME decoder) | A2 | `package sun.misc does not exist` |
| 2.4 | Spring Data 2 API: `findById`/`Optional`, `existsById`, `saveAll`, `Sort.by` | D1 | `cannot find symbol: method findOne` |
| 2.5 | `WebMvcConfigurer`, `HandlerInterceptor` | C3 | Deprecation warnings |
| 2.6 | Security: password encoder (`{noop}` or BCrypt), permit `/actuator/health` and `/actuator/info` | E1, C2 | Compiles, but **every login fails** |
| 2.7 | Properties: `server.servlet.context-path`, `spring.servlet.multipart.*`, actuator exposure, `management.info.env.enabled` | C1, C2 | The migrator logs the warnings. URLs return 404 |
| 2.8 | Springfox → springdoc 1.7 (or the `ant_path_matcher` workaround, to keep the step small) | F1 | Startup fails: `documentationPluginsBootstrapper` NPE |
| 2.9 | Nashorn: rewrite the 3 rules in Java, or add `nashorn-core` | A3 | Startup fails: `nashorn is not available` |
| 2.10 | Tests: JUnit 5, Mockito 4 APIs, strict-stub fixes, `multipart()`, `/actuator/health` | H1–H5 | Test compile errors, then `UnnecessaryStubbingException` |
| 2.11 | Remove `spring-boot-properties-migrator` | – | – |

Bob is good at steps 2.2–2.5 and 2.10, which are mechanical. Steps 2.6, 2.8 and 2.9 involve design choices. Make Bob
explain the options (prompts **2.x**) and decide yourself.

### Checkpoint

```bash
java -version                       # 17.x
./mvnw clean verify                 # Tests run: 71, Failures: 0 (the count may change if you rewrote the Nashorn tests)
./mvnw spring-boot:run
HEALTH_PATH=/actuator/health ./scripts/smoke-test.sh --capture phase2
./scripts/compare-smoke.sh baseline phase2
```

✅ All smoke checks pass. `compare-smoke.sh` shows differences **only** in `health.txt` (the actuator response
format) and possibly in `unauthorized.txt` (the error body for a 401). If any other response changed (an amount,
a fraud score, a status), stop and investigate.

**Discussion:**

- Which changes compiled fine but would have broken production? (A3, E1, C1, C2, F1 and G1. See the catalog's
  "it compiled, but…" table.)
- Did the fraud score of the suspicious claim stay identical after replacing Nashorn?

---

## Phase 3: Spring Boot 3 + Java 21

**Goal:** green on **JDK 21** with Spring Boot 3.x and `jakarta.*`.

| Step | What | Catalog |
|---|---|---|
| 3.1 | Boot parent 3.x, `java.version` 21, springdoc 2.x (`springdoc-openapi-starter-webmvc-ui`) | I2, F1 |
| 3.2 | `javax.persistence` / `validation` / `servlet` / `annotation` / `xml.bind` → `jakarta.*`. **Not** `javax.script` or other JDK packages | I1, A5 |
| 3.3 | JAXB coordinates → `jakarta.xml.bind-api` + `jaxb-runtime` 4 | A1 |
| 3.4 | Security: `SecurityFilterChain` bean, `authorizeHttpRequests`, `requestMatchers`, `PathRequest.toH2Console()`, lambda DSL | E2–E4 |
| 3.5 | Hibernate 6: check the startup logs and the generated DDL | I3, C4 |

Keep `spring-boot-properties-migrator` in for the first run of this phase too. Boot 3 renamed a few more
properties.

**Watch for:** a blanket `javax.` → `jakarta.` replacement (it breaks `javax.script` if Nashorn is still
there, and `javax.crypto` in real apps), and the H2 console request matcher error (E3).

### Checkpoint

```bash
java -version                       # 21.x
./mvnw clean verify
./mvnw spring-boot:run
HEALTH_PATH=/actuator/health ./scripts/smoke-test.sh --capture phase3
./scripts/compare-smoke.sh phase2 phase3     # expect: no differences
```

> **Going further.** Spring Boot 4 and Java 25 (LTS) are available. Upgrading 3.x → 4 is a smaller step than the one
> you just made, and it's a good optional extension for fast groups.

---

## Phase 4: Idiomatic modern Java

**Goal:** use the modern stack to pay down tech debt, **without changing behaviour**. Choose items from catalog
sections J and K based on the time you have. Recommended, in order of demo value:

1. **`DateUtils` → `java.time`** (J1). It fixes three real bugs (K1–K3). Ask Bob to explain each bug first, then
   to make the change.
2. **Observer → Spring events** (J8) and **Timer → `@Scheduled`** (J6). These remove hand-rolled infrastructure.
3. **DTOs → records** (J10), **switch expressions** (J9), and **lambdas** (J2). Quick and visible.
4. **`ClaimImportService` CSV parsing** (K6). Write the failing test *first* (an empty incident date), then fix.
5. **Field → constructor injection** (J12) across the services.

Rules for this phase:

- Have Bob write or update tests **before** a change that alters behaviour (K-items).
- One concern per commit, so the reviewer can follow each one.
- Run `PremiumCalculatorTest` before and after changing `new BigDecimal(double)`. If a cent changes, decide
  deliberately.

### Checkpoint

```bash
./mvnw clean verify
HEALTH_PATH=/actuator/health ./scripts/smoke-test.sh --capture phase4
./scripts/compare-smoke.sh phase3 phase4
```

---

## Phase 5: Verify, review and wrap up

1. **Review the whole diff** (`git diff main...HEAD --stat`, then per file). Ask Bob to review it as a
   senior reviewer would (prompt **5.1**): what's risky, what's untested, and what got missed.
2. **Re-check the catalog**: which items are done, and which you deliberately left (and why)?
3. **Run the final proof:**
   ```bash
   ./mvnw clean verify && HEALTH_PATH=/actuator/health ./scripts/smoke-test.sh --capture final \
     && ./scripts/compare-smoke.sh baseline final
   ```
4. **Discussion:**
   - Where did Bob save the most time? Where did it need correcting?
   - Which checks caught problems that the compiler didn't?
   - How would you do this for 200 services instead of one? (Hint: the playbook, the checkpoints and the smoke tests
     matter more than the individual edits.)

---

## Facilitator notes

### Prepare checkpoint branches (recommended)

Do the migration once yourself before the workshop. Push one branch per phase checkpoint, so that anyone who gets
stuck can continue:

```bash
git switch -c checkpoint/phase-2-boot27   # after phase 2 is green
git switch -c checkpoint/phase-3-boot3    # after phase 3 is green
git switch -c checkpoint/phase-4-idiomatic
```

A participant who falls behind runs `git stash && git switch checkpoint/phase-2-boot27` and carries on.

### Where groups usually get stuck

| Symptom | Cause | Catalog |
|---|---|---|
| 401 on every call after phase 2, even with correct passwords | No password encoder | E1 |
| All URLs return 404 | `server.context-path` not renamed | C1 |
| Smoke test fails on `health` only | `HEALTH_PATH` not set, or actuator not exposed or permitted | C2 |
| `quote-invalid` returns 200 instead of 400 | `spring-boot-starter-validation` missing | G1 |
| `UnnecessaryStubbingException` in `ClaimServiceImplTest` | Mockito strict stubs; this is intended | H3 |
| App fails at startup with `documentationPluginsBootstrapper` | Springfox on Boot 2.6+ | F1 |
| App fails at startup: `nashorn is not available` | Nashorn | A3 |
| Startup error about "Spring MVC patterns" (Boot 3) | H2 console + `requestMatchers` | E3 |
| `Unsupported class file major version` during tests | Old jacoco | B2 |
| Document download fails with `Illegal base64 character` | Plain decoder used on MIME-encoded data | A2 |

### Resetting the in-memory data

The H2 database is in memory and re-seeded on every start, so a restart resets everything. The smoke test creates
new claims each time it runs. It always passes on a re-run, but **captures meant for `compare-smoke.sh` must come
from a freshly started app**, otherwise lists and report totals include the earlier run's claims. Two fresh
captures of the same build compare as identical.

### Customizing

- To make the workshop shorter, skip phase 3 and present it as a demo from the checkpoint branch.
- To make it harder, give participants **only** the README, and reveal the catalog after phase 1.
