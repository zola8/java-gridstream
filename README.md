# java-gridstream

## Docker services

Let's start the services first. The command `docker compose up -d` brings them all up.

Daily commands:

| Command                                 | What it does                                                                |
|-----------------------------------------|-----------------------------------------------------------------------------|
| `docker compose up -d`                  | Start **all** services, in the background (`-d` = detached)                 |
| `docker compose up -d postgres`         | Start **only** the `postgres` service                                       |
| `docker compose ps`                     | Show status of all services in this project                                 |
| `docker compose ps postgres`            | Status of one service                                                       |
| `docker compose logs -f postgres`       | Tail Postgres logs (`-f` = follow)                                          |
| `docker compose logs -f kafka postgres` | Tail multiple at once                                                       |
| `docker compose stop postgres`          | Stop a service (container stays defined, can be started again)              |
| `docker compose start postgres`         | Start a stopped service                                                     |
| `docker compose restart kafka`          | Restart one service                                                         |
| `docker compose down`                   | Stop + remove all containers + network. **Volumes survive** — data is safe. |
| `docker compose down -v`                | Same as above **plus delete volumes** — full reset, all data gone.          |

## The complete data flow (MVP)

GridStream is a miniature version of any live-tracking system.

```
[AvatarSimulator]
       │ publishes PositionUpdate
       ▼
[grid.positions topic]  ← Kafka
       │
       ├──────────────────────┬──────────────────────┐
       ▼                      ▼                      ▼
[PositionPersistConsumer] [AsciiRenderer]         (future consumers)
 group: gridstream-persist  group: gridstream-renderer
       │                      │
       ▼                      ├─→ log.info(ASCII)     ← console output
  Postgres                    └─→ SnapshotService.push() ─→ SseEmitter ─→ Browser
 (avatar_state,                                                   │
  position_history)                                               ▼
                                                            [index.html]
                                                           table + JS renders
                                                           colored avatar cells
```

Each consumer group gets its own copy of every event. Each maintains its own offset. They start, stop, restart, and
scale independently. This is the whole point of Kafka's pub/sub model — and you can now see it working: Postgres fills
up while the ASCII grid updates in parallel.

1. **Producer ↔ consumer decoupling**

   AvatarSimulator publishes events and knows nothing about what consumes them. It doesn't import AvatarStateRepository
   or AsciiRenderer. Adding a new consumer requires zero changes to the producer.

2. **Multiple consumers, same event stream**

   Both PositionPersistConsumer and AsciiRenderer receive every PositionUpdate. Different purposes (durability vs.
   display), different storage (Postgres vs. in-memory), different lifecycles. They don't know about each other and
   don't need to.

3. **Independent consumer groups = independent offsets**

   Each group has its own cursor in the topic. If the renderer crashes and restarts 30 seconds later, it resumes from
   its last committed offset — it doesn't lose events. The persist consumer's offset is unaffected. They don't block
   each other.

4. **Async by design**

   kafkaTemplate.send() returns immediately. The producer doesn't wait for the consumer to process. If the persist
   consumer is slow (e.g., Postgres is under load), events buffer in Kafka's log. No backpressure propagates to the
   simulator.

5. **Durability & replayability**

   Events persist in Kafka's log (default 7-day retention). If a consumer is down, events aren't lost — they wait. When
   the consumer comes back, it catches up. This is why auto-offset-reset: earliest matters: a new consumer group can
   replay history from the start. (Postgres got every event from tick 1 on its first startup, remember.)

6. **Extensibility without modification**

   Want to add:
    - An analytics consumer that computes average avatar speed? New @KafkaListener, new group gridstream-analytics. Zero
      changes to existing code.
    - An alert consumer that fires when two avatars collide? New @KafkaListener, new group gridstream-alerts. Zero
      changes.
