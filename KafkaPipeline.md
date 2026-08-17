# Kafka Pipeline — how the pieces fit

A snapshot of the running system: shelter (producer) → broker → notifier (consumer).

Last updated: 2026-08-17. Stages 9–11 shipped (broker, topics, outbox, idempotent
read model). Stage 12 — Error Handling & Resilience — in progress.
All three declared topics now have a producer and a consumer.

Shelter `main` = `c95c74a` (adoption events PR #10, health alerts PR #11).
Notifier `main` = `113a457` (read model PR #1, CI PR #2, health alerts +
adoptions PR #3, alert digest PR #4).

## The whole path

```
┌─ SHELTER APP ────────────────────────────────────────────────────┐
│                                                                  │
│  Three producers, each writing its outbox row in the SAME        │
│  transaction as its domain write:                                │
│                                                                  │
│  AnimalService.saveAnimal()             @Transactional           │
│       ├── animal row                                             │
│       └── outbox row   "AnimalAdded"                             │
│                                                                  │
│  AnimalCareService.addMedicalRecord()   @Transactional           │
│       ├── medical_record row                                     │
│       └── outbox row   "HealthAlert"   — non-ROUTINE only        │
│                                                                  │
│  AdoptionService.completeAdoption()     @Transactional           │
│       ├── adoption row  (@MapsId: its id IS the animal's id)     │
│       ├── animal.status = ADOPTED                                │
│       └── outbox row   "AdoptionCompleted"                       │
│                                                                  │
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
│   shelter.adoption.completed        [p0][p1][p2]                 │
│   shelter.animal.added.DLT          [p0][p1][p2]  ┐ declared by  │
│   shelter.animal.health-alert.DLT   [p0][p1][p2]  │ the notifier │
│   shelter.adoption.completed.DLT    [p0][p1][p2]  ┘              │
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
│  Three listeners, one per topic. All @Transactional. All guard   │
│  against redelivery with the event-id header as a version.       │
│                                                                  │
│         ├──► animal/AnimalAddedListener                          │
│         │      └── AnimalRepository.apply(...)                   │
│         │           INSERT … ON CONFLICT (animal_id) DO UPDATE   │
│         │           WHERE animal.version < excluded.version      │
│         │           → 0 rows = stale/duplicate, a no-op          │
│         │                                                        │
│         ├──► healthalert/HealthAlertListener                     │
│         │      └── HealthAlertRepository.apply(...)              │
│         │           keyed by medical_record_id                   │
│         │                                                        │
│         ├──► adoption/AdoptionListener   — ONE event, TWO writes │
│         │      ├── AdoptionRepository.apply(...)   new fact      │
│         │      └── AnimalRepository.markAdopted()  existing state│
│         │           UPDATE animal SET status='ADOPTED'           │
│         │           WHERE animal_id=? AND version < ?            │
│         │      both in one transaction, so the notifier cannot   │
│         │      record an adoption yet still call the animal      │
│         │      available. stored=1 + statusChanged=0 is logged   │
│         │      as drift: the animal is unknown or already newer. │
│         │                                                        │
│         └──► DefaultErrorHandler          ✗ failure path         │
│                └── DeadLetterPublishingRecoverer                 │
│                      route = record.topic() + ".DLT" ────────────┼──┐
│                      KafkaTemplate + DelegatingByTypeSerializer  │  │
│                                                                  │  │
│  notifier-postgres :5433  ── own repo, own compose, own volume   │  │
│      animal        (V1)  version-guarded upsert                  │  │
│      health_alert  (V2)  version-guarded upsert                  │  │
│      adoption      (V3)  written once, never revised — the       │  │
│                          shelter refuses a second adoption       │  │
│                                                                  │  │
│  AlertDigestJob   @Scheduled @Transactional                      │  │
│      ├── tryClaimDigest()   advisory lock → one notifier mails   │  │
│      ├── findUnnotified(batchSize)  ◄── health_alert table       │  │
│      ├── mailSender.send(...)   ONE mail for the batch           │  │
│      └── stamp notifiedAt       send first, mark after: a failed │  │
│                                 send rolls back and retries      │  │
└──────────────────────────────────────────────────────────────────┘  │
                     ▲                                                │
                     └────────────────────────────────────────────────┘
```

## Four things the diagram makes visible

**The transaction boundary is the whole point of the outbox.** The domain write
and the outbox row share one commit; the Kafka send happens *outside* it, a
second later, in a different method. You give up "instant" for "never lost."

**The relay is the only producer.** None of the three services imports
anything from Kafka. Everything that reaches the broker went through the
outbox table first, and routing lives in one switch.

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
| writes the AdoptionCompleted outbox row | `shelter/adoption/AdoptionService.java` |
| adoption endpoint | `shelter/adoption/AdoptionController.java` |
| outbox entity / table | `shelter/messaging/OutboxEvent.java`, `db/migration/V3__create_outbox.sql` |
| polls, routes, publishes | `shelter/messaging/OutboxRelay.java` |
| advisory lock query | `shelter/messaging/OutboxRepository.java` |
| topic names | `shelter/messaging/Topics.java`, `notifier/messaging/Topics.java` (duplicated on purpose) |
| topic declarations | `shelter/messaging/KafkaTopicConfig.java` (source topics), `notifier/messaging/KafkaErrorHandlingConfig.java` (dead letter topics) |
| event payloads | `shelter/messaging/AnimalAddedEvent.java`, `HealthAlertEvent.java`, `AdoptionCompletedEvent.java` |
| consumer payloads | `notifier/animal/AnimalAdded.java`, `healthalert/HealthAlertAdded.java`, `adoption/AdoptionCompleted.java` |
| listeners | `notifier/{animal,healthalert,adoption}/*Listener.java` |
| idempotent upserts | `notifier/{animal,healthalert,adoption}/*Repository.java` |
| notifier read models | `notifier/db/migration/V1..V3` |
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

- No test proves a `HealthAlert` or `AdoptionCompleted` row travels through a
  real broker. `OutboxRelayIT` covers that path for `AnimalAdded` only; the
  other two rely on the relay's switch being obviously correct, which is what
  the relay's two earlier bugs also looked like.
- **The shelter's cache is per-replica.** `@Cacheable("animals")` uses an
  in-heap Caffeine cache, so with `replicas: 3` an evict on one pod is
  invisible to the other two. `expireAfterWrite=60s` bounds the staleness; it
  does not remove it. Redis is the real fix.
- `Species`, `Status` and `Urgency` are hand-copied into the notifier. Adding a
  constant on the shelter side breaks the notifier until redeployed — a
  deliberate choice, not an oversight. `@JsonEnumDefaultValue UNKNOWN` would
  soften it.
- `k8s/kafka.yaml` has never been applied — written but unverified. The cluster
  manifests also predate the notifier: nothing deploys it.
- The digest mails on a timer with no delivery record beyond `notified_at`. A
  crash between a successful send and the commit resends next tick — deliberate,
  since a duplicate health alert beats a missed one.
- Deferred: splitting `OutboxRelay` into its own `replicas: 1` deployment, which
  would remove the need for the advisory lock entirely. The same argument now
  applies to `AlertDigestJob`, which uses the same advisory-lock trick.
