# Media Microservices

This directory is an independently executable microservice implementation of the media API. Nginx exposes the public `/api/v1/**` routes; media-service owns video and rendition data, transcoder-service owns processing, and notification-service owns notifications and SSE.

## Build And Run

Requirements: Java 21, Maven 3.9+, Docker Compose, and Docker for the full stack.

Run every command in this section from the repository root; do not change directories between
commands.

```bash
mvn -f backend/media-microservices/pom.xml spotless:apply
mvn -f backend/media-microservices/pom.xml verify
cp backend/media-microservices/.env.example backend/media-microservices/.env
docker compose --env-file backend/media-microservices/.env \
  -f backend/media-microservices/docker-compose.yml config
docker compose --env-file backend/media-microservices/.env \
  -f backend/media-microservices/docker-compose.yml up --build --wait
```

After materializing the benchmark fixture, run the complete public flow against the healthy stack:

```bash
benchmark/scripts/generate-video.sh
backend/media-microservices/scripts/smoke-e2e.sh
```

The smoke script uploads the source, requests `SD_360` and `HD_720`, waits for both outputs, downloads and probes them, checks persisted notifications, and verifies an SSE event through Nginx. It prints the temporary artifact directory containing the HTTP and media evidence.

To stop the stack while retaining data, omit `--volumes` from this command. To fully reset all
microservice databases, RabbitMQ data, and stored media, run from the repository root:

```bash
docker compose --env-file backend/media-microservices/.env \
  -f backend/media-microservices/docker-compose.yml down --volumes --remove-orphans
rm -f backend/media-microservices/.env
```

The gateway listens at `http://localhost:8080` by default. RabbitMQ management listens at `http://localhost:15672`. Internal health and Prometheus endpoints are:

- `http://media-service:8080/api/actuator/{health,prometheus}`
- `http://transcoder-service:8080/actuator/{health,prometheus}`
- `http://notification-service:8080/api/actuator/{health,prometheus}`

All business metrics have stable tags `variant=microservices` and `service=media-service|transcoder-service|notification-service`. Container metrics should use Compose project `media-microservices` and service labels.

## Resource Boundary

The application aggregate is gateway + media-service + transcoder-service + notification-service. Default limits total exactly 2 CPU and 2048 MiB: `0.10/64m`, `0.65/768m`, `1.00/896m`, and `0.25/320m`. Override the four `*_CPUS` and `*_MEMORY` variables as a divided benchmark profile; do not assign the full profile to each service. RabbitMQ and PostgreSQL limits are infrastructure and must be reported separately. `RENDITION_POOL_SIZE` controls only Rabbit consumer concurrency in transcoder-service.

## Data And Delivery Boundaries

PostgreSQL initializes `media_db`/`media_user` and `notification_db`/`notification_user`, revokes public database connect rights, and grants each login only its database. Initialization runs only for a new `postgres_data` volume. Source and rendition files intentionally share `media_data` between media-service and transcoder-service.

Rendition commands are published after the media transaction commits. Same-video creation requests
take a PostgreSQL pessimistic write lock so a concurrent request validates against the first
committed rendition. Transcoder running/outcome events are consumed serially by media-service,
which commits state before publishing canonical terminal notifications. Notification-service
commits a notification before SSE broadcast.

Rabbit listeners use `AUTO` acknowledgement, no retry interceptor, and rejected-message requeueing.
A command is acknowledged after the transcoder listener returns. Processing failures are converted
to a failed event; if running, success, or failure event publication throws, the listener exception
escapes and RabbitMQ can redeliver the command. Metric failures also escape and may cause duplicate
processing. RabbitTemplate publisher confirms are not enabled, so a send that returns normally is
not proof that the broker durably accepted the message.

This prototype therefore does not guarantee unconditional end-to-end at-least-once delivery. It
has no transactional outbox/inbox, comprehensive idempotency, dead-letter workflow, bounded retry,
tracing, or restart recovery. Database commit/publication gaps, fatal conversion failures, poison
message requeue loops, lost unconfirmed publications, duplicate delivery, and work left in
`RUNNING` remain possible.

## Specifications

- `spec/openapi.yaml`: public gateway HTTP/SSE contract.
- `spec/asyncapi.yaml`: RabbitMQ exchanges, queues, routing keys, and payloads.
- `diagrams/*.puml`: editable component, deployment, sequence, and ownership diagrams.
