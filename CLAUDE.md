# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

nh-ecommerce is a portfolio mini e-commerce, built to exercise a full checkout flow end-to-end: cart, order creation, sandbox payment via webhook, and atomic stock updates with rollback on failure. Payments run in sandbox mode only — no real transactions.

The repo is a two-app monorepo with no shared root tooling (no root `package.json`, no workspace/monorepo tool). Each app is built and run independently from its own directory.

- `apps/api` — Spring Boot 4.1.0 backend (Java 21, Gradle)
- `apps/web` — Vite + React 19 + TypeScript frontend

The codebase currently has `Product`, `Order`, and `OrderItem` JPA entities; no repositories, services, or controllers yet. Don't assume the full architecture described below exists — check current source before relying on it.

## Commands

### API (`apps/api`)

Run from `apps/api/`:

```
./gradlew build          # compile + run tests
./gradlew test           # run tests only (JUnit 5 / Testcontainers via JUnit Platform)
./gradlew test --tests "com.nicolai.ecommerce.SomeClassTest"   # run a single test class
./gradlew bootRun         # run the API locally
```

Local Postgres for the API is provided via Spring Boot's Docker Compose support (`developmentOnly 'org.springframework.boot:spring-boot-docker-compose'`), which auto-starts/stops `apps/api/compose.yaml` (`postgres:latest`, db `mydatabase`, user `myuser`) whenever you run `bootRun` — no manual `docker compose up` needed. The Docker daemon itself still has to be running first (`sudo systemctl start docker`), or Spring's auto-start fails with a "cannot connect to the Docker API" error.

There used to be a second, unrelated root-level `docker-compose.yml` — it was removed since it's outside `apps/api/` (Spring never auto-detected it anyway) and duplicated what the Spring-managed file already does. `apps/api/compose.yaml` is now the only Postgres config for local dev.

### Web (`apps/web`)

Run from `apps/web/`:

```
npm run dev        # start Vite dev server
npm run build       # tsc -b && vite build
npm run lint        # oxlint
npm run preview     # preview production build
```

No test runner is currently configured for the web app.

## Architecture

### API

- Package root: `com.nicolai.ecommerce`. Feature code is organized by domain package with a `domain` sub-package for entities, e.g. `com.nicolai.ecommerce.product.domain.Product` — follow this `<feature>/domain` (and expect `<feature>/{web,service,repository}` as siblings) convention when adding new features rather than layering by technical type (`entities/`, `controllers/`, etc. at the top level).
- Stack: Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL driver. JWT auth and resource-owner access control are intended per the README but not yet implemented in source.
- Integration tests use Testcontainers (`spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:postgresql` — added in `build.gradle`). Pattern: `@Testcontainers` + `@Container static PostgreSQLContainer<?>` with `@ServiceConnection`, so Spring wires the `DataSource` automatically — no manual `@DynamicPropertySource`.

### Web

- Vite + React 19 + TypeScript, path alias `@/*` → `src/*`.
- UI components come from shadcn/ui (`components.json`: style `base-nova`, neutral base color, icon library `lucide-react`); generated components live under `src/components/ui`. Use the shadcn CLI conventions (`@/components`, `@/lib`, `@/hooks` aliases) when adding new UI components rather than hand-rolling structure.
- Styling via Tailwind CSS v4 (`@tailwindcss/vite` plugin, no separate `tailwind.config` — config lives in `src/index.css`).
- Data fetching/state: TanStack Query + axios. Forms: react-hook-form + zod via `@hookform/resolvers`.
- Linting via oxlint (`.oxlintrc.json`), not ESLint.

## Domain model (planned)

- `User` — id, nome, email, senha_hash, role (`ADMIN` / `CLIENTE`)
- `Product` — id, nome, descricao, preco_centavos, estoque, imagem_url, ativo
- `Order` — id, user_id, status (`PENDENTE` / `PAGO` / `CANCELADO`), total_centavos
- `OrderItem` — id, order_id, product_id, quantidade, preco_unitario_centavos
- `Payment` — id, order_id, gateway (`STRIPE` / `MERCADOPAGO`), gateway_payment_id, status (`PENDENTE` / `APROVADO` / `RECUSADO`)

`Payment` is deliberately its own entity, not fields on `Order` — keeps the order/business logic decoupled from whichever gateway is wired in. Money fields are integer cents, not decimal, to avoid floating-point rounding errors.

## Conventions

- **Constructor injection only.** Never use field-level `@Autowired`. This is a deliberate choice for this project.
- **Checkout is one atomic transaction.** Payment confirmation → order status update → stock decrement must succeed or roll back together. Never leave an order in a partially-updated state — a failure partway through must fail closed, not silently succeed.
- **Resource ownership, not just role.** `GET /orders/{id}` (and similar) must check that the authenticated user is the actual owner of that specific order, not only that they hold a valid `CLIENTE`/`ADMIN` role.
- **Webhook signature validation is mandatory.** The payment webhook endpoint must verify the gateway's signature before trusting any payload — never process an unverified POST body.

## Testing conventions

- Plain data-holder entities (fields + Bean Validation annotations, no behavior) don't get a dedicated unit test file — their constraints are exercised through use case and integration tests instead.
- If an entity gains real behavior (a calculated field, a state-transition method), that specific behavior gets its own unit test.
- Use case tests must cover the failure/validation path, not just the happy path — assert on the actual returned/thrown value and `verify()` mock calls with the expected arguments, never just `assertNotNull`.
- Exception tests use JUnit 5's `assertThrows`, not manual try/catch.
- Integration tests (Testcontainers, real Postgres) are reserved for flows that cross layers — e.g. full checkout: order creation → payment webhook → stock decrement.

## Database migrations

Flyway (not Liquibase — simpler, plain SQL, matches Postgres-only scope). Migration files live in `src/main/resources/db/migration/`, named `V{n}__description.sql` (e.g. `V1__create_initial_schema.sql`), applied in order automatically on startup.

`spring.jpa.hibernate.ddl-auto` is set to `validate`, not `update` — Hibernate checks the schema matches the entities but never generates or alters DDL itself. Flyway is the single source of truth for schema changes. Every new entity or column needs a new versioned migration file, never a reliance on Hibernate auto-generation.

## Scope (v1)

Single-item checkout for the first version — no multi-item cart yet, though the data model already supports it via `OrderItem` for later. No real shipping and no fiscal document emission: sandbox-only payments are a deliberate design decision for this portfolio project, not a limitation to "fix" later.
