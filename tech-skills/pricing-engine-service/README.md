# Pricing Engine

Pricing Engine is an event-driven Spring Boot microservice responsible for maintaining the latest pricing snapshot of financial instruments.

The service consumes market price events from Kafka, updates an in-memory pricing model, calculates market statistics, and publishes enriched price snapshots for downstream consumers.

The project is intentionally built using **Clean Architecture (Ports & Adapters)** and **Domain-Driven Design (DDD)** to demonstrate a scalable, thread-safe, event-driven service.

---

# Architecture

```
                    Kafka
        market.price.updated
                     │
                     ▼
        +--------------------------+
        | Kafka Consumer Adapter   |
        +------------+-------------+
                     │
               Inbound Port
                     │
        +------------▼-------------+
        | Pricing Service          |
        | Business Logic           |
        +------------+-------------+
                     │
      +--------------+--------------+
      |                             |
      ▼                             ▼
Price Snapshot Store        Domain Event Publisher
      │                             │
      │                      Kafka Adapter
      │                             │
      ▼                             ▼
 In-Memory Store        price.snapshot.updated
```

The domain is completely independent from Spring, Kafka and persistence technologies.

---

# Responsibilities

The Pricing Engine is responsible for:

- Consuming market price events
- Maintaining the latest state of each financial instrument
- Calculating:
    - Current price
    - Average price
    - Percentage change
    - Volatility
    - Tick count
- Publishing enriched pricing snapshots
- Serving the latest snapshots through REST APIs

---

# Features

- Kafka consumer
- Kafka producer
- Event-driven architecture
- Clean Architecture
- Hexagonal Architecture
- Domain-Driven Design
- Lock-free price updates
- Thread-safe processing
- REST API
- Java 21
- Spring Boot 3

---

# Project Structure

```
src/main/java

api
 ├── controller
 ├── mapper
 └── advice

domain
 ├── model
 ├── event
 ├── service
 └── port
      ├── in
      └── out

infrastructure
 ├── messaging
 │      ├── consumer
 │      ├── publisher
 │      ├── mapper
 │      └── config
 └── persistence
```

---

# Processing Flow

```
Market Simulator

        │
        ▼

market.price.updated

        │
        ▼

Kafka Consumer

        │
        ▼

Pricing Service

        │
        ▼

Price Snapshot

        │
        ├── Update current price
        ├── Update average
        ├── Calculate volatility
        ├── Calculate change %
        └── Increment tick count

        │
        ▼

PriceSnapshotUpdated Event

        │
        ▼

Kafka Publisher

        │
        ▼

price.snapshot.updated
```

---

# REST API

## Get all snapshots

```
GET /api/v1/pricing-engine/snapshots
```

Example response

```json
[
  {
    "symbol": "AAPL",
    "currentPrice": 203.18,
    "averagePrice": 202.45,
    "changePercent": 0.41,
    "volatility": 1.87,
    "tickCount": 153
  }
]
```

---

## Get snapshot by symbol

```
GET /api/v1/pricing-engine/snapshots/{symbol}
```

Example

```
GET /snapshots/AAPL
```

---

# Kafka

## Consumed Topic

```
market.price.updated
```

Example message

```json
{
  "symbol": "AAPL",
  "price": 203.18,
  "timestamp": "2026-06-28T20:15:31Z"
}
```

---

## Published Topic

```
price.snapshot.updated
```

Example

```json
{
  "symbol": "AAPL",
  "currentPrice": 203.18,
  "averagePrice": 202.45,
  "changePercent": 0.41,
  "volatility": 1.87,
  "tickCount": 153,
  "timestamp": "2026-06-28T20:15:31Z"
}
```

---

# Concurrency Design

The Pricing Engine was designed to safely process multiple Kafka messages concurrently without introducing global locks.

## Stateless Service

`PricingService` contains no mutable state.

All application state is stored inside the repository (`PriceSnapshotStore`) and within each `PriceSnapshot`.

This allows multiple Kafka consumer threads to invoke the service simultaneously.

---

## Concurrent Snapshot Registry

Snapshots are stored using a `ConcurrentHashMap`.

Creation is performed through:

```java
computeIfAbsent(...)
```

This guarantees that only one snapshot instance exists for each symbol, even when multiple messages for a new instrument arrive simultaneously.

Benefits:

- atomic creation
- lock-free reads
- thread-safe access
- scalable under concurrent workloads

---

## Lock-Free Snapshot Updates

Each `PriceSnapshot` owns its internal mutable state.

Instead of using synchronized blocks, updates rely on an `AtomicReference<State>` with a Compare-And-Set (CAS) loop.

Conceptually:

```
Read State

      │

Compute New State

      │

compareAndSet()

      │
 ┌────┴────┐
 │ Success │──► Done
 └────┬────┘
      │
      ▼
 Retry
```

Advantages:

- no blocking
- no monitor contention
- excellent scalability under concurrent updates
- linearizable state transitions

---

## Immutable State

Each update creates a brand new immutable `State` object.

The previous state is never modified.

This approach eliminates partial updates and guarantees readers always observe a consistent snapshot.

Benefits:

- thread safety
- easier reasoning
- no defensive copying
- lock-free reads

---

## Kafka Parallelism

Kafka naturally distributes partitions across consumer threads.

Because every symbol owns an independent `PriceSnapshot`, different instruments can be processed concurrently without contention.

The design scales horizontally as partitions or consumer instances increase.

---

## Event Publication

After a successful update, the service publishes a `PriceSnapshotUpdated` domain event.

The business logic remains independent of Kafka by publishing through an outbound port.

This enables replacing Kafka with another messaging technology without changing the domain.

---

# Why CAS Instead of synchronized?

Traditional synchronization serializes competing threads.

```
Thread A
      │
      ▼
 synchronized
      ▲
      │
Thread B waits
```

With Compare-And-Set:

```
Thread A

Thread B

Both compute independently

Only conflicting updates retry.
```

This reduces contention and generally provides better throughput in highly concurrent environments.

---

# Running

Requirements

- Java 21
- Gradle
- Kafka

Build

```bash
./gradlew clean build
```

Run

```bash
./gradlew bootRun
```

---

# Docker

Build

```bash
docker build -t pricing-engine .
```

Run

```bash
docker run \
-e KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
-p 8080:8080 \
pricing-engine
```

---

# Technology Stack

- Java 21
- Spring Boot 3
- Spring Kafka
- Gradle
- Docker
- Clean Architecture
- Hexagonal Architecture
- Domain-Driven Design (DDD)

---

# Purpose

This project demonstrates how to build a high-throughput, event-driven pricing service capable of safely processing concurrent market updates using modern Java concurrency techniques, immutable domain models, and a clean separation between business logic and infrastructure.