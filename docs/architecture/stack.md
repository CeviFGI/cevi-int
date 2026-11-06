# Stack

**For the agent:** read before writing code. This file is the AI Unified Process record of the technical layers,
independent of how the team documents its architecture. The Layers table says which layer skill builds and tests
each technical layer: `/use-case-implement` runs the Skill column bottom-up, `/use-case-test` runs the Test skill
column from the integration layer up, and `/use-case-coverage` searches for the markers in the last table.

**Last updated:** 2026-11-06

## Layers

| Layer       | Technology                                             | Skill | Test skill | Location                                                          |
|-------------|--------------------------------------------------------|-------|------------|-------------------------------------------------------------------|
| Migration   | Flyway SQL scripts on SQLite (C-002, C-003)            | –     | –          | `src/main/resources/db/migration`                                 |
| Persistence | Hibernate ORM with Panache, active-record entities     | –     | –          | entity class in each feature package of `tools.cevi`              |
| Client      | –                                                      | –     | –          | –                                                                 |
| Service     | –                                                      | –     | –          | –                                                                 |
| API         | –                                                      | –     | –          | –                                                                 |
| UI          | Quarkus REST + Qute server-side templates (C-001, C-013) | –   | –          | `*Resource` in each feature package; `src/main/resources/templates`, `META-INF/resources` |
| E2E         | Playwright for Java                                    | –     | –          | `src/test/java/tools/cevi/e2e` (`*E2ETest.java`)                  |

**Integration layer:**

| Use case offered through | Tested in   | Test skill |
|--------------------------|-------------|------------|
| UI                       | UI          | –          |
| Service                  | Persistence | –          |

A layer the project does not have is marked `–` in every column (a UI layer in a REST service). The Integration
layer table keeps only the rows of the entry layers the project has — a REST service drops the UI row — and its
Test skill column names what the Layers table names for that layer (`–` while the skill is not installed). A use
case is tested in the first row its `**Layers:**` line names, or in the first row when it has no such line. That layer's tests run in process against the real database
and exercise every layer below it, so `/use-case-test` runs its test skill and the ones above it and writes no tests
of their own for the layers below unless a run names them (`/use-case-test UC-001 persistence`).

No layer skill of the marketplace covers Quarkus, so every Skill and Test skill column is `–`:
`/use-case-implement` and `/use-case-test` build and test each layer from the project's existing code and the
conventions in `CLAUDE.md`. The project has no separate service layer: the Quarkus REST resource is the
transaction boundary (`QuarkusTransaction`) and enforces the business rules, and the Panache entity holds the
queries. The resource renders Qute templates on the server, so it is the UI layer, not an API for a separate
client. UI tests are `@QuarkusTest` classes with REST Assured — in process, against the real SQLite database —
which makes the UI the integration layer.

**E2E scope:** Use cases and test cases

`Use cases and test cases`: `/use-case-test` adds a browser test to every use case offered through the UI, and
`/test-case-implement` automates the TC-* journeys. `Test cases only`: browser tests are written for the journeys
alone, and a use case is tested in its integration layer. A project without an E2E layer drops the line.

## Module layout

Flat single Maven module, root package `tools.cevi`, one package per feature: `event`, `voluntary`, `contact`,
`auth`, and `infra` for cross-cutting code. Each feature package holds its Panache entity and its `*Resource`;
the resource's templates live in `src/main/resources/templates/<ResourceName>/`, shared fragments in
`templates/tags/`, static assets (CSS, the few self-hosted scripts, fonts) in
`src/main/resources/META-INF/resources/`. Tests mirror the packages under `src/test/java/tools/cevi`; fixtures in
`tools.cevi.fixture`, browser tests in `tools.cevi.e2e`.

## Commands

All commands run through the Docker wrapper (`tooling/docker.sh`), never `mvn` directly (C-016).

| Purpose            | Command                                              |
|--------------------|------------------------------------------------------|
| Build              | `tooling/docker.sh build`                            |
| Unit tests         | `tooling/docker.sh test`                             |
| Integration tests  | `tooling/docker.sh test` (`@QuarkusTest` + REST Assured) |
| End-to-end tests   | `tooling/docker.sh verify`                           |

## Markers

Each layer skill keeps its own rows here: on every run it checks that the rows for its layer match its marker
reference and corrects them. Do not edit the rows by hand; rerun the layer skill instead.

| Layer | Skill | Kind | Marker to grep for |
|-------|-------|------|--------------------|
| All   | –     | Code | `// UC-XXX BR-YYY:` directly above the method, query condition, or validator that enforces the rule |
| UI, E2E | –   | Test | `UC-XXX` in the test class Javadoc; each test's Javadoc names the flow or rule it covers (`A1`, `BR-003`, "Main success scenario") |
