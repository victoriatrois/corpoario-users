# Users

[Português (Brasil)](README.pt-br.md)

Initial Maven/Java 25 and Quarkus project for the users microservice of a digital atelier platform. It contains dependencies and configuration only: no entities, business endpoints, functional authentication, business rules, UI, email delivery, or verification/recovery flows.

## Stack

The Quarkus BOM manages REST with Jackson JSON, Hibernate Validator, Hibernate ORM with Panache, PostgreSQL JDBC, Flyway, SmallRye JWT, Elytron Security Common (including `BcryptUtil`), SmallRye OpenAPI, SmallRye Health, and Micrometer's Prometheus registry.

## Run

Requirements: JDK 25, Maven 3.9+ and an accessible PostgreSQL database. Set `JAVA_HOME` to the JDK 25 installation. In Bash, supply the connection settings through environment variables, without committing them:

```sh
export DB_URL='jdbc:postgresql://<host>:<port>/<database>'
export DB_USERNAME='<database-user>'
read -rs QUARKUS_DATASOURCE_PASSWORD; export QUARKUS_DATASOURCE_PASSWORD; echo
mvn quarkus:dev
```

Enter the database password at the prompt. Hibernate schema generation is disabled; Flyway runs migrations at startup. Add future migrations under `src/main/resources/db/migration` using Flyway naming conventions. No migration or schema is provided yet.

To build and run the packaged application:

```sh
mvn verify
java -jar target/quarkus-app/quarkus-run.jar
```

The same environment variables are required when running the packaged application. The default operational endpoints are `/q/openapi`, `/q/health`, and `/q/metrics`. Protect operational endpoints appropriately at the deployment boundary; they are not secured by this starter. JWT verification keys, issuer, authorization policies, and token issuance must be configured and implemented before authentication can be used.

## Requirements and invariants for future implementation

- Visitors without an account may publicly view the catalog and artist's profile.
- Registered customers may make purchases and view only their own orders.
- The artist may manage artwork, profile, and orders.
- Public registration creates customer accounts only. The service assigns the role; it must not accept or set the role from the request body.
- The artist account must only be provisioned through a controlled administrative flow, never through public registration.
- Email addresses must be unique.
- Passwords must be stored only as strong hashes, never as plaintext. Use a password-specific algorithm such as BCrypt with an appropriate work factor; do not log or return passwords.
- Email verification and password recovery must be considered in the future design; neither flow is implemented here.
