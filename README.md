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

