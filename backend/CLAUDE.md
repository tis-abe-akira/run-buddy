# backend: rules for AI agents

Spring Boot 4 / Java 25 / Maven. Read `docs/adr/` (especially ADR-0004 and ADR-0007) before
changing the structure.

## Design

- Domain types (`plan`, `activity`, `dashboard`, `shared`) are immutable records or final classes
  that check their own invariants. Do not add setters, no-arg constructors or persistence
  annotations to them.
- Repositories are ports (interfaces in the domain packages). Adapters live in `persistence/`.
  Keep ports free of `default` methods: a default method runs outside the adapter's
  `@Transactional` proxy (this caused a real bug, see ADR-0007).

## Database (PostgreSQL + jOOQ + row level security)

- Every table has a `user_id` column filled by the database and protected by row level security.
  **Application SQL never mentions `user_id`.** Do not add it to queries "for safety"; do not
  write a table without `user_id`, its default, and an `own_rows` policy.
- **All database access runs inside a transaction.** `RlsTransactionManager` sets `app.user_id`
  when a transaction begins; outside one, PostgreSQL raises `app.user_id is not set`.
  Adapters are annotated `@Transactional` (read methods `readOnly = true`).
- The application connects as `runbuddy_app`, which does not own the tables. Never connect the
  application or its tests as the owner or a superuser: they bypass row level security.
- Schema changes are new Flyway migrations in `src/main/resources/db/migration`
  (`V2__...sql`, ...). Never edit a migration that has been merged.
- **After changing a migration, regenerate jOOQ:** `./scripts/generate-jooq.sh` (needs Docker),
  then fix the compile errors it produces; they list every place the change affects.
- **Never edit `persistence/jooq/generated` by hand.** CI regenerates it and fails on any diff
  (`docs/ci/ci.yml`, to be installed as `.github/workflows/ci.yml`).
- Write queries with the jOOQ DSL and the generated tables. For SQL the DSL cannot express, use
  jOOQ's plain SQL API (`dsl.resultQuery(...)`) rather than another library.

## Tests

- `mvn test` runs everything; database tests start PostgreSQL with Testcontainers (Docker).
- Repository behaviour is specified once, as contract interfaces
  (`MonthlyPlanRepositoryContract`, `RunResultRepositoryContract`, `RowLevelSecurityContract`,
  `ApiOverDatabaseContract`), and every adapter's test implements them. New repository methods get
  a contract test first.
- **Never delete, skip or weaken `RowLevelSecurityContract` or `DatabaseSafetyTest`.** Because
  queries rely on row level security instead of filtering by user, these tests are what stops
  one user's data from leaking to another.
- Database tests act as a fresh random user (`DatabaseTest`), so they need no cleanup and must not
  depend on data from other tests.
