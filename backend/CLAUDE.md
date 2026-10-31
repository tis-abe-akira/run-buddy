# backend: rules for AI agents

Spring Boot 4 / Java 25 / Maven. Read `docs/adr/` (especially ADR-0004 and ADR-0007) and
`docs/guides/database.md` (inspecting the database, row level security, schema changes) before
changing the structure.

## Design

- Domain types (`plan`, `activity`, `dashboard`, `shared`) are immutable records or final classes
  that check their own invariants. Do not add setters, no-arg constructors or persistence
  annotations to them.
- Repositories are ports (interfaces in the domain packages). Adapters live in `persistence/`.
  Keep ports free of `default` methods: a default method runs outside the adapter's
  `@Transactional` proxy (this caused a real bug, see ADR-0007). `TransactionalPortRulesTest`
  (ArchUnit) fails the build on any default method in an interface a `@Transactional` class
  implements; never weaken it to make a change pass.
  `scripts/archunit-mutation-check.sh` (run in CI) plants violations in a throwaway worktree and
  fails unless the rule reports each one; when you change the rule, keep that check passing.

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
- **Never edit `persistence/jooq/generated` by hand.** CI (`.github/workflows/ci.yml`)
  regenerates it and fails on any diff.
- Claude's GitHub App cannot push changes under `.github/workflows/`. To change CI, propose the
  edit and let the owner commit it.
- Write queries with the jOOQ DSL and the generated tables. For SQL the DSL cannot express, use
  jOOQ's plain SQL API (`dsl.resultQuery(...)`) rather than another library.

## Tests

- `./mvnw test` runs everything; database tests start PostgreSQL with Testcontainers (Docker).
- Repository behaviour is specified once, as contract interfaces
  (`MonthlyPlanRepositoryContract`, `RunResultRepositoryContract`, `RowLevelSecurityContract`,
  `ApiOverDatabaseContract`), and every adapter's test implements them. New repository methods get
  a contract test first.
- **Never delete, skip or weaken `RowLevelSecurityContract` or `DatabaseSafetyTest`.** Because
  queries rely on row level security instead of filtering by user, these tests are what stops
  one user's data from leaking to another.
- Database tests act as a fresh random user (`DatabaseTest`), so they need no cleanup and must not
  depend on data from other tests.

## AI advisor (ADR-0008)

- `TrainingAdvisor` is the port, one implementation per provider; `AdvisorDirectory` routes by
  provider id. Spring AI types never leave `advisor/springai`.
- To add a provider, add a configuration class like `GeminiAdvisorConfig`: active only when its
  credentials are set, building a `SpringAiTrainingAdvisor`. Do not add Spring AI starters; they
  auto-configure (and can fail) without credentials.
- Rely only on chat, tool calling and structured output. Provider-specific options go in the
  provider's `optionsForModel` function, never in the shared code.
- The model's proposed plan is untrusted input: it becomes a `MonthlyPlan` only through
  `AdvisorAnswer.toPlan`, and is never saved automatically.
- Never call a model inside a database transaction. Tools in `AdvisorTools` read through the
  repositories, one short transaction per call.
- Tests must not call real providers except the `*LiveTest` classes, which are skipped unless the
  provider's API key is set. Use `ScriptedChatModel` or the fake advisor elsewhere.

## Garmin (ADR-0009)

- `GarminActivitySource` (in `activity/garmin`) is the only code that knows Garmin's API; it
  throws `ActivitySourceUnavailableException` with a reason, never Spring's HTTP exceptions.
- Only build on endpoints and fields confirmed in Garmin's Swagger or in a real response, and
  record what was confirmed in ADR-0009. Test fixtures copy the response's shape with made-up
  values: never commit real activity ids, names, locations or other personal data.
- Never log the API key or the access token.

## Local settings

- Local settings and secrets live in the repository root's `.env` (template: `.env.example`),
  read by docker compose and by the backend through `spring.config.import`. Never commit `.env`
  or put a key in any tracked file; add new settings to `.env.example` with an empty value.
- Live tests that call real providers are gated on an OS environment variable, not on `.env`, so
  a plain `./mvnw test` never spends money.

## Wiki (ADR-0011)

- `openwiki/` is generated by OpenWiki in CI; do not edit it, except `openwiki/INSTRUCTIONS.md`
  (the human-written brief). When a change makes the brief wrong (a renamed class, a new
  architectural rule), update the brief in the same change.
