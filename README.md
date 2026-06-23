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
Day 1: Entity design and PostgreSQL setup
