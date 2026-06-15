# Project Stages

## Stage 1: Project Setup
- Spring Boot project creation with Maven
- Dependencies: Spring Web, Spring Data JPA, PostgreSQL, Kafka, Lombok, Testcontainers
- Project structure and package organization
- Basic application.yml configuration

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

## Stage 6: Advanced Queries & Filtering
- Implement search/filter endpoints (by health status, adoption date, etc.)
- Pagination and sorting
- Specification pattern or custom criteria
- Performance optimization

## Stage 7: Caching
- Add Spring Cache abstraction
- Cache annotations (@Cacheable, @CacheEvict, @CachePut)
- Cache invalidation strategy
- Testing cache behavior

## Stage 8: Kafka Setup
- Docker Compose configuration for Kafka + Zookeeper
- Topic creation (AnimalAdded, AdoptionCompleted, HealthAlert)
- Kafka producer configuration
- Kafka consumer configuration

## Stage 9: Event Producers
- Implement event publishing from services
- Publish events on entity creation/updates (Animal, Adoption)
- Event serialization to JSON
- Error handling in producers

## Stage 10: Event Consumers
- Implement listeners for each event type
- Update database state from events
- Email/notification stubs for alerts
- Async message processing

## Stage 11: Error Handling & Resilience
- Retry logic for failed messages
- Dead Letter Queue (DLQ) for unprocessable events
- Exception handling strategies
- Monitoring and logging

## Stage 12: Integration Testing
- End-to-end test flows (REST API → Database → Kafka → Consumer)
- Testcontainers for PostgreSQL + Kafka
- Mock external dependencies
- Performance and reliability tests

## Stage 13: Documentation & Polish
- API documentation (Swagger/OpenAPI)
- Code cleanup and refactoring
- Final testing and bug fixes
- GitHub README with setup instructions
