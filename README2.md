# GridStream

And here is the AI summary.

A real-time grid simulator demonstrating **event-driven architecture** with Spring Boot 4, Kafka, and Postgres.

Avatars move on a 10×10 grid. Each movement is published as a `PositionUpdate` event to Kafka. Multiple independent
consumers process the same event stream for different purposes — persistence (Postgres), console display (ASCII), and
browser display (SSE).

The grid is a friendly skin over the canonical event-driven pattern used by Uber, food delivery, IoT telemetry, and
multiplayer games: many things move, many systems care about the movement, for different reasons, simultaneously.

---

## Architecture

```
                           [AvatarSimulator]
                                  │
                                  │ @Scheduled(tick-ms)
                                  │ publishes PositionUpdate(avatarId, x, y, tick)
                                  ▼
                     ┌──────────────────────────────┐
                     │   Kafka topic: grid.positions │
                     │   (keyed by avatarId)         │
                     └──────────────────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            ▼                     ▼                     ▼
   consumer group        consumer group          (future groups)
  gridstream-persist    gridstream-renderer      gridstream-analytics
            │                     │               gridstream-alerts
            │                     │                ...
            ▼                     ▼
  PositionPersistConsumer    AsciiRenderer
            │                     │
            │ @Transactional      ├─→ log.info(ASCII grid)     ──▶ Console
            ▼                     └─→ SnapshotService.push()
       ┌────────┐                            │
       │Postgres│                       SseEmitter
       └────────┘                            │
        │   │                                ▼
        │   │                           ┌─────────┐
        │   └─ position_history         │ Browser  │
        │      (append-only log)        │ (SSE)    │
        └─ avatar_state                 └─────────┘
           (upsert projection)            index.html
                                         colored grid
```

**Three things happen on every tick, independently:**

1. `AvatarSimulator` moves 3 avatars, publishes 3 events to Kafka
2. `PositionPersistConsumer` upserts `avatar_state` + appends `position_history` in Postgres
3. `AsciiRenderer` updates in-memory grid → prints ASCII to console + pushes snapshot to browsers via SSE

---

## Components

### Producer

| Component         | File                            | Role                                                                                                                                         |
|-------------------|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `AvatarSimulator` | `producer/AvatarSimulator.java` | `@Scheduled` task. Spawns N avatars at random positions, moves each in a random direction every `tick-ms`, publishes `PositionUpdate` events |

### Consumers (each in its own consumer group)

| Component                 | File                                    | Group                 | Role                                                                                                                                          |
|---------------------------|-----------------------------------------|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `PositionPersistConsumer` | `consumer/PositionPersistConsumer.java` | `gridstream-persist`  | `@KafkaListener` + `@Transactional`. Upserts `avatar_state` (current position), appends `position_history` (audit log)                        |
| `AsciiRenderer`           | `consumer/AsciiRenderer.java`           | `gridstream-renderer` | `@KafkaListener` maintaining in-memory grid state. `@Scheduled render()` prints ASCII to console + pushes `GridSnapshot` to `SnapshotService` |

### Display layer

| Component            | File                                 | Role                                                                                                                         |
|----------------------|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `SnapshotService`    | `service/SnapshotService.java`       | Manages `SseEmitter` connections in a `CopyOnWriteArrayList`. `register()` for new clients, `push(snapshot)` fans out to all |
| `GridViewController` | `controller/GridViewController.java` | `@RestController`. `GET /stream` returns an `SseEmitter`. Static `index.html` served at `/` by Spring Boot                   |
| `index.html`         | `resources/static/index.html`        | Vanilla HTML + ~30 lines JS. `EventSource('/stream')`, listens for `snapshot` events, renders colored 10×10 `<table>`        |

### Domain model

| Type                         | File                                                | Role                                                              |
|------------------------------|-----------------------------------------------------|-------------------------------------------------------------------|
| `PositionUpdate`             | `domain/PositionUpdate.java`                        | Record `(avatarId, x, y, tick)` — the Kafka message value         |
| `GridProperties`             | `domain/GridProperties.java`                        | `@ConfigurationProperties` record `(rows, cols)`                  |
| `SimulationProperties`       | `domain/SimulationProperties.java`                  | `@ConfigurationProperties` record `(avatarCount, tickMs)`         |
| `Avatar`                     | `domain/Avatar.java`                                | Mutable class `(id, x, y)` with `move(Direction, GridProperties)` |
| `Direction`                  | `domain/Direction.java`                             | Enum `UP/DOWN/LEFT/RIGHT` with `(dx, dy)` deltas                  |
| `AvatarState`                | `domain/AvatarState.java`                           | JPA `@Entity` mapping `avatar_state` table (upsert projection)    |
| `PositionHistoryEntry`       | `domain/PositionHistoryEntry.java`                  | JPA `@Entity` mapping `position_history` table (append-only log)  |
| `AvatarPos` / `GridSnapshot` | `domain/AvatarPos.java`, `domain/GridSnapshot.java` | SSE DTOs for browser snapshots                                    |

### Config

| Component             | File                              | Role                                                                                                                                                                                                                      |
|-----------------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `KafkaTopicsConfig`   | `config/KafkaTopicsConfig.java`   | `NewTopic` bean for `grid.positions` (1 partition, replication factor 1)                                                                                                                                                  |
| `KafkaProducerConfig` | `config/KafkaProducerConfig.java` | `ProducerFactory` + `KafkaTemplate` beans. `StringSerializer` (key) + `JacksonJsonSerializer` (value, Jackson 3.x)                                                                                                        |
| `KafkaConsumerConfig` | `config/KafkaConsumerConfig.java` | `ConsumerFactory` + `ConcurrentKafkaListenerContainerFactory` beans. `StringDeserializer` (key) + `JacksonJsonDeserializer<PositionUpdate>` (value). Default group `gridstream-persist`, overridable per `@KafkaListener` |

### Repositories

| Interface                   | File                                    | Entity                                      |
|-----------------------------|-----------------------------------------|---------------------------------------------|
| `AvatarStateRepository`     | `domain/AvatarStateRepository.java`     | `JpaRepository<AvatarState, String>`        |
| `PositionHistoryRepository` | `domain/PositionHistoryRepository.java` | `JpaRepository<PositionHistoryEntry, Long>` |

---

## Quick start

### Prerequisites

- **Docker** + Docker Compose
- **JDK 21** (or later)
- **Maven 3.8+**

### Run

```bash
# 1. Start infrastructure (Postgres 17 + Kafka 4.4 in KRaft mode)
docker compose up -d

# 2. Verify both services are healthy
docker compose ps
# gridstream-postgres: Up (healthy)
# gridstream-kafka: Up

# 3. Build and run the Spring Boot app
mvn clean spring-boot:run

# 4. Open the browser
#    http://localhost:8080/
```

### Verify

```bash
# Current avatar positions (3 rows, positions updating every 2s)
docker exec -it gridstream-postgres psql -U gridstream -d gridstream -c "SELECT * FROM avatar_state"

# History grows over time
docker exec -it gridstream-postgres psql -U gridstream -d gridstream -c "SELECT COUNT(*) FROM position_history"

# Kafka topic exists and has messages
docker exec -it gridstream-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec -it gridstream-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic grid.positions --from-beginning --property print.key=true
```

See [`docs/postgres.md`](docs/postgres.md) and [`docs/kafka.md`](docs/kafka.md) for detailed infrastructure setup.

---

## Configuration

`src/main/resources/application.yml`:

```yaml
gridstream:
  grid:
    rows: 10           # grid dimensions
    cols: 10
  simulation:
    avatar-count: 3    # number of avatars (1-9; >9 needs marker scheme change)
    tick-ms: 2000      # simulation + render cadence (ms)
```

**Tunables:**

- `rows` / `cols` — grid dimensions (browser and ASCII render adapt)
- `avatar-count` — number of avatars. Markers use last digit of avatar ID (`avatar-1` → `1`)
- `tick-ms` — controls producer publish rate, consumer processing rate, ASCII render rate, and SSE push rate. All
  synchronized to this single value.

**Other settings (rarely changed):**

- `spring.kafka.bootstrap-servers` — Kafka broker address (default `localhost:9092`)
- `spring.datasource.*` — Postgres connection (default `localhost:5432/gridstream`)
- `logging.level.org.apache.kafka` — set to `WARN` to suppress Kafka client config dumps

---

## Architectural patterns demonstrated

### 1. Event-driven decoupling

`AvatarSimulator` publishes facts to Kafka and imports nothing from consumers. Adding a new consumer (e.g., analytics,
alerts) is purely additive — no existing file is touched. This is the Open-Closed Principle applied at the architecture
level.

### 2. Multi-consumer (pub/sub)

Same topic, multiple consumer groups, each receives every event independently:

- `gridstream-persist` → Postgres (durable)
- `gridstream-renderer` → console + browser (ephemeral)
- *(future: `gridstream-analytics`, `gridstream-alerts`, ...)*

Each group has its own offset, lifecycle, and scaling. If one consumer crashes, the other is unaffected.

### 3. CQRS / event sourcing

- `position_history` table = **append-only event log** (source of truth — "what happened at tick N")
- `avatar_state` table = **materialized projection** (query-optimized — "where is avatar-1 right now")

`avatar_state` is derived from events and can be rebuilt by replaying `position_history`. The persist consumer maintains
both in a single `@Transactional` method.

### 4. SSE for live display

The browser can't speak Kafka protocol. Instead, it consumes from `GET /stream` (an SSE endpoint), which is fed by
`AsciiRenderer` (which IS a Kafka consumer). Layering: Kafka → consumer → in-memory state → SSE → browser.

### 5. Async fire-and-forget producer

`kafkaTemplate.send()` returns immediately. The producer doesn't wait for consumers. If a consumer is slow, events
buffer in Kafka's log. No backpressure on the producer.

### 6. Independent scaling

At scale (10,000 avatars instead of 3):

- Partition the topic by `avatarId` (already the Kafka key)
- Add consumer instances to a group → Kafka rebalances partitions across them
- No code changes, no architecture changes — just more instances

---

## Tech stack

| Layer         | Technology                              | Version                                   |
|---------------|-----------------------------------------|-------------------------------------------|
| Language      | Java                                    | 21                                        |
| Framework     | Spring Boot                             | 4.2.0                                     |
| Web layer     | Spring WebMVC + embedded Tomcat         | (via `spring-boot-starter-webmvc`)        |
| Messaging     | Apache Kafka (KRaft mode, no Zookeeper) | 4.4.0                                     |
| Database      | PostgreSQL                              | 17 (alpine)                               |
| ORM           | Hibernate via Spring Data JPA           | (managed by Spring Boot 4.2)              |
| Serialization | Jackson                                 | 3.x (via `spring-boot-starter-jackson`)   |
| Build         | Maven                                   | (managed by `spring-boot-starter-parent`) |

---

## Project structure

```
gridstream/
├── docker-compose.yml              # Postgres 17 + Kafka 4.4 (KRaft)
├── pom.xml                         # Maven build (Spring Boot 4.2.0, Java 21)
├── README.md                       # this file
├── docs/
│   ├── postgres.md                 # Postgres setup guide
│   └── kafka.md                    # Kafka setup guide
└── src/main/
    ├── java/com/example/gridstream/
    │   ├── GridStreamApplication.java          # entry point (@SpringBootApplication)
    │   ├── config/
    │   │   ├── KafkaTopicsConfig.java          # NewTopic bean
    │   │   ├── KafkaProducerConfig.java        # ProducerFactory + KafkaTemplate
    │   │   └── KafkaConsumerConfig.java        # ConsumerFactory + ContainerFactory
    │   ├── domain/
    │   │   ├── PositionUpdate.java             # Kafka event record
    │   │   ├── GridProperties.java             # @ConfigurationProperties (rows, cols)
    │   │   ├── SimulationProperties.java       # @ConfigurationProperties (avatarCount, tickMs)
    │   │   ├── Avatar.java                     # mutable avatar state
    │   │   ├── Direction.java                  # movement enum
    │   │   ├── AvatarState.java                # JPA entity (upsert projection)
    │   │   ├── PositionHistoryEntry.java       # JPA entity (append-only log)
    │   │   ├── AvatarStateRepository.java      # Spring Data JPA repo
    │   │   ├── PositionHistoryRepository.java  # Spring Data JPA repo
    │   │   ├── AvatarPos.java                  # SSE DTO
    │   │   └── GridSnapshot.java               # SSE DTO
    │   ├── producer/
    │   │   └── AvatarSimulator.java            # @Scheduled publisher
    │   ├── consumer/
    │   │   ├── PositionPersistConsumer.java    # @KafkaListener → Postgres
    │   │   └── AsciiRenderer.java             # @KafkaListener → console + SSE
    │   ├── service/
    │   │   └── SnapshotService.java           # SseEmitter registry + fan-out
    │   └── controller/
    │       └── GridViewController.java        # GET /stream → SseEmitter
    └── resources/
        ├── application.yml                    # app config
        ├── schema.sql                         # Postgres DDL (CREATE TABLE IF NOT EXISTS)
        └── static/
            └── index.html                     # browser UI (vanilla HTML+JS)
```

---

## Build history (incremental)

This project was built step-by-step to demonstrate how an event-driven architecture comes together:

| Step  | Goal             | Key addition                                                     |
|-------|------------------|------------------------------------------------------------------|
| **0** | Skeleton         | Spring Boot 4.2 + Java 21 + docker-compose (Postgres + Kafka)    |
| **1** | Domain + topic   | `PositionUpdate` record, `GridProperties`, `NewTopic` bean       |
| **2** | Producer         | `AvatarSimulator` (`@Scheduled`) publishes events to Kafka       |
| **3** | Persist consumer | JPA entities + repositories, `@KafkaListener` writes to Postgres |
| **4** | ASCII renderer   | Second consumer (different group), in-memory grid, console ASCII |
| **5** | Browser display  | `SnapshotService` + SSE + `index.html` (MVP complete)            |
| **6** | Documentation    | This README                                                      |
