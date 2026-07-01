# Project Stages

> **Spring Boot 4 starter cheat-sheet** — Boot 4 restructured starters. When following
> Boot 3.x tutorials (most of them), translate names:
>
> | Boot 3.x (tutorials) | Boot 4 (this project) |
> |----------------------|-----------------------|
> | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
> | `spring-kafka` | `spring-boot-starter-kafka` |
> | `flyway-core` | `spring-boot-starter-flyway` |
> | `spring-boot-starter-test` (one fat starter) | per-feature `*-test` (`...-data-jpa-test`, `...-kafka-test`, ...) |
> | Jackson 2 (`com.fasterxml.jackson.*`) | Jackson 3 (`tools.jackson.*`) |

## Stage 1: Project Setup
- Spring Boot 4.1 project creation with Gradle (Groovy DSL), generated via Spring Initializr
- Dependencies (Boot 4 names): `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`,
  PostgreSQL driver, `spring-boot-starter-kafka`, `spring-boot-starter-flyway`, Lombok, Validation
- Project structure and package organization (feature-based: `animal/`, `adoption/`, `medical/`, `event/`, `config/`)
- Basic `application.properties` configuration (the generated file — keep `.properties` format)

## Stage 2: Entity Design
- Identify domain entities (Animal, Adoption, MedicalRecord, User)
- Define relationships (1:1, 1:N, M:N)
- Create JPA entities with annotations
- Establish referential integrity with foreign keys

## Stage 3: PostgreSQL Integration
- Database setup and connection configuration
- Flyway migrations for schema creation
- Hibernate entity mapping
- Test with embedded PostgreSQL/Testcontainers

## Stage 4: Repository & Query Layer
- Spring Data JPA repositories
- Custom query methods
- Native SQL queries where needed
- Testing repository layer

## Stage 5: REST API - CRUD Operations
- Create controllers for each entity
- Implement GET, POST, PUT, DELETE endpoints
- Input validation and error handling
- Response DTOs and mappers

## Stage 6: Security & Authentication  _(added — optional track)_
- Add Spring Security (Boot 4 → Spring Security 7): `spring-boot-starter-security` + `spring-boot-starter-security-test`
- `SecurityFilterChain` bean; stateless sessions; BCrypt password hashing
- App account model + `UserDetailsService` (a security principal, distinct from domain `Adopter`/`Caretaker`)
- Authentication via JWT bearer tokens (`spring-boot-starter-oauth2-resource-server`): a login endpoint issues a token, protected endpoints validate it
- Authorization: roles (e.g. `STAFF` vs `ADMIN`), lock down write endpoints, method-level `@PreAuthorize`
- Testing: `@WithMockUser` and security-aware slice tests (401 vs 403 vs 200)

## Stage 7: Advanced Queries & Filtering
- Implement search/filter endpoints (by health status, adoption date, etc.)
- Pagination and sorting
- Specification pattern or custom criteria
- Performance optimization

## Stage 8: Caching
- Add Spring Cache abstraction
- Cache annotations (@Cacheable, @CacheEvict, @CachePut)
- Cache invalidation strategy
- Testing cache behavior

## Stage 9: Kafka Setup
- Docker Compose configuration for Kafka + Zookeeper
- Topic creation (AnimalAdded, AdoptionCompleted, HealthAlert)
- Kafka producer configuration
- Kafka consumer configuration

## Stage 10: Event Producers
- Implement event publishing from services
- Publish events on entity creation/updates (Animal, Adoption)
- Event serialization to JSON
- Error handling in producers

## Stage 11: Event Consumers
- Implement listeners for each event type
- Update database state from events
- Email/notification stubs for alerts
- Async message processing

## Stage 12: Error Handling & Resilience
- Retry logic for failed messages
- Dead Letter Queue (DLQ) for unprocessable events
- Exception handling strategies
- Monitoring and logging

## Stage 13: Integration Testing
- End-to-end test flows (REST API → Database → Kafka → Consumer)
- Testcontainers for PostgreSQL + Kafka
- Mock external dependencies
- Performance and reliability tests

## Stage 14: Documentation & Polish
- API documentation (Swagger/OpenAPI)
- Code cleanup and refactoring
- Final testing and bug fixes
- GitHub README with setup instructions
