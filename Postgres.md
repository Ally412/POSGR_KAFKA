# PostgreSQL Topics

## Core Concepts
- ACID properties (Atomicity, Consistency, Isolation, Durability)
- Tables and schemas
- Columns and data types (int, varchar, timestamp, json, etc.)
- Constraints (PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK)
- Indexes (B-tree, Hash, BRIN, GiST)

## Relationships
- One-to-One relationships
- One-to-Many relationships
- Many-to-Many relationships
- Foreign keys and referential integrity
- Cascading (DELETE CASCADE, UPDATE CASCADE)

## Queries
- SELECT with WHERE, ORDER BY, GROUP BY, HAVING
- JOINs (INNER, LEFT, RIGHT, FULL OUTER, CROSS)
- Subqueries
- Aggregation functions (COUNT, SUM, AVG, MIN, MAX)
- Window functions
- Common Table Expressions (CTEs)

## Transactions & Isolation
- Transaction basics (BEGIN, COMMIT, ROLLBACK)
- Isolation levels (READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, SERIALIZABLE)
- Locks
- Deadlocks

## Spring Boot Integration
- Spring Data JPA annotations (@Entity, @Column, @JoinColumn, etc.)
- Hibernate ORM basics
- Repository pattern
- Query methods
- JPQL and native queries
- Entity relationships in code

## Database Migrations
- Flyway basics
- Version control for schema changes
- Migration best practices

## Testing
- Embedded PostgreSQL for tests
- Testcontainers
- Test data setup and cleanup
- H2 as alternative for tests

## Performance
- Query optimization
- Index selection
- Execution plans (EXPLAIN)
- Connection pooling (HikariCP)
- Slow query logs
