# PostgreSQL Topics

**Priority legend:**
- `[CORE]` — must know to build this project. Don't skip.
- `[NICE]` — improves quality/understanding; learn if on schedule.
- `[LATER]` — out of scope for Phase 2. Skip now, revisit in Phase 3 or beyond.

---

## Core Concepts
- `[CORE]` ACID properties (Atomicity, Consistency, Isolation, Durability) — **1.5 hours**
- `[CORE]` Tables and schemas — **1 hour**
- `[CORE]` Columns and data types (int, varchar, timestamp, json, etc.) — **1.5 hours**
- `[CORE]` Constraints (PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK) — **2 hours**
- `[CORE]` Indexes — focus on B-tree only for now; ignore Hash/BRIN/GiST — **2 hours**

## Relationships
- `[CORE]` One-to-One relationships — **2 hours**
- `[CORE]` One-to-Many relationships — **2 hours**
- `[CORE]` Many-to-Many relationships — **2.5 hours**
- `[CORE]` Foreign keys and referential integrity — **1.5 hours**
- `[CORE]` Cascading (DELETE CASCADE, UPDATE CASCADE) — **1 hour**

## Queries
- `[CORE]` SELECT with WHERE, ORDER BY, GROUP BY, HAVING — **2 hours**
- `[CORE]` JOINs (INNER, LEFT, RIGHT, FULL OUTER, CROSS) — **2.5 hours**
- `[CORE]` Aggregation functions (COUNT, SUM, AVG, MIN, MAX) — **1 hour**
- `[NICE]` Subqueries — **1.5 hours**
- `[NICE]` Common Table Expressions (CTEs) — **1.5 hours**
- `[LATER]` Window functions — **2 hours**

## Transactions & Isolation
- `[CORE]` Transaction basics (BEGIN, COMMIT, ROLLBACK) — **1.5 hours**
- `[NICE]` Isolation levels (READ COMMITTED, REPEATABLE READ, SERIALIZABLE) — **2 hours**
- `[NICE]` Locks — **1.5 hours**
- `[LATER]` Deadlocks — **1 hour**

## Spring Boot Integration
- `[CORE]` Spring Data JPA annotations (@Entity, @Column, @JoinColumn, etc.) — **2 hours**
- `[CORE]` Hibernate ORM basics — **4 hours**
- `[CORE]` Repository pattern — **1.5 hours**
- `[CORE]` Query methods — **2 hours**
- `[CORE]` Entity relationships in code — **3 hours**
- `[NICE]` JPQL and native queries — **2.5 hours**

## Database Migrations
- `[CORE]` Flyway basics — **1.5 hours**
- `[CORE]` Version control for schema changes — **1 hour**
- `[NICE]` Migration best practices — **1 hour**

## Testing
- `[CORE]` Testcontainers — **1.5 hours**
- `[CORE]` Test data setup and cleanup — **1.5 hours**
- `[NICE]` Embedded PostgreSQL for tests — **1 hour**
- `[LATER]` H2 as alternative for tests — **0.5 hours**

## Performance
- `[NICE]` Query optimization — **3 hours**
- `[NICE]` Index selection — **2 hours**
- `[NICE]` Execution plans (EXPLAIN) — **1.5 hours**
- `[NICE]` Connection pooling (HikariCP) — **1 hour**
- `[LATER]` Slow query logs — **0.5 hours**

---

## Time Totals

| Priority | Hours |
|----------|-------|
| `[CORE]` (must do) | **~43 hours** |
| `[NICE]` (if on schedule) | **~18.5 hours** |
| `[LATER]` (skip for now) | **~4 hours** |
| **Full list** | **~65.5 hours** |

**Realism note:** These are *study* hours only. Add ~40-50% for debugging, environment setup, and getting things to actually run. Plan around the **CORE ~43h** as your true Phase 2 PostgreSQL target.
