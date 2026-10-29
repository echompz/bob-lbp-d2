# Contributing

Thanks for helping improve this workshop project.

## The baseline stays legacy, on purpose

`main` is the **"before"** state of a modernization exercise. The outdated dependencies, removed JDK APIs and
old-fashioned idioms are the teaching material. Please **don't** upgrade them on `main`. In particular:

- Keep Java 8, Spring Boot 1.5.x, springfox, JUnit 4 and Mockito 1.
- Keep the patterns that [docs/MODERNIZATION_CATALOG.md](docs/MODERNIZATION_CATALOG.md) lists (JAXB, `sun.misc`,
  Nashorn, `Observable`, `Timer`, `SimpleDateFormat` and so on), including the latent bugs in section K.

If a change makes a catalog item disappear, update the catalog in the same pull request.

## Welcome contributions

- New legacy patterns that make a good teaching moment, with a catalog entry and, ideally, a test that proves the
  behaviour before and after.
- Better tests, smoke-test steps or comparison normalization.
- Documentation fixes, clearer prompts and facilitator notes.
- Reference solution branches (for example `checkpoint/phase-2-boot27`) that follow the workshop guide.

## Before you open a pull request

```bash
java -version                                # must be 1.8.x
./mvnw clean verify                          # all tests green
./mvnw spring-boot:run &
./scripts/smoke-test.sh                      # Failed: 0
```

Keep each pull request focused on one concern, and describe which workshop phase or catalog item it affects.
