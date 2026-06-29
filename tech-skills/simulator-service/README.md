# Market Simulator

Market Simulator is a Spring Boot service that generates synthetic market price updates for a configurable list of financial instruments and publishes those updates to Kafka.

The project is intentionally built using **Clean Architecture (Ports & Adapters)** and **Domain-Driven Design (DDD)** to demonstrate a maintainable and extensible architecture rather than focusing only on business functionality.

---

## Architecture

The application follows this architecture:

```
                +----------------------+
                | REST Controller      |
                +----------+-----------+
                           |
                    Inbound Port
                           |
                +----------v-----------+
                | Domain Service       |
                | Business Rules       |
                +----------+-----------+
                           |
                 Outbound Port
                           |
      +--------------------+-------------------+
      |                                        |
+-----v------+                        +---------v---------+
| Kafka      |                        | In-Memory Logger  |
| Publisher  |                        | Publisher         |
+------------+                        +-------------------+
```

The domain has no dependency on Spring, Kafka, or any infrastructure framework.

---

## Features

- Simulates market price movements
- Configurable financial instruments
- Configurable volatility
- Scheduled market ticks
- Publishes price updates to Kafka
- REST endpoint for tracked symbols
- Clean Architecture / Hexagonal Architecture
- Domain Events
- Spring Boot 3
- Java 21

---

## Project Structure

```
src/main/java
├── api
│   ├── controller
│   └── advice
│
├── domain
│   ├── model
│   ├── event
│   ├── service
│   └── port
│       ├── in
│       └── out
│
├── infrastructure
│   ├── scheduler
│   ├── messaging
│   ├── listener
│   └── config
```

---

## How it Works

Every configurable interval, the scheduler triggers a simulation cycle.

During each cycle:

1. Every tracked instrument generates a new market price.
2. The new price becomes the current state of the instrument.
3. A domain event is created.
4. The event is published through the outbound port.
5. Infrastructure publishes the event to Kafka.

The scheduler knows nothing about the implementation details of the simulation, and the domain knows nothing about Kafka.

---

## Configuration

Application configuration is defined in:

```yaml
application.yml
```

Example:

```yaml
market-simulator:
  tick-interval-ms: 10000

  instruments:
    - symbol: AAPL
      starting-price: 201.35
      volatility-basis-points: 15

    - symbol: MSFT
      starting-price: 468.12
      volatility-basis-points: 12

    - symbol: NVDA
      starting-price: 180.52
      volatility-basis-points: 30
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
```

---

## REST API

### Get tracked symbols

```
GET /api/v1/market-simulator/symbols
```

Example response

```json
[
  "AAPL",
  "MSFT",
  "NVDA"
]
```

---

## Kafka Events

Topic

```
market.price.updated
```

The message key is the instrument symbol.

Example:

```json
{
  "symbol": "AAPL",
  "price": 201.87,
  "timestamp": "2026-06-28T19:22:11Z"
}
```

Using the symbol as the partition key guarantees ordering for each instrument.

---

## Running the Application

### Requirements

- Java 21
- Gradle
- Kafka

Run:

```bash
./gradlew bootRun
```

or

```bash
./gradlew clean build
```

---

## Docker

Build

```bash
docker build -t market-simulator .
```

Run

```bash
docker run \
-e KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
-p 8083:8083 \
market-simulator
```

---

## Concurrency Design

Although the current implementation executes market ticks from a single scheduled thread, the domain was intentionally designed to be thread-safe and ready for future concurrent execution.

### Thread-safe Instrument State

Each `TrackedInstrument` owns its mutable state (`lastPrice`).

Price generation is synchronized:

```java
public synchronized BigDecimal nextPrice() {
    ...
    this.lastPrice = candidate;
    return candidate;
}
```

This guarantees that:

- only one thread can update an instrument at a time;
- price updates cannot be lost due to race conditions;
- every generated price is based on the latest committed value.

The synchronization scope is limited to a single instrument, so different instruments can still be processed concurrently.

---

### Volatile State Visibility

The latest price is stored as:

```java
private volatile BigDecimal lastPrice;
```

Using `volatile` guarantees visibility between threads.

Whenever one thread updates the latest price, all other threads immediately observe the new value without requiring additional synchronization for reads.

---

### Concurrent Instrument Registry

Tracked instruments are stored in a `ConcurrentHashMap`.

```java
private final Map<Symbol, TrackedInstrument> instruments =
    new ConcurrentHashMap<>();
```

This provides:

- lock-free reads
- thread-safe insertions and removals
- safe iteration while the collection is being modified

Although instruments are currently loaded during startup, this design allows future features such as:

- dynamically adding instruments
- removing instruments
- updating simulation parameters at runtime

without redesigning the concurrency model.

---

### Thread-local Random Number Generation

Random price movements use:

```java
ThreadLocalRandom.current()
```

instead of a shared `Random` instance.

Benefits include:

- no contention between threads
- better scalability under parallel execution
- improved throughput in multi-threaded workloads

---

### Stateless Domain Service

`MarketSimulationService` is intentionally stateless apart from the concurrent instrument registry.

It does not maintain execution-specific state, allowing multiple scheduler threads or asynchronous execution strategies to invoke the service safely.

---

### Current Execution Model

Today the simulation is executed by a single Spring scheduler:

```
Scheduler Thread
        │
        ▼
tickAll()
        │
        ├── Instrument A
        ├── Instrument B
        ├── Instrument C
        └── ...
```

This guarantees deterministic ordering during each simulation cycle while keeping the implementation simple.

---

### Ready for Parallel Processing

Because each instrument protects its own state independently, the simulation could be parallelized with minimal changes.

Example:

```
                 tickAll()

          Parallel Processing

      Instrument A ─────► Thread 1
      Instrument B ─────► Thread 2
      Instrument C ─────► Thread 3
      Instrument D ─────► Thread 4
```

Since every instrument has its own synchronization boundary, there is no global lock that limits scalability.

---

### Kafka Publishing

Each generated market price is published independently.

Kafka message keys use the instrument symbol, ensuring that all updates for the same symbol are routed to the same partition and consumed in order, while allowing different symbols to be processed in parallel.

---

## Design Decisions

### Why Ports & Adapters?

The domain should not depend on infrastructure.

Instead of directly calling Kafka from business logic, the domain publishes through an outbound port.

This allows replacing Kafka with another messaging technology without modifying the business rules.

---

### Why a Scheduler Adapter?

Scheduling is considered infrastructure.

The domain exposes a `tickAll()` use case while Spring is responsible only for deciding **when** to execute it.

---

### Why Domain Events?

Price updates are business events.

Publishing them as domain events keeps the simulation logic independent from the communication mechanism.

---

### Why Kafka Partition by Symbol?

Using the symbol as the message key guarantees that all updates for the same instrument are delivered in order.

Without this, consumers could receive prices out of sequence.

---

## Technology Stack

- Java 21
- Spring Boot 3
- Spring Kafka
- Gradle
- Docker
- Clean Architecture
- Hexagonal Architecture
- Domain-Driven Design (DDD)

---

## Purpose

This project was created as a reference implementation for designing event-driven microservices using modern Java architecture principles, emphasizing separation of concerns, testability, and maintainability.