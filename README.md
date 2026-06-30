# Engineering Portfolio

This repository was created as part of a technical assessment to showcase my software engineering capabilities beyond coding. It includes artifacts that demonstrate my communication skills, technical expertise, and written communication through practical examples and real professional experiences.

The repository is organized according to the three evaluation areas.

---

# Repository Structure

```
engineering-portfolio/
│
├── communication-skills/
│   ├── README.md
│   ├── ai-productivity-speech.mp3
│
├── tech-skills/
│   ├── README.md
│   └── event-driven-pricing-platform/
│       ├── README.md
│       ├── market-simulator/
│       └── pricing-engine/
│
└── writing-skills/
    ├── README.md
```

---

# Repository Contents

## Communication Skills

This section contains a recorded presentation discussing the following topic:

> **What is the degree of impact of AI to software engineering productivity?**

It includes:

- Audio recording

The objective is to demonstrate the ability to communicate technical ideas clearly, organize complex concepts into a structured narrative, and present them effectively in English.

---

## Technical Skills

This section contains a complete event-driven microservices project designed to demonstrate software architecture, concurrent programming, and modern backend engineering practices.

The project consists of two independent microservices communicating asynchronously through Apache Kafka:

- **Market Simulator**
  - Generates synthetic market prices
  - Publishes market events
  - Demonstrates fine-grained synchronization and thread-safe mutable aggregates

- **Pricing Engine**
  - Consumes market events
  - Calculates pricing statistics
  - Publishes pricing snapshots
  - Demonstrates lock-free programming using immutable state and Compare-And-Set (CAS)

### Production Readiness

Given additional time, I would incorporate several production-grade capabilities to improve resilience, reliability and observability.

#### Resilience

- **Retry policies** with exponential backoff for Kafka producers and consumers.
- **Circuit Breakers** (Resilience4j) to protect Kafka producers from cascading failures and to temporarily pause message processing when downstream dependencies become unavailable.
- **Timeouts** and **Bulkheads** to isolate failures and prevent resource exhaustion.

#### Reliable Messaging

- **Dead Letter Queue (DLQ)** for messages that cannot be processed after multiple retry attempts.
- **Outbox Pattern** in the Pricing Engine to guarantee atomicity between state changes and event publication, avoiding message loss in case of failures.

#### Observability

- Structured logging with correlation IDs to simplify troubleshooting across services.

#### Persistence

- Replace the in-memory snapshot store with **PostgreSQL**.

---

## Writing Skills

This section contains written responses describing real experiences from my professional career.

Topics include:

- An unconventional career path and the lessons learned
- A situation that required relentless focus and perseverance
- Joining a complex engineering environment and becoming effective in a short period of time

The objective is to demonstrate written communication, self-reflection, structured thinking, and problem-solving.

---

# Purpose

The purpose of this repository is to demonstrate not only technical implementation skills, but also software architecture, communication, technical writing, and engineering decision-making.

Each artifact was intentionally created to showcase different competencies expected from a senior software engineer, including system design, concurrent programming, clean code, architectural thinking, and effective communication.