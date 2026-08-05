# Store Chain Management System (Java, Client-Server)

Course project (HIT college) — a Java client-server system for managing a chain of stores: employee auth, per-branch inventory, a polymorphic customer purchase flow (New/Returning/VIP), inter-branch employee chat with a queue-based free-employee lookup, and JSON/Word reporting.

## Status

Design phase. No code yet — see the specs below for what's planned and why.

## Docs

- [Project overview & full requirements](docs/superpowers/specs/2026-08-05-store-chain-system-overview-design.md) — the complete assignment requirements, the 5-stage breakdown, and the code-simplicity constraint that applies to every stage.
- [Stage 1 — Entities design](docs/superpowers/specs/2026-08-05-stage1-entities-design.md) — the current stage: Employee, Branch, Product, Inventory, and the Customer class hierarchy.

## Stack

Java, Maven, JUnit, Swing (GUI stage), plain `java.net` sockets (server stage).
