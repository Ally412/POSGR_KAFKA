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

## Deployment (Kubernetes)

The app deploys to Kubernetes via manifests in `k8s/` and scripts in `scripts/`.
It's built and tested on **minikube** (local), but the manifests and rollout logic
are cluster-agnostic — only a few minikube-specific glue commands differ in production.

### Local flow (minikube)

```bash
# once per cluster — sealed-secrets controller, ingress controller,
# staging/production namespaces, sealed DB creds, LoadBalancer
scripts/bootstrap.sh

# ship a version: build → load into minikube → staging deploy + smoke test
# → approval gate → production canary
docker build -t shelter:v2 .
minikube image load shelter:v2
scripts/release.sh v2

# to reach a LoadBalancer Service from the host (assigns its EXTERNAL-IP):
minikube tunnel        # long-running, needs sudo — run in its own terminal
```

Canary rollout (`scripts/canary.sh`) runs two Deployments (`shelter` = stable,
`shelter-canary` = new) behind two nginx Ingresses on the same host; it ramps the
`canary-weight` annotation (10→20→50→100 %), health-checks each step, and rolls back
on failure or promotes on success.

### Local (minikube) vs Production

Everything in `k8s/` and the rollout logic transfers unchanged. Only these local
shortcuts swap out on a real cluster:

| Concern | Local (minikube) | Production (cloud) |
|---|---|---|
| Ingress controller | `minikube addons enable ingress` | `helm upgrade --install ingress-nginx ingress-nginx --repo https://kubernetes.github.io/ingress-nginx -n ingress-nginx --create-namespace` |
| Image delivery | `minikube image load shelter:vN` | `docker push` to a registry (e.g. `ghcr.io/ally412/shelter:vN`); manifests use the registry path |
| External IP | `minikube tunnel` (fakes it) | cloud provisions a public IP automatically |
| `k8s/lb.yaml` | needed (addon controller is `NodePort`) | **not needed** — the helm chart's controller Service is already `type: LoadBalancer` |
| Ingress host | `shelter.local` + `Host:` header / `/etc/hosts` | real DNS record → the LoadBalancer's public IP |

So `k8s/lb.yaml` and `minikube tunnel` are **minikube stand-ins** for what a cloud
cluster provides out of the box. Swap the ~5 commands above (or parameterize them)
and the same manifests + scripts run in production.
