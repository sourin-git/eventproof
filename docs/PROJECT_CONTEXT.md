# EventProof

EventProof is a reliability-testing platform for event-driven applications.

## Current Phase

University Review 1.

## Current Technology Stack

- Java 25
- JavaFX
- Maven
- PostgreSQL
- JDBC

## Current Architecture

JavaFX
→ Controller
→ Service
→ DAO
→ JDBC
→ PostgreSQL

## Review 1 Features

1. Create projects.
2. Register events belonging to projects.
3. Create reliability experiments.
4. Support:
   - NORMAL
   - DUPLICATE
   - DROP
5. Generate an event execution sequence.
6. Save experiments and executions in PostgreSQL.
7. Display experiment results and event timeline.

## Future Technologies

These are NOT part of Review 1:

- Spring Boot
- Kafka
- Redis
- React
- Docker
- Microservices

They may be introduced after Review 1.

## Development Rules

- Do not overengineer.
- Do not introduce Spring or Spring Boot.
- Do not use Hibernate/JPA.
- Use plain JDBC.
- Use PreparedStatement.
- Keep SQL out of JavaFX controllers.
- Keep business logic out of JavaFX controllers.
- Prefer simple code understandable by a second-year CSE student.
- Keep architecture capable of evolving into Spring Boot + Kafka later.