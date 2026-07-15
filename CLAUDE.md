# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

nh-ecommerce is a portfolio mini e-commerce, built to exercise a full checkout flow end-to-end: cart, order creation, sandbox payment via webhook, and atomic stock updates with rollback on failure. Payments run in sandbox mode only — no real transactions.

The repo is a two-app monorepo with no shared root tooling (no root `package.json`, no workspace/monorepo tool). Each app is built and run independently from its own directory.

- `apps/api` — Spring Boot 4.1.0 backend (Java 21, Gradle)
- `apps/web` — Vite + React 19 + TypeScript frontend

The codebase currently has all five domain entities implemented: `User`, `Product`, `Order`, `OrderItem`, and `Payment`. `Product` is the first feature implemented end-to-end (repository → use cases → controller); the other four entities have no repository, use case, or controller yet. Don't assume the full architecture described below exists — check current source before relying on it.

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

- Package root: `com.nicolai.ecommerce`. Feature code is organized by domain package with a `domain` sub-package for entities, e.g. `com.nicolai.ecommerce.product.domain.Product` — follow this `<feature>/domain` (and expect `<feature>/{web,repository}` as siblings, plus `<feature>/domain/usecase` for use cases) convention when adding new features rather than layering by technical type (`entities/`, `controllers/`, etc. at the top level). `Product` is the reference implementation:
  - `product/domain` — entity, request DTOs (e.g. `CreateProductInput`), domain-specific exceptions (e.g. `ProductNotFoundException`)
  - `product/domain/usecase` — one class per action (`CreateProductUseCase`, `FindProductByIdUseCase`), see Conventions below
  - `product/repository` — Spring Data JPA interface (`ProductRepository extends JpaRepository<Product, Long>`), no manual implementation
  - `product/web` — controller + response DTO (e.g. `ProductResponse`)
  - Cross-cutting code that doesn't belong to one feature lives under `shared` (not a feature package): `shared/exception` (base HTTP-semantic exceptions + `GlobalExceptionHandler`), `shared/web` (`ErrorResponse`), `shared/config` (e.g. `SecurityConfig`).
- Stack: Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL driver. JWT auth and resource-owner access control are intended per the README but not yet implemented in source — see the Security note in Conventions below for the current interim state.
- Integration tests use Testcontainers (`spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:postgresql` — added in `build.gradle`). Pattern: `@Testcontainers` + `@Container static PostgreSQLContainer<?>` with `@ServiceConnection`, so Spring wires the `DataSource` automatically — no manual `@DynamicPropertySource`.

### Web

- Vite + React 19 + TypeScript, path alias `@/*` → `src/*`.
- UI components come from shadcn/ui (`components.json`: style `base-nova`, neutral base color, icon library `lucide-react`); generated components live under `src/components/ui`. Use the shadcn CLI conventions (`@/components`, `@/lib`, `@/hooks` aliases) when adding new UI components rather than hand-rolling structure.
- Styling via Tailwind CSS v4 (`@tailwindcss/vite` plugin, no separate `tailwind.config` — config lives in `src/index.css`).
- Data fetching/state: TanStack Query + axios. Forms: react-hook-form + zod via `@hookform/resolvers`.
- Linting via oxlint (`.oxlintrc.json`), not ESLint.

## Domain model

All five entities exist in source today:

- `User` — id, nome, email, senha_hash, role (`ADMIN` / `CLIENTE`) *(implemented)*
- `Product` — id, nome, descricao, preco_centavos, estoque, imagem_url, ativo *(implemented)*
- `Order` — id, user_id, status (`PENDENTE` / `PAGO` / `CANCELADO`), total_centavos *(implemented)*
- `OrderItem` — id, order_id, product_id, quantidade, preco_unitario_centavos *(implemented)*
- `Payment` — id, order_id, gateway (`STRIPE` / `MERCADOPAGO`), gateway_payment_id, status (`PENDENTE` / `APROVADO` / `RECUSADO`) *(implemented)*

`Product` is done end-to-end (repository → use cases → controller). Next: repeat the same vertical slice (repository → use cases → controller) for `Order`/`OrderItem`, `Payment`, and `User`, following the `<feature>/domain` package convention described in Architecture above.

`Payment` is deliberately its own entity, not fields on `Order` — keeps the order/business logic decoupled from whichever gateway is wired in. Money fields are integer cents, not decimal, to avoid floating-point rounding errors.

## Conventions

- **Constructor injection only.** Never use field-level `@Autowired`. This is a deliberate choice for this project.
- **Repositories extend `JpaRepository<T, Long>` directly**, not `CrudRepository` — pagination/sorting is needed (e.g. `GET /products`), and the interface-purity of composing over a separate JPA repository isn't worth the boilerplate at this project's scale.
- **One use case per action, not a multi-method service.** `CreateProductUseCase`, `FindProductByIdUseCase`, etc. — each does exactly one thing, each testable in isolation. This is deliberate, matches the SRP pattern already proven in prior projects, and matters most for `Checkout` specifically (multi-step transaction with rollback is much easier to unit test as an isolated class). Don't collapse related actions into a single `ProductService` with several methods. Use cases are `@Component`s that inject their repository (and any other collaborator) via constructor, and depend on nothing web-related — no `HttpServletRequest`, no DTOs meant only for JSON shaping.
- **Bean Validation runs inside the use case, not via `@Valid` on the controller.** A creation use case (e.g. `CreateProductUseCase`) takes a `jakarta.validation.Validator` via constructor and calls `validator.validate(input)` itself, throwing `ConstraintViolationException` on failure — instead of relying on `@Valid` on the controller's `@RequestBody`. Reason: `@Valid` validates via a Spring MVC/AOP proxy, which only exists when the endpoint is hit through a real `DispatcherServlet` — that would make the validation path untestable with a plain Mockito unit test (no Spring context) and, if `@Valid` were added on top, would make the use case's own validation call dead code on the HTTP path (the controller would reject invalid input before the use case is ever invoked). Business rules that aren't expressible as a Bean Validation annotation (e.g. `stock` can't be negative) are checked manually in the same use case and throw a feature-specific exception (e.g. `InvalidProductException`) — kept separate from `ConstraintViolationException` because one is "the request is malformed", the other is "the request is well-formed but violates a domain rule".
- **Domain exceptions extend a shared HTTP-semantic base, handled centrally.** `shared/exception` has `HttpNotFoundException` and `HttpBadRequestException` (plain `RuntimeException`s, no Spring dependency). Feature-specific exceptions extend one of these (e.g. `ProductNotFoundException extends HttpNotFoundException`). `shared/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) maps `HttpNotFoundException` → 404, `HttpBadRequestException` → 400, and `ConstraintViolationException` → 400, each returning a `shared/web/ErrorResponse(String message)` body. Add new feature exceptions by extending one of the two bases rather than writing a new `@ExceptionHandler` per feature.
- **Security is locked down by default; new endpoints need an explicit carve-out.** `spring-boot-starter-security` is on the classpath with no `SecurityFilterChain` until `shared/config/SecurityConfig` was added — Spring's default behavior secures every route. Since JWT auth isn't implemented yet, `SecurityConfig` currently does a narrow `permitAll()` scoped to `/products/**` only (plus `csrf().ignoringRequestMatchers("/products/**")`, needed for `POST` to work without a CSRF token), each line tagged `// TODO: remove once JWT is implemented`. Every route outside that carve-out still requires authentication (Spring's default). When a new feature's controller is added, extend the same `permitAll()` carve-out for its path rather than disabling security more broadly — and remove all of it once real JWT auth lands.
- **Entities need a creation constructor + getters, added when the feature's application layer is built.** Entities start as bare field-holders (JPA uses field access when `@Id` is on a field, so persistence works without accessors) but use cases/controllers need a way to build and read them. Add a protected no-arg constructor (required by JPA) plus a public constructor covering the creation fields (excluding `id`/auto-populated fields like `created_at`), and getters for every field — no setters unless the feature actually needs updates.
- **Checkout is one atomic transaction.** Payment confirmation → order status update → stock decrement must succeed or roll back together. Never leave an order in a partially-updated state — a failure partway through must fail closed, not silently succeed.
- **Resource ownership, not just role.** `GET /orders/{id}` (and similar) must check that the authenticated user is the actual owner of that specific order, not only that they hold a valid `CLIENTE`/`ADMIN` role.
- **Webhook signature validation is mandatory.** The payment webhook endpoint must verify the gateway's signature before trusting any payload — never process an unverified POST body.

## Testing conventions

- Plain data-holder entities (fields + Bean Validation annotations, no behavior) don't get a dedicated unit test file — their constraints are exercised through use case and integration tests instead.
- If an entity gains real behavior (a calculated field, a state-transition method), that specific behavior gets its own unit test.
- Use case tests must cover the failure/validation path, not just the happy path — assert on the actual returned/thrown value and `verify()` mock calls with the expected arguments, never just `assertNotNull`.
- Exception tests use JUnit 5's `assertThrows`, not manual try/catch.
- Controllers get a `@WebMvcTest(FeatureController.class)` slice test, use cases mocked with `@MockitoBean` (not the deprecated `@MockBean`) — covers the success status/body, the not-found path, and the validation-failure path (asserting the `GlobalExceptionHandler` mapping, not re-testing the use case's own validation logic, which already has its own unit test). Since a custom `SecurityFilterChain` bean isn't auto-included in a `@WebMvcTest` slice, add `@Import(SecurityConfig.class)` so the real `permitAll()`/authenticated rules are exercised instead of Spring Security's stricter default.
- Spring Boot 4 moved several test-slice annotations to new packages — e.g. `@WebMvcTest` is now `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, not the classic `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`. If a test-slice import doesn't resolve, check the actual class location inside the relevant `spring-boot-*-test` jar rather than assuming the old package.
- Integration tests (Testcontainers, real Postgres) are reserved for flows that cross layers — e.g. full checkout: order creation → payment webhook → stock decrement.

## CI

GitHub Actions, two separate workflows (`.github/workflows/api.yml`, `.github/workflows/web.yml`), each filtered by `paths` to only its own app — avoids running the frontend CI when only the backend changed, and vice versa. Both run on `ubuntu-latest`, which ships with Docker preinstalled, so Testcontainers-based integration tests work with no extra setup.

- API: `./gradlew build` (compiles + runs all tests, including Testcontainers)
- Web: `npm ci` → `npm run lint` → `npm run build`

## Database migrations

Flyway (not Liquibase — simpler, plain SQL, matches Postgres-only scope). Migration files live in `src/main/resources/db/migration/`, named `V{n}__description.sql` (e.g. `V1__create_initial_schema.sql`), applied in order automatically on startup.

`spring.jpa.hibernate.ddl-auto` is set to `validate`, not `update` — Hibernate checks the schema matches the entities but never generates or alters DDL itself. Flyway is the single source of truth for schema changes. Every new entity or column needs a new versioned migration file, never a reliance on Hibernate auto-generation.

## Scope (v1)

Single-item checkout for the first version — no multi-item cart yet, though the data model already supports it via `OrderItem` for later. No real shipping and no fiscal document emission: sandbox-only payments are a deliberate design decision for this portfolio project, not a limitation to "fix" later.
