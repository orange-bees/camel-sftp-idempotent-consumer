# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 4.1.2 / Apache Camel 4.23.0 / Hawtio 5.3.0 (`hawtio-springboot4`) / Java 25 app that polls an SFTP server and copies files to a local directory, copying each file only once (Idempotent Consumer pattern). Hawtio is embedded for monitoring at http://localhost:8080/actuator/hawtio.

## Commands

- JDK 25 is required. If `mvn` runs through a jenv shim that ignores `JAVA_HOME`, set `JAVA_HOME` to a JDK 25 and call the Maven binary directly (e.g. `/opt/homebrew/bin/mvn`).
- Build: `mvn clean package`
- Run: `mvn spring-boot:run`
- All tests: `mvn test`
- Single test: `mvn -Dtest=ApplicationTests test` (or `-Dtest=ApplicationTests#contextLoads`)

There is no linter configured. Tests: `ApplicationTests` (`@SpringBootTest` loading the full context incl. the Camel route; it overrides `idempotent.file` and `file.output.uri` with temp paths via `@DynamicPropertySource`) and `IdempotentRepositoryTest` (plain JUnit on `FileIdempotentRepository`).

## Architecture

The whole app is one Camel route, `SFTPRoute` (`src/main/java/com/orangebees/camel/route/SFTPRoute.java`), registered as a Spring `@Component` `RouteBuilder` and started by Spring Boot (`Application`):

`sftp consumer` -> `idempotentConsumer(header("CamelFileName"), FileIdempotentRepository)` -> log processor -> `file://{{file.output.uri}}`

- The SFTP endpoint uses `noop=true` (remote files are never moved/deleted), so idempotency is the only thing preventing re-copying on every poll (`delay=10` seconds). Processed file names are persisted in the file named by the `idempotent.file` property, so state survives restarts.
- SFTP host/port (`localhost:2222`) is hardcoded in the route; username, password, JSch log level and file filter come from `{{sftp.*}}` / `{{file.filter}}` placeholders in `src/main/resources/application.properties`.
- `idempotent.file` is read via `Environment.getProperty` rather than a `{{}}` placeholder because the `File` constructor needs a plain value.
- `application.properties` holds environment-specific absolute paths (`file.output.uri`, `idempotent.file`) and must be customized per machine, as the README notes. Also enabled there: Camel supervised route controller, health checks, tracing/debugging, JMX, and Hawtio/Jolokia endpoints (Hawtio auth disabled).

## Known inconsistencies

- `sftp.idempotentFileRepositoryUri` is defined in `application.properties` but unused (the route reads `idempotent.file`).
- `file.filter='java'` is compared against `${file:ext}` via a Simple expression in `filterFile`, so only `.java` files are consumed.

Logging uses Log4j2 (`src/main/resources/log4j2.properties`); Lombok (`@Slf4j`) is used for loggers.
