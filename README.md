# GitHub Repository Searcher

Spring Boot REST API that searches GitHub repositories, upserts results into PostgreSQL, and lets you retrieve stored repos with filters.

No UI — use Swagger or curl/Postman.

## Features

- Search GitHub via `https://api.github.com/search/repositories`
- Persist repository details in PostgreSQL (upsert by GitHub repo id)
- Retrieve stored results with optional `language`, `minStars`, and `sort`
- Validation + structured error responses (rate limits, bad input, etc.)
- Swagger UI / OpenAPI docs
- Unit + integration tests with JaCoCo coverage on business logic

## Tech stack

- Java 17
- Spring Boot 4.1
- Spring Data JPA + PostgreSQL
- RestClient (GitHub API)
- springdoc-openapi
- JUnit 5, Mockito, H2 (tests)
- Docker Compose (local Postgres)

## Prerequisites

- JDK 17+
- Docker Desktop (local Postgres)
- Optional: GitHub personal access token (higher API rate limits)

## Quick start

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Optional: local GitHub token (gitignored)
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# edit application-local.yml and paste your token

# 3. Run the app
./mvnw spring-boot:run
```

| Resource | URL |
|----------|-----|
| App | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |

Stop Postgres when done: `docker compose down`

## API

### Search and save

`POST /api/github/search`

```bash
curl -X POST http://localhost:8080/api/github/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "spring boot",
    "language": "Java",
    "sort": "stars"
  }'
```

| Field | Required | Values |
|-------|----------|--------|
| `query` | Yes | Search text |
| `language` | No | e.g. `Java`, `Python` |
| `sort` | No | `stars`, `forks`, `updated` |

### Retrieve stored repositories

`GET /api/github/repositories`

```bash
curl "http://localhost:8080/api/github/repositories?language=Java&minStars=100&sort=stars"
```

| Param | Required | Values |
|-------|----------|--------|
| `language` | No | Filter by language |
| `minStars` | No | Minimum stars (≥ 0) |
| `sort` | No | `stars` (default), `forks`, `updated` |

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/github_searcher` | JDBC URL |
| `DATABASE_USERNAME` | `postgres` | DB user |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `GITHUB_TOKEN` | _(empty)_ | Optional GitHub token |
| `GITHUB_API_BASE_URL` | `https://api.github.com` | GitHub API base |
| `SERVER_PORT` | `8080` | HTTP port |

Local token tip: use gitignored `application-local.yml` (see example file). Never commit secrets.

## Tests

```bash
./mvnw test
```

Coverage report: `target/site/jacoco/index.html`

JaCoCo enforces 100% line/branch coverage on business logic (`controller`, `service`, `client`, exception handler). Data holders (`dto`, `entity`, `config`, etc.) are excluded.

## Project structure

```text
src/main/java/com/assignment/githubsearcher/
  client/        GitHub REST client
  config/        OpenAPI config
  controller/    REST endpoints
  dto/           Request/response models
  entity/        JPA entity
  exception/     Error handling
  repository/    Spring Data repository
  service/       Business logic
```

