# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**cevi-int** is a Quarkus web application for the Cevi international platform. It manages international events, voluntary services, and a contact form. The UI is in German (Swiss German locale). The application uses SQLite as its database.

## Commands

### Development
```shell
./mvnw compile quarkus:dev
```
Starts the app with hot reload at http://localhost:8080. Dev UI at http://localhost:8080/q/dev/. On first start with an empty DB, demo data is inserted automatically (including a default `admin`/`admin` user via `DemoData.java`).

### Run tests
```shell
./mvnw test
```

### Run a single test class
```shell
./mvnw test -Dtest=EventUpcomingTest
```

### Build über-jar
```shell
./mvnw package -Dquarkus.package.type=uber-jar
```

### Build Docker image
```shell
./mvnw package
sudo DOCKER_BUILDKIT=1 docker build -f src/main/docker/Dockerfile.jvm -t quarkus/international-jvm .
```

### Run via Docker Compose (last published image)
```shell
sudo docker-compose up
```

## Architecture

The app follows the Quarkus resource/template pattern:

- **Resources** (`src/main/java/tools/cevi/`) — JAX-RS endpoints that render Qute templates. Each resource has a nested `@CheckedTemplate` inner class (`Templates`) that maps method names to template files.
- **Entities** — Panache entities (`PanacheEntityBase`) with public fields and static query methods. No separate repository layer.
- **Templates** (`src/main/resources/templates/`) — Qute HTML templates organized by resource class name. `base.qute.html` is the shared layout. Tags in `templates/tags/` are reusable template fragments.
- **DB Migrations** (`src/main/resources/db/migration/`) — Flyway SQL scripts, versioned `V<major>.<minor>.<patch>__<Description>.sql`. Schema uses SQLite.

### Packages
| Package | Contents |
|---|---|
| `tools.cevi.event` | `Event` entity + `EventResource` (CRUD for international events at `/anlaesse`) |
| `tools.cevi.voluntary` | `VoluntaryService` entity + `VoluntaryResource` (CRUD at `/voluntary`) |
| `tools.cevi.contact` | `ContactFormEntry` + `ContactResource` (contact form, sends email) |
| `tools.cevi.auth` | `User` entity (bcrypt passwords, JPA security) + `AuthResource` (login/logout at `/auth`) |
| `tools.cevi.infra` | Cross-cutting: `DemoData` (startup seed), `Slug` (URL slug generator), `ValidationMessage`, exception mappers, `LoggingFilter` |

### Authentication
Form-based auth via Quarkus Security JPA. Only the `admin` role can create/edit/delete. Admin-only endpoints are annotated `@RolesAllowed("admin")`. The login page is at `/auth/login`.

### Validation Pattern
Resources manually call `validator.validate(entity)` and collect results into `Set<ValidationMessage>`. Violations are returned to the template to render inline error messages. Transactions are managed explicitly with `QuarkusTransaction.begin()/commit()/rollback()`.

### Testing
- Uses `@QuarkusTest` with REST Assured for integration tests.
- `@TestSecurity(user = "admin", roles = {"admin"})` simulates an authenticated admin.
- `EventFixture` and `VoluntaryFixture` in `src/test/java/tools/cevi/fixture/` are registered as `QuarkusTestAfterEachCallback` (via `src/test/resources/META-INF/services/`) to clean up test data after each test.
- Tests use a file-based SQLite DB (not `:memory:`), as in-memory SQLite causes test failures with Quarkus.
- JaCoCo enforces ≥80% line coverage (checked during `post-integration-test` phase).

### Key Configuration (`application.properties`)
- Database: SQLite at `/tmp/international.db` (dev), overridden by `QUARKUS_DATASOURCE_JDBC_URL` in production.
- Mailer is mocked in dev (`quarkus.mailer.mock=true`).
- Locale: `de-CH` (German/Swiss).
- Session encryption key is set in `application.properties` for dev; must be overridden via `QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY` secret in production.
