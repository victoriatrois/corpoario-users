# Users

[Português (Brasil)](README.pt-br.md)

The Users microservice for the Corpoário digital atelier platform, built
with Java 25 and Quarkus.

## Requirements

- JDK 25
- Maven 3.9 or later
- Docker with Docker Compose

## Run in development mode

Create a `.env` file in the project root with a local PostgreSQL password:

```dotenv
DB_PASSWORD=local-password
```

Load the variables into your shell, then start the database and application:

```bash
set -a
. ./.env
set +a

docker compose up -d
mvn quarkus:dev
```

The development PostgreSQL uses port 5434 to avoid conflicting with the
catalogue. If the container already exists, run `docker compose up -d` again
to recreate it with the new port. The volume retains its data and initial
password; changing `DB_PASSWORD` in `.env` does not change an initialized
database's password.

To stop the database without deleting its data, run `docker compose down`.
Hibernate schema generation is disabled; Flyway runs migrations at startup.

The defaults for `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` apply to development
mode only. In production, all three must be provided; the application will not
start without them.

In development mode, Swagger UI is available at
`http://localhost:8081/q/swagger-ui`. Health checks and Prometheus metrics are
available at `/q/health` and `/q/metrics`.

## Customer registration

`POST /customers` creates a customer account from a JSON body with `email` and
`password`:

```json
{"email": "cliente@example.com", "password": "at-least-8-characters"}
```

The email is trimmed and lowercased before validation and persistence, and must
have a dotted domain. The password must have at least 8 characters and at most
72 bytes in UTF-8 (BCrypt ignores anything longer), and is stored only as a
BCrypt hash. The server always assigns the `CUSTOMER` role; any `role`, `status`
or other extra field in the payload is ignored. The response never includes the
password or its hash.

| Status | Meaning |
| --- | --- |
| `201 Created` | Account created; the body has `id`, `email`, `role`, `status` and `createdAt`. |
| `400 Bad Request` | Missing body, malformed JSON, or an invalid email or password. |
| `409 Conflict` | An account with this email already exists, ignoring case and surrounding whitespace. |

Errors use the body `{"status": 409, "error": "...", "message": "..."}`.

## Run the tests

```bash
mvn test
```

Tests run against a disposable PostgreSQL container started automatically by
Quarkus Dev Services, so Docker must be running. They never use the development
database or `DB_URL`. If the Docker socket is not at the default location,
configure it before running tests. For the default Colima profile:

```bash
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

Export these variables in the same terminal before `mvn quarkus:dev` so
continuous tests can also find Docker. For another profile or runtime, use the
endpoint reported by `docker context inspect`.
