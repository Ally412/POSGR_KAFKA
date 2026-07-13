# Animal Shelter Management System

Phase 2 Learning Project: PostgreSQL + Kafka + Spring Boot 4

## Project Overview
A system to manage animal shelters with:
- Animal inventory (stored in PostgreSQL)
- Adoption tracking
- Medical records
- Event-driven architecture (Kafka for events)

## Learning Goals
- **PostgreSQL:** Entity relationships, migrations, complex queries, testing
- **Kafka:** Producers, consumers, event-driven architecture, error handling
- **Spring Boot:** Async processing, caching, REST API design

## Timeline
- **Week 1 (Days 1-7):** PostgreSQL setup + REST API
- **Week 2 (Days 8-14):** Kafka integration + event handling

## Tech Stack
- Spring Boot 4.1 (on Spring Framework 7)
- Java 21 (Boot 4 baseline is 17, supported through 26)
- Gradle (Groovy DSL) — via the wrapper (`./gradlew`)
- PostgreSQL
- Spring Data JPA
- Kafka
- Jackson 3 (Boot 4 default; Jackson 2 is deprecated)
- Docker Compose
- Spring Cache
- JUnit 5 + Testcontainers

> **Boot 4 note:** Spring Boot 4.0 went GA in November 2025 and restructured its
> starters (e.g. `spring-boot-starter-webmvc` replaces `-web`, `spring-boot-starter-kafka`
> replaces `spring-kafka`, and tests use per-feature `*-test` starters). Most online
> tutorials still target Boot 3.x — see the starter cheat-sheet in `Stages.md`.

## Getting Started

### Prerequisites
- JDK 21 (Temurin recommended)
- Docker (for the local Postgres and for Testcontainers-based tests)

### Run locally
```bash
# 1. clone
git clone git@github.com:ally412/POSGR_KAFKA.git
cd POSGR_KAFKA

# 2. start Postgres (docker-compose)
docker compose up -d

# 3. build (also runs the tests)
./gradlew build

# 4. run the app
./gradlew bootRun
```
The app starts on `http://localhost:8080`. Flyway applies migrations on startup and
`ddl-auto=validate` checks the schema matches the JPA entities.

### Run the tests
```bash
./gradlew test
```
Integration tests (`*IT`) start a throwaway Postgres via Testcontainers, so **Docker
must be running**.

## Configuration

The datasource is externalized via environment variables (with local-dev defaults).
Override these when deploying (e.g. in Kubernetes); unset, the app runs against a
local Postgres on `localhost:5432`.

| Variable      | Default     | Description        |
|---------------|-------------|--------------------|
| `DB_HOST`     | `localhost` | Postgres host      |
| `DB_PORT`     | `5432`      | Postgres port      |
| `DB_NAME`     | `shelter`   | Database name      |
| `DB_USER`     | `shelter`   | Database username  |
| `DB_PASSWORD` | `shelter`   | Database password  |

These map to Spring's `spring.datasource.*`. In tests, Testcontainers supplies its
own connection via `@ServiceConnection`, ignoring these.
