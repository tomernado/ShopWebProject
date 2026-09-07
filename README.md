# Store Chain Management System (Java, Client-Server)

Course project (HIT college) — a Java client-server system for managing a chain of stores: employee authentication and account management, per-branch inventory and sales, a polymorphic customer purchase flow (New/Returning/VIP), inter-branch employee chat with a queue-based free-employee lookup, system logging, and JSON/Word sales reporting.

## Features

- **Authentication & accounts** — login over sockets, password policy enforcement, manager-only account creation.
- **Customers** — abstract `Customer` class with `NewCustomer` / `ReturningCustomer` / `VipCustomer` subclasses, each applying its own discount via polymorphism.
- **Inventory & sales** — per-branch product catalog and inventory, sale recording, a network-wide customer directory.
- **Chat** — inter-branch employee chat with a queue-based lookup for free employees to join a session.
- **Reports** — sales aggregation by branch/product/category, exported to JSON (Gson) and Word (Apache POI).
- **Logging** — system-wide event log, viewable from the client (manager-only).
- **GUI** — Swing desktop client: login, dashboard, purchases, customers, employees, reports, and logs panels.

## Project Structure

```
src/main/java/
  model/     Employee, Branch, Product, Inventory, Customer hierarchy
  server/    Socket server, auth/account/sale services, request-response handlers
  client/    Swing GUI (MainFrame + panels) and the server connection layer
  chat/      Chat sessions, requests/responses, the free-employee queue dispatcher
  reports/   Report generation and JSON/Word export
  logging/   System event logger
src/test/java/   JUnit 5 test suite mirroring the structure above
```

## Stack

Java 17, Maven, JUnit 5, Gson, Apache POI (`poi-ooxml`), Swing, plain `java.net` sockets.

## Running

Build and test:

```
mvn test
```

Then run `server.Main` and `client.Main` from your IDE, or from the terminal (in separate windows):

```
mvn org.codehaus.mojo:exec-maven-plugin:3.1.1:java -Dexec.mainClass=server.Main -Dexec.classpathScope=test
mvn org.codehaus.mojo:exec-maven-plugin:3.1.1:java -Dexec.mainClass=client.Main -Dexec.classpathScope=test
```

The server listens on port `5000`; the client connects to `localhost:5000`.
