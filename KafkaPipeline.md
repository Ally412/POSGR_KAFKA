# Kafka Pipeline — how the pieces fit

A snapshot of the running system: shelter (producer) → broker → notifier (consumer).

Last updated: 2026-08-12. Stages 9–11 shipped (broker, topics, outbox, idempotent
read model). Stage 12 — Error Handling & Resilience — in progress.
Shelter `main` = `056f822`. Notifier on branch `ci`, one commit ahead of `a636483`.

## The whole path

```
┌─ SHELTER APP ────────────────────────────────────────────────────┐
│                                                                  │
│  AnimalController         AnimalCareService                      │
│  POST /animals            (medical record → health alert)        │
│       │                        │                                 │
│       ▼                        ▼                                 │
│  saveAnimal()             raiseHealthAlert()                     │
│       │  @Transactional        │  @Transactional                 │
│       ├── animal row           ├── medical_record row            │
│       └── outbox row           └── outbox row                    │
│           eventType=               eventType=                    │
│           "AnimalAdded"            "HealthAlert"                 │
│                                                                  │
│           └──────────┬─────────────┘                             │
│                      ▼                                           │
│              outbox table   (payload = JSON string, jsonb)       │
│                      │                                           │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┼ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─       │
│                      ▼                                           │
│  OutboxRelay   @Scheduled(1s) @Transactional                     │
│       ├── tryClaimRelay()    advisory lock → ONE replica relays  │
│       ├── findUnpublished(100)                                   │
│       ├── switch eventType → topic  (throws if unmapped)         │
│       ├── ProducerRecord(topic, key=aggregateId,                 │
│       │                  value=payload, header event-id)         │
│       ├── KafkaTemplate<String,String>  ── StringSerializer      │
│       └── allOf(futures).join()   INSIDE the tx → batch rolls    │
│                    │              back if any send fails         │
│                    ▼                                             │
│              KafkaProducer  ── buffer + sender thread            │
└────────────────────┼─────────────────────────────────────────────┘
                     │  bytes
                     ▼
┌─ KAFKA BROKER  (single node, KRaft: broker + controller) ────────┐
│                                                                  │
│   shelter.animal.added              [p0][p1][p2]                 │
│   shelter.animal.health-alert       [p0][p1][p2]                 │
│   shelter.adoption.completed        [p0][p1][p2]  declared,unused│
│   shelter.animal.added.DLT          [p0][p1][p2]  ┐ declared by  │
│   shelter.animal.health-alert.DLT   [p0][p1][p2]  ┘ the notifier │
│                                                                  │
│   stores opaque bytes + assigns offsets. interprets nothing.     │
└────────────────────┼─────────────────────────────────────────────┘
                     │  bytes
                     ▼
┌─ NOTIFIER APP ───────────────────────────────────────────────────┐
│                                                                  │
│  KafkaConsumer.poll()          group-id = notifier               │
│         │                                                        │
│         ├── ErrorHandlingDeserializer   ── try/catch wrapper     │
│         │        └── JacksonJsonDeserializer                     │
│         │               value.default.type per listener          │
│         ▼                                                        │
│    ConsumerRecord  ── built HERE, in this JVM                    │
│         │                                                        │
│    listener container  (owns the poll loop + offset commits)     │
│         │                                                        │
│         ├──► AnimalAddedListener      @Transactional             │
│         │      └── AnimalRepository.apply(...)                   │
│         │           INSERT … ON CONFLICT (animal_id) DO UPDATE   │
│         │           WHERE animal.version < excluded.version      │
│         │           version = event-id header                    │
│         │           → 0 rows = stale/duplicate, a no-op          │
│         │                                                        │
│         ├──► HealthAlertListener      ⚠ EMPTY BODY (stub)        │
│         │                                                        │
│         └──► DefaultErrorHandler          ✗ failure path         │
│                └── DeadLetterPublishingRecoverer                 │
│                      route = record.topic() + ".DLT" ────────────┼──┐
│                      KafkaTemplate + DelegatingByTypeSerializer  │  │
│                                                                  │  │
│  notifier-postgres :5433  ── own repo, own compose, own volume   │  │
│      animal        (read model, V1)                              │  │
│      health_alert  (V2 — table exists, nothing writes to it yet) │  │
└──────────────────────────────────────────────────────────────────┘  │
                     ▲                                                │
                     └────────────────────────────────────────────────┘
```

## Four things the diagram makes visible

**The transaction boundary is the whole point of the outbox.** The domain write
and the outbox row share one commit; the Kafka send happens *outside* it, a
second later, in a different method. You give up "instant" for "never lost."

**The relay is the only producer.** Neither `AnimalService` nor
`AnimalCareService` imports anything from Kafka. Everything that reaches the
broker went through the outbox table first, and routing lives in one switch.

**Nothing load-balances a timer.** `k8s/app.yaml` runs `replicas: 3`, so all
three would relay the same rows. The Postgres advisory lock is what makes
exactly one of them do it.

**The notifier is also a producer.** Its dead letter path needs its own
`KafkaTemplate`, which is why `KafkaErrorHandlingConfig` exists in an app you
would otherwise describe as consume-only.

## Where each piece lives

| Piece | File |
|---|---|
| writes the AnimalAdded outbox row | `shelter/animal/AnimalService.java` |
| writes the HealthAlert outbox row | `shelter/care/AnimalCareService.java` |
| outbox entity / table | `shelter/messaging/OutboxEvent.java`, `db/migration/V3__create_outbox.sql` |
| polls, routes, publishes | `shelter/messaging/OutboxRelay.java` |
| advisory lock query | `shelter/messaging/OutboxRepository.java` |
| topic names | `shelter/messaging/Topics.java`, `notifier/Topics.java` (duplicated on purpose) |
| topic declarations | `shelter/messaging/KafkaTopicConfig.java` (source topics), `notifier/KafkaErrorHandlingConfig.java` (dead letter topics) |
| event payloads | `shelter/messaging/AnimalAddedEvent.java`, `HealthAlertEvent.java` |
| consumer payloads | `notifier/AnimalAdded.java`, `HealthAlertAdded.java` |
| idempotent upsert | `notifier/AnimalRepository.java` |
| broker, local | `docker-compose.yml` (shelter repo — shared infrastructure) |
| broker, cluster | `k8s/kafka.yaml` |
| notifier database | `docker-compose.yml` in the **notifier** repo, port 5433 |

## Facts worth not re-deriving

- The broker stores **opaque bytes**. It never validates content, so every
  format disagreement surfaces at the consumer, never at publish time.
- `ConsumerRecord` is constructed in the **consumer's JVM**, not sent by the
  broker. Deserialization happens inside `poll()`, which is why an unreadable
  message ("poison pill") fails before the listener runs.
- `ErrorHandlingDeserializer` runs on **every** message, not just broken ones.
  It is a try/catch wrapper delegating to Jackson.
- `.DLT` is a Spring Kafka convention, not a Kafka feature — the dead letter
  topic is an ordinary topic. It needs its own `NewTopic` bean, or auto-creation
  gives it one partition and same-partition routing breaks.
- The message **key** is the aggregate id (repeats across events for one
  animal). The **`event-id` header** is the outbox row id — unique per event,
  which is what makes it usable as the version in the upsert guard.
- Delivery is **at-least-once by construction**: a mid-batch relay failure rolls
  back `publishedAt` for rows already sent. The consumer's version guard is what
  makes redelivery harmless.

## Known gaps

- `HealthAlertListener` has an empty body — the `health_alert` table (V2) exists
  but nothing populates it. Next obvious piece of work.
- `shelter.adoption.completed` is declared but nothing publishes or consumes it.
- `Species` and `Status` are hand-copied into the notifier. Adding a constant on
  the shelter side breaks the notifier until redeployed — a deliberate choice,
  not an oversight. `@JsonEnumDefaultValue UNKNOWN` would soften it.
- `k8s/kafka.yaml` has never been applied — written but unverified.
- Notifier CI exists only on its `ci` branch, not yet merged.
- Deferred: splitting `OutboxRelay` into its own `replicas: 1` deployment, which
  would remove the need for the advisory lock entirely.
