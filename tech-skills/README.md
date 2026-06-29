# High-Performance Event-Driven Pricing Platform

A reference implementation demonstrating **high-throughput concurrent processing**, **thread-safe domain modeling**, and **event-driven microservice architecture** using Java 21, Spring Boot, Apache Kafka, and Clean Architecture.

Rather than focusing on business complexity, this project focuses on **engineering challenges** commonly found in real-time financial systems:

* Concurrent state updates
* Thread safety
* Lock-free algorithms
* Event-driven communication
* Domain isolation
* Horizontal scalability

The project is intentionally designed as a learning resource and architectural reference for modern backend systems.

---

# Motivation

Financial systems continuously receive price updates from multiple sources.

As throughput increases, applications must safely process thousands of events concurrently while preserving data consistency and maximizing throughput.

This project demonstrates two complementary concurrency strategies:

* **Fine-grained synchronization**, where each domain aggregate protects its own mutable state.
* **Lock-free state management**, using immutable objects and Compare-And-Set (CAS).

Together they illustrate different approaches to building highly concurrent Java applications.

---

# Project Components

```
pricing-platform/

│
├── market-simulator/
│
│     Generates synthetic market prices
│     Publishes market events
│
└── pricing-engine/
      Consumes market events
      Maintains pricing snapshots
      Publishes enriched pricing data
```

---

# Overall Architecture

```
                +----------------------+
                |   Market Simulator   |
                +----------+-----------+
                           |
                           |
                 market.price.updated
                           |
                     Apache Kafka
                           |
                           ▼
                +----------+-----------+
                |    Pricing Engine    |
                +----------+-----------+
                           |
                price.snapshot.updated
                           |
                    Downstream Systems
```

The services communicate asynchronously through Kafka, remaining completely independent from one another.

---

# Component Responsibilities

## Market Simulator

The Market Simulator generates realistic market price movements for configurable financial instruments.

Responsibilities include:

* simulating market ticks
* maintaining the latest price for each instrument
* publishing market events
* exposing tracked symbols via REST

Its main engineering goal is demonstrating **thread-safe mutable aggregates**.

Each instrument owns its internal state and synchronizes updates independently, eliminating the need for global locks.

---

## Pricing Engine

The Pricing Engine consumes market events and maintains the latest pricing snapshot for every instrument.

For every received event it:

* updates current price
* calculates moving statistics
* updates volatility
* computes percentage change
* increments tick counters
* publishes an enriched snapshot

Instead of synchronization, it uses **immutable state combined with Compare-And-Set (CAS)** to achieve lock-free updates.

---

# End-to-End Flow

```
Scheduler

      │

      ▼

Generate Price

      │

      ▼

Market Event

      │

      ▼

Kafka

      │

      ▼

Pricing Engine

      │

      ▼

Update Snapshot

      │

      ▼

Publish Snapshot Event

      │

      ▼

Consumers
```

---

# Why This Project is a Good Concurrency Example

Many concurrency examples focus on simple counters or producer-consumer problems.

Real-world systems are considerably more challenging because they must:

* protect mutable state
* process events concurrently
* minimize contention
* scale horizontally
* preserve consistency
* remain maintainable

This project demonstrates these challenges using realistic business scenarios.

---

# Thread Safety Strategies

The project intentionally showcases two different approaches.

## Strategy 1 — Fine-Grained Synchronization

Used by **Market Simulator**.

Each financial instrument owns its own synchronization boundary.

```
Instrument A

 synchronized

Instrument B

 synchronized

Instrument C

 synchronized
```

Advantages:

* simple reasoning
* strong consistency
* independent locking
* minimal contention

No global lock exists.

Different instruments can be processed simultaneously.

---

## Strategy 2 — Lock-Free Updates

Used by **Pricing Engine**.

Each pricing snapshot is stored as an immutable object inside an `AtomicReference`.

Updates use Compare-And-Set (CAS):

```
Read State

      │

Compute New State

      │

compareAndSet()

      │
 Success?
      │

Retry if necessary
```

Advantages:

* no blocking
* no monitor contention
* excellent scalability
* consistent immutable snapshots

---

# Concurrency Design Principles

Several principles were intentionally applied throughout the project.

## Stateless Services

Business services contain no mutable execution state.

This allows multiple threads to invoke the same service safely.

---

## Aggregate Ownership

Each aggregate owns its own mutable data.

Instead of protecting an entire collection with one lock, synchronization is isolated to each business entity.

This dramatically reduces contention.

---

## Concurrent Collections

Shared registries use `ConcurrentHashMap`.

Benefits include:

* thread-safe access
* atomic initialization
* scalable reads
* minimal locking

---

## Immutable Domain State

Whenever practical, mutable state is replaced by immutable objects.

Readers always observe consistent snapshots.

No defensive copies are required.

---

## Event-Driven Communication

Services never invoke one another directly.

All communication occurs through Kafka events.

Benefits include:

* loose coupling
* independent deployment
* horizontal scalability
* asynchronous processing

---

# Design Patterns

The project combines several architectural and concurrency patterns.

## Architectural Patterns

* Clean Architecture
* Hexagonal Architecture (Ports & Adapters)
* Domain-Driven Design (DDD)
* Event-Driven Architecture
* Microservices

---

## Messaging Patterns

* Domain Events
* Publish / Subscribe
* Event Streaming

---

## Concurrency Patterns

* Fine-grained locking
* Lock-free programming
* Compare-And-Set (CAS)
* Immutable objects
* Atomic references
* Thread-safe collections
* Thread confinement
* Stateless services

---

# Technologies

* Java 21
* Spring Boot 3
* Spring Kafka
* Apache Kafka
* Gradle
* Docker
* REST APIs

---

# Scalability

The architecture is designed to evolve toward production-grade deployments.

Potential enhancements include:

* Kafka Consumer Groups
* Virtual Threads
* Parallel processing
* Outbox Pattern
* Retry policies
* Dead Letter Queue
* OpenTelemetry
* Micrometer
* PostgreSQL persistence
* Redis caching
* Kubernetes deployment

---

# Learning Objectives

This repository demonstrates how to:

* design thread-safe domain models
* build lock-free business components
* process Kafka events concurrently
* isolate business logic from infrastructure
* build scalable event-driven services
* apply Clean Architecture in real-world microservices
* compare synchronization versus CAS-based concurrency

---

# Running the Project

## Start the Infrastructure

```bash
docker-compose up --build
```

---

## Verify the Flow

You can also verify the system using the REST endpoints exposed by each service.

### Market Simulator

Retrieve tracked instruments:

```
GET /api/v1/market-simulator/symbols
```

### Pricing Engine

Retrieve all pricing snapshots:

```
GET /api/v1/pricing-engine/snapshots
```

Retrieve a specific instrument:

```
GET /api/v1/pricing-engine/snapshots/{symbol}
```

---

# Purpose

This project was created as a practical reference for software engineers interested in concurrent programming, event-driven systems, and modern Java architecture.

Rather than presenting isolated concurrency examples, it demonstrates how thread safety, scalability, and clean architectural principles can be applied together in a realistic distributed system.
