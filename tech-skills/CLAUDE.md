# Architecture — Ports and Adapters (Hexagonal)

This project uses hexagonal architecture. **Before creating, moving, or suggesting the location of any class, consult this file.** If new code doesn't respect this structure, fix it or ask before continuing — don't invent a new folder on your own.

## Dependency rule (non-negotiable)

```
api/  ──▶  domain/  ◀──  infrastructure/
```

- `domain/` **never** imports anything from `api/` or `infrastructure/`.
- `domain/` **never** imports annotations or classes from a specific framework, ORM, message broker, or HTTP client (no `@Entity`, no Spring Data `@Repository`, no Kafka/RabbitMQ/SQS classes, no `org.springframework.web`, etc.). The domain is plain Java/Kotlin.
- `api/` and `infrastructure/` can import from `domain/`; never the other way around.
- If the domain needs something external (persistence, messaging, a call to another service, a clock, an ID generator), it's defined as an **interface** in `domain/port/out/` and implemented in `infrastructure/`.

This rule applies regardless of which concrete technology is used in each layer (JPA or MongoDB or an HTTP client; Kafka or RabbitMQ or SQS; REST or GraphQL or gRPC). What changes is the content of `infrastructure/`, never the contract of `domain/`.

## Package structure per module/service

```
<base-package>/
│
├── domain/
│   ├── model/              Business entities and value objects (no infrastructure annotations)
│   ├── port/
│   │   ├── in/              Use case interfaces → suffix UseCase
│   │   └── out/             Interfaces the domain needs from the outside (persistence, messaging, external services, time, etc.)
│   ├── exception/          Business exceptions
│   ├── event/               Domain event payloads (if the domain publishes events)
│   └── service/
│       ├── dto/              Internal use-case DTOs (UseCase input/output)
│       └── *Impl.java        UseCase implementation
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/           Persistence models → suffix depends on technology (see table)
│   │   ├── repository/       Data access for the chosen technology
│   │   ├── adapter/          Implements the domain's port/out/
│   │   └── mapper/           Maps between domain/model and the persistence model
│   ├── messaging/             (only if the project uses asynchronous messaging)
│   │   ├── publisher/        Implements port/out/*Publisher with the chosen broker
│   │   ├── consumer/          Receives the message, deserializes it, and calls the corresponding UseCase
│   │   └── mapper/            Maps between message payload and domain/model
│   └── client/                 (only if the project calls external services)
│       └── adapter/            Implements the port/out/ representing that external dependency
│
└── api/                         (or whichever inbound adapter applies: rest/, graphql/, grpc/, cli/)
    ├── controller/             Exposes the use case to the inbound protocol
    ├── controller/advice/      Global error handling for the inbound protocol
    ├── mapper/                  Maps between the external contract (request/response) and domain/service/dto
    └── filter/                   Interceptors/filters specific to the inbound protocol
```

If the project doesn't use messaging, omit `infrastructure/messaging/`. If it doesn't have a REST API but another inbound protocol, rename `api/` accordingly (`graphql/`, `grpc/`, `cli/`) while keeping the same internal substructure.

## Naming conventions (mandatory)

| Layer | Class type | Suffix / pattern |
|---|---|---|
| `domain/port/in/` | Use case interface | `*UseCase` |
| `domain/service/` | Use case implementation | `*ServiceImpl implements *UseCase` |
| `domain/model/` | Business entity or value object | Plain name, no technical suffix |
| `domain/port/out/` | Outbound port (interface) | No technology suffix (e.g. `OrderRepository`, not `OrderJpaRepository`) |
| `infrastructure/persistence/entity/` | Persistence model | Suffix per technology: `*JpaEntity` (JPA), `*Document` (MongoDB), `*Record` (jOOQ/JDBC), etc. |
| `infrastructure/persistence/repository/` | Concrete data access | Suffix indicating the technology (e.g. `*JpaRepository`, `*MongoRepository`) |
| `infrastructure/persistence/adapter/` | Adapter implementing the port/out | `*RepositoryAdapter` |
| `infrastructure/messaging/publisher/` | Event/message publisher | Prefix or suffix with the technology (e.g. `Kafka*Publisher`, `Sqs*Publisher`) |
| `infrastructure/messaging/consumer/` | Event/message receiver | `*Consumer` or `*Listener` |
| `infrastructure/client/adapter/` | External service client | `*ClientAdapter` |
| `api/controller/` | Inbound protocol controller | `*Controller` |
| `api/mapper/` | External contract ↔ domain mapper | `*ApiMapper` (or the protocol's suffix: `*GraphQlMapper`, etc.) |
| `infrastructure/persistence/mapper/` | Domain ↔ persistence model mapper | `*Mapper` (no external-protocol suffix) |

If the project standardizes on a specific technology (e.g. always JPA, always Kafka), replace the generic suffix in the table with the specific one and keep it consistent across the whole project — the important rule is that the same kind of class always uses the same suffix, with no case-by-case exceptions.

## Additional rules

1. **An artifact that exists only because of an infrastructure decision never lives in `domain/model/`.** Examples: a cache table, a replica/snapshot of another service's data, a technical audit table. These classes belong in `infrastructure/persistence/entity/`, not in the domain, even if they conceptually "look like" a business entity.

2. **The domain must not be anemic.** Business logic (calculations, validations, state transitions, invariants) lives in the domain model or in the `*ServiceImpl`, never in the controller, the persistence adapter, or the mapper.

3. **Values from a fixed, known set are an `Enum` or closed type, never a loose `String`/`int`.** If a field can only take a specific set of values in the business domain, it must be an enum (or value object) defined in `domain/model/`.

4. **Tests mirror the same structure** as production code, with `domain/`, `api/`, `infrastructure/` subfolders paralleling `main`.

5. **Before adding a new dependency to the package manager**, verify it's actually used in code. Don't leave imported libraries unused.

6. **Don't mix conventions across similar modules in the same project.** If there are multiple services or modules, all must follow exactly the same structure and the same suffixes — an inconsistency between modules (e.g. a mapper in a different location in each service) is a defect to fix, not an acceptable variation.

## Example of a correct flow (creating a resource, illustrative)

```
1. api/controller/...Controller            receives the request from the inbound protocol
2. api/mapper/...ApiMapper                  converts the external contract → domain DTO
3. domain/port/in/...UseCase                interface the controller invokes
4. domain/service/...ServiceImpl            validates business rules, uses the domain model
5. domain/port/out/...Repository            interface the service invokes to persist
6. infrastructure/persistence/adapter/...   implements the port/out, translates to the chosen technology
7. infrastructure/persistence/mapper/...    converts domain/model → persistence model
8. infrastructure/persistence/repository/.. executes the save/query operation
```

If a new class doesn't clearly fit any of these folders, ask before inventing a new location.

# Concurrency Guidelines

Thread safety is part of the architecture.

When introducing mutable state:

- Mutable state must belong to a single aggregate.
- Never protect unrelated aggregates with a global lock.
- Prefer fine-grained synchronization over coarse-grained locking.
- Prefer immutable state whenever practical.
- Prefer AtomicReference + Compare-And-Set for high-contention updates.
- Use ConcurrentHashMap instead of synchronized collections.
- Services should remain stateless.
- Synchronization should be encapsulated inside the aggregate, never exposed to callers.
- Avoid synchronized methods in services unless absolutely necessary.

# Domain Events

Domain events represent meaningful business occurrences.

Rules:

- Domain events are created by the domain.
- Infrastructure publishes them.
- Domain events never depend on Kafka classes.
- Event payloads belong in domain/event.
- Infrastructure maps domain events into Kafka messages.
  Mapper responsibilities:

# Data transformation

Not allowed:

- Business validation
- Calculations
- Calling repositories
- Calling external services
- Throwing business exceptions

# Use cases orchestrate business logic

## Responsibilities:

- validate business rules
- coordinate aggregates
- call outbound ports
- publish domain events

## They should NOT:

- contain HTTP concepts
- know Kafka
- know JPA
- know Spring

# Aggregates

Aggregates own their consistency boundaries.

Rules:

- Mutable state belongs to the aggregate.
- Business invariants are enforced inside the aggregate.
- External code must not modify aggregate state directly.
- Aggregates expose behavior instead of setters.

# Value Objects

- Immutable
- Equality by value
- No setters
- Validate themselves during construction

# Immutability

Prefer immutable objects whenever possible.

If state changes frequently:

- Create a new immutable state.
- Replace the reference atomically.
- Never mutate shared objects after publication.

# Messaging

## Publishers:

- implement domain ports
- never contain business rules

## Consumers:

- deserialize messages
- invoke a UseCase
- never contain business logic

Message mappers translate Kafka payloads into domain events.

# Naming

## Business events:

PriceUpdated
SnapshotCalculated
OrderConfirmed

## Kafka publishers:

KafkaPriceUpdatedPublisher

## Kafka consumers:

PriceUpdatedConsumer

# Design Philosophy

Prefer:

- composition over inheritance
- immutable objects
- explicit dependencies
- ports over direct framework usage
- constructor injection
- encapsulation
- behavior over getters/setters
- small focused classes
- high cohesion
- low coupling

# Application Services

Application services coordinate work.

They should:

- orchestrate use cases
- invoke ports
- publish events

They should NOT:

- implement business entities
- expose mutable state
- cache domain objects

# Never inject infrastructure classes into domain services.

## Correct

UseCase
↓
Repository Port

## Incorrect

UseCase
↓
JpaRepository