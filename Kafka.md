# Kafka Topics

**Priority legend:**
- `[CORE]` — must know to build this project (Kafka basics). Don't skip.
- `[NICE]` — improves quality/understanding; learn if on schedule.
- `[LATER]` — intermediate/advanced. Out of scope for "Kafka basics". Skip now.

---

## Core Concepts
- `[CORE]` Brokers and clusters — **1.5 hours**
- `[CORE]` Topics and partitions — **2 hours**
- `[CORE]` Log-based architecture — **1.5 hours**
- `[NICE]` Replicas and leadership — **1.5 hours**
- `[NICE]` Retention policies (time-based, size-based) — **1 hour**

## Producers
- `[CORE]` Sending messages to topics — **2 hours**
- `[CORE]` Serialization and deserialization — **1.5 hours**
- `[CORE]` Acknowledgments (acks: 0, 1, all) — **1 hour**
- `[CORE]` Error handling and retries — **2 hours**
- `[NICE]` Partitioning strategy — **1.5 hours**
- `[NICE]` Idempotent producers — **1.5 hours**
- `[LATER]` Batching and compression — **1 hour**

## Consumers
- `[CORE]` Consumer groups — **2 hours**
- `[CORE]` Offsets and offset management (auto-commit, manual) — **2 hours**
- `[CORE]` Polling and message processing — **1.5 hours**
- `[CORE]` Consumer configurations — **1 hour**
- `[NICE]` Rebalancing — **1.5 hours**
- `[NICE]` Lag monitoring — **1 hour**

## Message Format
- `[CORE]` Keys and values — **0.5 hours**
- `[NICE]` Headers — **0.5 hours**
- `[LATER]` Timestamps — **0.5 hours**
- `[LATER]` Serialization formats (Avro, Protobuf) — **2 hours**
- `[LATER]` Schema compatibility — **1 hour**

## Event-Driven Architecture
- `[CORE]` Domain events — **1 hour**
- `[CORE]` Event publishing and subscribing — **1.5 hours**
- `[LATER]` Event sourcing basics — **2 hours**
- `[LATER]` Saga pattern (distributed transactions) — **2.5 hours**

## Error Handling
- `[CORE]` Dead Letter Queues (DLQ) — **1.5 hours**
- `[CORE]` Retry policies — **1.5 hours**
- `[CORE]` Exception handling in consumers — **1 hour**
- `[NICE]` Monitoring and alerting — **1.5 hours**

## Spring Kafka Integration
> **Boot 4:** dependency is `spring-boot-starter-kafka` (not the bare `spring-kafka`).
> JSON (de)serialization runs on **Jackson 3** (`tools.jackson.*`) — tutorials showing
> `com.fasterxml.jackson.*` imports for `JsonSerializer`/`JsonDeserializer` are Boot 3 era; adapt them.
- `[CORE]` @KafkaListener annotation — **1 hour**
- `[CORE]` KafkaTemplate for sending — **1 hour**
- `[CORE]` JSON serialization with Jackson 3 (`JsonSerializer`/`JsonDeserializer`, trusted packages) — **1 hour**
- `[CORE]` Error handling strategies — **1.5 hours**
- `[NICE]` Message converters — **1 hour**
- `[NICE]` Transactions in Kafka — **1.5 hours**

## Docker & Local Setup
- `[CORE]` Docker Compose for Kafka + Zookeeper — **1 hour**
- `[CORE]` Kafka CLI tools (topics, console producers/consumers) — **1 hour**
- `[NICE]` Monitoring tools (Kafdrop, Confluent Control Center) — **1 hour**

## Testing
- `[CORE]` Testcontainers with Kafka — **1 hour**
- `[NICE]` Embedded Kafka for tests — **0.5 hours**
- `[NICE]` Mock producers and consumers — **1 hour**

---

## Time Totals

| Priority | Hours |
|----------|-------|
| `[CORE]` (must do) | **~32.5 hours** |
| `[NICE]` (if on schedule) | **~15 hours** |
| `[LATER]` (skip for now) | **~9 hours** |
| **Full list** | **~56.5 hours** |

**Realism note:** These are *study* hours only. Add ~40-50% for Docker/Kafka setup pain and debugging consumers. Since you chose **Kafka = basics**, the `[CORE] ~31.5h` is your real target — the `[LATER]` items (Avro, Saga, event sourcing) are genuinely advanced and not needed to ship this project.
