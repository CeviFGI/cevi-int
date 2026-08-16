# Project Context

This project follows the AI Unified Process. Read `docs/vision.md`, `docs/requirements.md`,
and `docs/entity_model.md` for product context before making decisions. Always follow the skill
when changing requirements, entity-model or use-cases. `docs/deployment.md` holds everything the
production deployment needs (secrets, rate limiting, TLS, request limits, backup).

## AI Unified Process Workflow

1. `/requirements`        → derives `docs/requirements.md` from `docs/vision.md`
2. `/entity-model`        → derives `docs/entity_model.md` from requirements
3. `/use-case-diagram`    → produces `docs/use_cases.puml`
4. `/use-case-spec UC-XX` → produces `docs/use_cases/UC-XX-*.md`
5. No specific skill for implementation. Make sure to write tests for all changes and features.
6. All newly written code must be checked for Sonar Issues (see Sonar MCP with analyze snippet tool)

Never skip the spec for a use case before implementing it.
Always read the entity model before writing data access code.

The user is watching. You can ask for help, input or also things like visual verification

## Commands

### Development
```shell
./mvnw compile quarkus:dev
```
Starts the app with hot reload at http://localhost:8080. Dev UI at http://localhost:8080/q/dev/. On first start with an empty DB, demo events and offers are inserted automatically (`DemoData.java`, excluded from the `prod` profile). The administrator account is created separately by `AdminAccountBootstrap` from `application.admin.username`/`application.admin.password` — `admin`/`admin` in dev and test, a deployment secret in production.

### Run tests
```shell
./mvnw test
```

### Run a single test class
```shell
./mvnw test -Dtest=EventUpcomingTest
```

### Run tests including Playwright e2e tests
```shell
./mvnw verify
```
Runs the unit tests plus the browser-based e2e tests (`*E2ETest.java`, see Testing section below). The first run downloads a Chromium binary via the Playwright Maven plugin execution.

### Run a single e2e test class
```shell
./mvnw verify -Dit.test=LoginE2ETest
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

- **Resources** (`src/main/java/tools/cevi/`) — Quarkus REST endpoints that render Qute templates. Each resource has a nested `@CheckedTemplate` inner class (`Templates`) that maps method names to template files. The stack is Quarkus REST (`quarkus-rest-qute`), not RESTEasy Classic — `quarkus-rest-csrf` is only available there.
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
Form-based auth via Quarkus Security JPA. Only the `admin` role can create/edit/delete. Admin-only endpoints are annotated `@RolesAllowed("admin")`. The login page is at `/auth/login`. Templates ask `inject:currentUser` (a `@Named` request-scoped bean over `SecurityIdentity`) whether to show maintenance links — never the presence of the session cookie, which any visitor can set.

### Security invariants
These are load-bearing; changing them needs a matching change in `docs/requirements.md` and the use case specs:

- **Every state change is a `POST` carrying a CSRF token.** Forms include `<input type="hidden" name="{inject:csrf.parameterName}" value="{inject:csrf.token}"/>`; that includes deleting and signing out. A `GET` never changes data.
- **Descriptions are sanitised before storage**, not before rendering: `HtmlSanitizer.sanitize(...)` in the resource, because `{it.description.raw}` renders them unescaped. The allow-list is documented in `docs/entity_model.md`.
- **No inline `<script>` in templates** — the Content-Security-Policy keeps `script-src 'self'`. Editor setup lives in `META-INF/resources/js/summernote-init.js`.
- **Secrets are never in `application.properties`** for the `prod` profile. `ProductionSecurityCheck` and `mount_secrets.sh` abort the start if one is missing.
- jQuery and Summernote come from WebJars and are served under version-less `/webjars/...` URLs by `quarkus-webjars-locator`. Do not check frontend libraries into the repository.

### Validation Pattern
Resources manually call `validator.validate(entity)` and collect results into `Set<ValidationMessage>`. Violations are returned to the template to render inline error messages. Transactions are managed explicitly with `QuarkusTransaction.begin()/commit()/rollback()`.

### Testing
- Uses `@QuarkusTest` with REST Assured for integration tests.
- `@TestSecurity(user = "admin", roles = {"admin"})` simulates an authenticated admin.
- `EventFixture` and `VoluntaryFixture` in `src/test/java/tools/cevi/fixture/` are registered as `QuarkusTestAfterEachCallback` (via `src/test/resources/META-INF/services/`) to clean up test data after each test.
- **Any test that POSTs a form must go through `Csrf.given()`** (`src/test/java/tools/cevi/fixture/Csrf.java`), which fetches a page first and sends the matching cookie and hidden field. `Csrf.givenWithoutToken()` is for the tests that assert the rejection. CSRF verification stays enabled in the test profile on purpose.
- Tests use a file-based SQLite DB (not `:memory:`), as in-memory SQLite causes test failures with Quarkus.
- JaCoCo enforces ≥80% line coverage (checked during `post-integration-test` phase) against `target/jacoco-merged.exec`, which merges the unit-test run (`jacoco-unit.exec`) with the e2e-test run (`jacoco-it.exec`, produced by Failsafe via `prepare-agent-integration`/`failsafeArgLine`). They're recorded to separate files and merged rather than sharing one file because Quarkus's own coverage dump on JVM shutdown (the `quarkus-jacoco` extension) does not append across JVM sessions — writing both test phases to the same file would silently let the second (much smaller) session overwrite the first's data. Local coverage runs also need a *fresh* `/tmp/international.db` (delete it first) — otherwise `DemoData`'s seed-on-empty-DB logic never re-executes and coverage reads artificially low.
- **e2e tests**: real-browser tests live in `src/test/java/tools/cevi/e2e/`, named `*E2ETest.java` so Failsafe (not Surefire) picks them up — they only run on `./mvnw verify`, not `./mvnw test`. They extend `PlaywrightTestBase`, which boots the same `@QuarkusTest` HTTP server as the REST Assured tests and drives it with a real headless Chromium (Playwright Java, no Node.js/npm involved). Use this layer for behavior that can't be verified over plain HTTP, e.g. the jQuery/Summernote rich-text editor. Failed tests leave a trace under `target/playwright-traces/` (uploaded as a CI artifact on failure, viewable with the Playwright Trace Viewer).

### Key Configuration (`application.properties`)
- Database: SQLite at `/tmp/international.db` (dev), overridden by `QUARKUS_DATASOURCE_JDBC_URL` in production.
- Mailer is mocked in dev (`quarkus.mailer.mock=true`).
- Locale: `de-CH` (German/Swiss).
- Session encryption key is set in `application.properties` for dev; must be overridden via `QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY` secret in production.
