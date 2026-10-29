# Prompts for the ClaimTrack modernization workshop

These prompts follow the phases in [WORKSHOP_GUIDE.md](WORKSHOP_GUIDE.md) and use the same numbers. They are written in plain language and don't depend on any particular Bob mode or menu. Paste them into whichever Bob workflow you're using, and adjust the wording as you like.

Tips that apply to every phase:

* **Ask for a plan before edits.** "Explain first, don't change any files yet" keeps you in control of design decisions.
* **Keep each step small.** One concern per request makes Bob's changes easy to review and easy to revert.
* **Verify every time.** After each change, run `./mvnw clean verify`. At each checkpoint, also run the smoke test. Paste failures back to Bob verbatim.
* **Use the catalog as ground truth.** [MODERNIZATION_CATALOG.md](MODERNIZATION_CATALOG.md) is the answer key. Use it to score Bob's findings, not to feed them to it.

---

## Phase 0: Explore

**0.1 Overview**
```text
Give me an overview of this application: what business problem it solves, the main domain entities
and how they relate, and the lifecycle of a claim (which statuses exist and which transitions are
allowed, and where in the code that's enforced). Keep it under one page.
```

**0.2 Fraud scoring end to end**
```text
Explain how a claim's fraud score is calculated, from the moment POST /api/claims is called until the
score is saved. Include every place rules are defined or loaded, how they're combined, and what
happens when the score is high. List each rule with its points.
```
*Facilitator check:* the answer must include the JavaScript rules in `fraud-rules.properties`, which are evaluated by `ScriptedRuleEvaluator`.

**0.3 API and security map**
```text
Produce a table of every REST endpoint: HTTP method, path (including the context path), controller
method, the service method it calls, and which roles may call it according to SecurityConfig.
Point out anything surprising in the security rules.
```

**0.4 Test coverage**
```text
Summarize the automated tests: which classes are unit tested, which behaviour is covered by the
integration test, and what is only covered by scripts/smoke-test.sh. Which areas of the code would you
be least confident changing, and why?
```

---

## Phase 1: Assess and plan

**1.1 Upgrade blockers**
```text
We want to move this application from Java 8 / Spring Boot 1.5 to Java 21 / Spring Boot 3.
Without changing any files, produce an assessment of everything that will break or must change.
Group findings by category (JDK API removals, build, Spring Boot, Spring Data, Spring Security,
API docs, validation, tests, javax->jakarta). For each finding give: file and symbol, why it breaks,
whether it fails at compile time, at startup or only at runtime, severity, and the recommended fix.
```

**1.2 Hidden runtime risks**
```text
Which of the issues you found will NOT be caught by the compiler? For each one, describe how it would
show up in production and which existing test (if any) would catch it. Also look for latent bugs in the
current code (thread safety, date arithmetic, resource handling) that the upgrade is a good moment to fix.
```

**1.3 Migration plan**
```text
Propose a staged migration plan with checkpoints. For each stage list the target versions, the changes
in recommended order, and how we verify the stage (build, tests, smoke test). Explain why you chose
this sequence over upgrading directly to the final versions.
```

*Facilitator check:* compare the answers with the "must find" table in the guide.

---

## Phase 2: Java 17 + Spring Boot 2.7

**2.1 Build file**
```text
Update pom.xml for Java 17 and Spring Boot 2.7.x. Remove compiler flags that are no longer needed,
update plugins that don't support Java 17, add spring-boot-starter-validation, and add
spring-boot-properties-migrator temporarily. Show me the diff and explain each change. Don't touch
Java sources yet.
```

**2.2 Mechanical source fixes**
```text
Fix the compile errors caused by the JDK and Spring Data upgrades: JAXB (add the dependencies, and
replace DatatypeConverter while keeping the checksum format identical: upper-case hex), sun.misc
Base64 (keep MIME-tolerant decoding), Spring Data 2 repository methods, and the deprecated MVC adapter
classes. Make the smallest changes that keep behaviour identical.
```

**2.3 Password encoding (decide before editing)**
```text
After the upgrade, every login fails. Explain why, and give me two or three options for fixing it in
this demo app, with trade-offs (for example {noop}, BCrypt, DelegatingPasswordEncoder). Don't
change code until I pick one.
```

**2.4 API documentation (decide before editing)**
```text
Springfox fails to start on Spring Boot 2.6+. Compare (a) keeping springfox with a path-matching
workaround and (b) migrating to springdoc-openapi 1.7. Include the effort, the annotation changes,
which URLs change, and what that means for the later Boot 3 step. Recommend one.
```

**2.5 Nashorn (decide before editing)**
```text
The scripted fraud rules use the Nashorn engine, which is removed from Java 15 onwards. Compare
adding the standalone nashorn-core dependency with rewriting the three scripted rules as Java
FraudRule classes. If we rewrite them, every claim must keep exactly the same fraud score:
tell me which tests prove that, and add any that are missing.
```

**2.6 Tests**
```text
Migrate the test suite to JUnit 5 and the Mockito version managed by Spring Boot 2.7. Replace
JUnit 4 rules and runners, update the assertion and exception APIs, fix strict-stubbing failures by
removing unnecessary stubs (don't switch to lenient unless you explain why), and update MockMvc
multipart calls. The test count must stay the same.
```

**2.7 Diagnose a failure**
```text
Here is the full output of ./mvnw clean verify (or the startup log). Identify the root cause, not just
the first symptom, explain it, and propose the smallest fix:

<paste output>
```

**2.8 Smoke-test differences**
```text
compare-smoke.sh reports these differences between the baseline and the upgraded app. For each one,
say whether it's an acceptable framework change or a behaviour regression, and point at the code
responsible:

<paste diff>
```

---

## Phase 3: Spring Boot 3 + Java 21

**3.1 Upgrade plan for Boot 3**
```text
We're now green on Java 17 / Boot 2.7. List the changes needed to reach Spring Boot 3.x on Java 21:
dependency coordinates, the javax->jakarta namespace change (make clear which javax packages must NOT
change), springdoc 2, Spring Security 6 configuration, and Hibernate 6. Order them so that we can build
after each step.
```

**3.2 Namespace migration**
```text
Migrate javax.persistence, javax.validation, javax.servlet, javax.annotation and javax.xml.bind to
jakarta.*, including the JAXB dependency coordinates. Leave JDK packages such as javax.script and
javax.crypto alone. Afterwards, list any remaining javax imports and explain why each one stays.
```

**3.3 Security configuration**
```text
Rewrite SecurityConfig for Spring Security 6 as a SecurityFilterChain bean using the lambda DSL and
authorizeHttpRequests. Keep exactly the same access rules and ordering as today, including the H2
console and the OpenAPI endpoints. Which tests verify that each rule still behaves the same?
```

---

## Phase 4: Idiomatic modern Java

**4.1 Explain before refactoring**
```text
Look at util/DateUtils and its callers. Before changing anything, list the bugs in the current
implementation (thread safety, DST, leap years) and write failing unit tests that demonstrate each one.
```

**4.2 java.time migration**
```text
Now migrate DateUtils and its callers to java.time (LocalDate for calendar dates, Instant or
LocalDateTime for timestamps). The JSON and XML wire formats must not change: show me how you keep
them identical, and make the tests from 4.1 pass.
```

**4.3 Replace hand-rolled infrastructure**
```text
Replace java.util.Observable/Observer in NotificationService with Spring application events, and
the java.util.Timer in ClaimEscalationScheduler with @Scheduled. Keep the same behaviour and
configuration properties. Show the diff for each one separately.
```

**4.4 Language features**
```text
Modernize the code where it improves readability, without changing behaviour: records for the DTOs,
switch expressions, pattern matching for instanceof, lambdas and streams instead of anonymous
classes and index loops, and constructor injection instead of field injection. Do one package at a
time and keep the tests green.
```

---

## Phase 5: Review

**5.1 Senior review of the whole migration**
```text
Review the full diff between main and this branch as a senior engineer would review a pull request.
Focus on: behaviour changes (API contracts, amounts, fraud scores, security rules), things that
compile but may fail at runtime, missing or weakened tests, and leftovers (temporary dependencies,
TODOs, deprecated APIs still in use). Rank the findings by risk.
```

**5.2 Pull request description**
```text
Write the pull request description for this migration: a summary, the version changes, notable
decisions and their reasons (password encoding, API docs, Nashorn), the verification performed, and
the known follow-ups.
```
