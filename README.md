# Book Lending Microservice

[![CI](https://github.com/mabdulloh/book_lending_lexhive/actions/workflows/ci.yml/badge.svg)](https://github.com/mabdulloh/book_lending_lexhive/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/mabdulloh/book_lending_lexhive/graph/badge.svg)](https://codecov.io/gh/mabdulloh/book_lending_lexhive)

REST microservice for managing a book catalog, library members, and loans. Built with Spring Boot 3.4 + Java 17, secured with JWT, persisted to PostgreSQL via Flyway, and exposed via Swagger UI at runtime.

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.4
- **Build**: Gradle 
- **Persistence**: Spring Data JPA + Hibernate, PostgreSQL 16
- **Migration**: Flyway
- **Validation**: Jakarta Validation (Hibernate Validator)
- **Security**: Spring Security with method-level authorization + JWT 
- **Observability**: Spring Boot Actuator (`/actuator/health`)
- **Documentation**: SwaggerUI (`/swagger-ui.html`)
- **Lombok**: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j` only
- **Testing**: JUnit 5, Mockito, Testcontainers

## Domain Model

- **Book**: title, author, ISBN (unique), totalCopies, availableCopies, soft delete
- **Member**: name, email (unique), linked to a `User`, soft delete
- **User**: username, passwordHash (BCrypt), role (`ADMIN`/`MEMBER`), optional FK to Member
- **Loan**: book (FK), member (FK), borrowedAt, dueDate, returnedAt

Public identifiers are UUIDs. Internal primary keys are sequential `Long`.

## API Endpoints

Base path: `/api/v1`. JWT Bearer required for all endpoints except `/auth/login`.

### Auth

| Method | Path                 | Auth | Body                   | Response  |
|--------|----------------------|------|------------------------|-----------|
| `POST` | `/api/v1/auth/login` | none | `{username, password}` | `{token}` |

### Books

| Method   | Path                   | Role          | Body                                 |
|----------|------------------------|---------------|--------------------------------------|
| `POST`   | `/api/v1/books`        | ADMIN         | `{title, author, isbn, totalCopies}` |
| `GET`    | `/api/v1/books`        | ADMIN, MEMBER | —                                    |
| `GET`    | `/api/v1/books/{uuid}` | ADMIN, MEMBER | —                                    |
| `PUT`    | `/api/v1/books/{uuid}` | ADMIN         | `{title?, author?, totalCopies?}`    |
| `DELETE` | `/api/v1/books/{uuid}` | ADMIN         | —                                    |

### Members

| Method   | Path                     | Role        | Body                      |
|----------|--------------------------|-------------|---------------------------|
| `POST`   | `/api/v1/members`        | ADMIN       | `{name, email, password}` |
| `GET`    | `/api/v1/members`        | ADMIN       | —                         |
| `GET`    | `/api/v1/members/{uuid}` | ADMIN, self | —                         |
| `DELETE` | `/api/v1/members/{uuid}` | ADMIN       | —                         |

### Loans

| Method | Path                            | Role          | Body / Query             | Notes                           |
|--------|---------------------------------|---------------|--------------------------|---------------------------------|
| `POST` | `/api/v1/loans`                 | MEMBER        | `{bookUuid, memberUuid}` | Borrowing rules enforced        |
| `POST` | `/api/v1/loans/{uuid}/return`   | ADMIN, MEMBER | —                        | Member must own loan (else 403) |
| `GET`  | `/api/v1/loans?memberId={uuid}` | ADMIN, MEMBER | —                        | Filter by member                |
| `GET`  | `/api/v1/loans/overdue`         | ADMIN         | —                        | Returns overdue loans           |

## Borrowing Rules

Configurable in `application.yaml` under `app.borrowing`:

| Key                           | Default | Description                      |
|-------------------------------|---------|----------------------------------|
| `max-active-loans-per-member` | `3`     | Max open loans per member        |
| `loan-duration-days`          | `14`    | Due date = `borrowedAt + N days` |

**Rules enforced**:

1. Member cannot exceed max active loans (default 3).
2. Member with at least one overdue loan cannot borrow new books.
3. Book with `availableCopies == 0` cannot be borrowed.

## Running with Docker Compose

```bash
docker compose up -d
```

Services:
- `postgres-db` — Postgres 16-alpine on port 5432
- `app` — Spring Boot app on port 8080

Override defaults via environment:

```bash
JWT_SECRET=please-override-this-with-32-plus-chars \
SEED_ADMIN_PASSWORD=secret \
docker compose up -d
```

Verify:

```bash
curl http://localhost:8080/actuator/health
```

## Local Development (without Docker)

Requires Java 17+.

```bash
# Start Postgres
docker compose up -d postgres-db

# Run app
./gradlew bootRun
```

App boots on `http://localhost:8080`. Default users seeded on startup:

| Username             | Password    | Role   |
|----------------------|-------------|--------|
| `admin`              | `admin123`  | ADMIN  |
| `member@example.com` | `member123` | MEMBER |

## Testing

```bash
./gradlew test                   # all unit + integration tests
```

Tests consist of unit test and integration test using TestContainers.

## API Reference

Interactive Swagger UI can be accessed when the app is running:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Click **Authorize** (top right) and paste a JWT obtained from `POST /api/v1/auth/login` to explore protected endpoints.

## Project Layout

```
src/
├── main/
│   ├── java/io/github/mabdulloh/booklending/
│   │   ├── BookLendingApplication.java
│   │   ├── config/      # SecurityConfig, DataSeeder, MemberSeeder, BorrowingRulesConfig
│   │   ├── controller/  # REST adapters (Book, Member, Loan, Auth)
│   │   ├── domain/      # JPA entities (Book, Member, Loan, User)
│   │   ├── dto/         # Records for request/response
│   │   ├── exception/   # Custom exceptions + GlobalExceptionHandler
│   │   ├── repository/  # Spring Data JPA repositories
│   │   ├── security/    # JWT service + filter
│   │   └── service/     # Business logic
│   └─ resources/
│       ├── application.yaml
│       └── db/migration/V1_20260923_initial_schema.sql
└── test/
    ├─ java/            # Service tests + controller integration + REST docs
    └─ resources/application-test.yml
```
