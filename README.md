# unq-desarollo-de-aplicaciones-f

A Spring Boot backend that exposes a football (soccer) API with JWT-based authentication and a reactive endpoint to retrieve team squads by scraping public data sources.

## Project Objective

- Provide an authenticated REST API to obtain football team squad information.
- Integrate a scraping service that locates a team on WhoScored and extracts the current squad and basic player stats.
- Offer a foundation for future integrations with external football APIs (a `WebClient` is preconfigured for football-data.org).

## Tech Stack

- Java 17
- Spring Boot 3.5.5
  - Spring WebFlux (Reactive)
  - Spring Security Reactive (JWT)
  - Spring Data JPA
- H2 Database (embedded)
- JSON Web Token (jjwt 0.12.3)
- Lombok
- Jsoup (HTML parsing/scraping)
- springdoc-openapi WebFlux (Swagger UI)
- Gradle (Wrapper included)
- JUnit 5 + JaCoCo
- GitHub Actions CI

## Current Architecture & Technical Details

- Package root: `unq.desapp.futbol`
- Entry point: `FutbolProjectApplication` with OpenAPI definition.
- **Fully reactive architecture** built with Spring WebFlux
- Security:
  - Reactive, stateless JWT-based authentication using Spring Security WebFlux
  - All requests require authentication except those under `/auth/**`
  - CORS enabled for all origins and standard HTTP methods
  - Users are persisted in H2 database (see Database section)
  - Authentication components:
    - `ReactiveJwtAuthenticationManager`: Validates JWT tokens reactively
    - `ReactiveUserPasswordAuthenticationManager`: Authenticates username/password for login
    - `ReactiveJwtAuthenticationConverter`: Extracts JWT from request headers
    - `ReactiveAuthenticationEntryPoint`: Handles authentication errors
- Configuration (`src/main/resources/application.properties`):
  - `spring.application.name=futbol-project`
  - `football.api.baseurl` and `football.api.token` are available for future HTTP API calls
  - JWT settings are externalizable via env vars with defaults:
    - `JWT_SECRET_KEY` (base64-encoded HMAC key)
    - `JWT_EXPIRATION` (milliseconds; default 604800000 = 7 days)
- Database:
  - **H2 Database** (embedded, file-based) for user persistence
  - Database file location: `./data/futbol-db`
  - H2 Console enabled at `/h2-console` for development
    - JDBC URL: `jdbc:h2:file:./data/futbol-db`
    - Username: `sa`
    - Password: (empty)
  - JPA/Hibernate configured with `ddl-auto=update` (auto-creates/updates schema)
  - Users persist between application restarts

### REST API

- Auth
  - `POST /auth/login`
    - Request body:

      ```json
      { "email": "user@example.com", "password": "password" }
      ```

    - Response body:

      ```json
      { "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 604800000 }
      ```

- Teams
  - `GET /teams/{country}/{name}/squad`
    - Description: Returns the squad for the given team by scraping WhoScored. The `{name}` segment accepts hyphen-separated names and is normalized (e.g., `manchester-united` -> `manchester united`).
    - Auth: Requires `Authorization: Bearer <jwt>`
    - Response: `List<Player>` with fields:

      ```json
      {
        "name": "string",
        "age": 0,
        "nationality": "string",
        "position": "string",
        "rating": 0.0,
        "matches": 0,
        "goals": 0,
        "assist": 0,
        "redCards": 0,
        "yellowCards": 0
      }
      ```

### Notes

- The entire application is built using reactive programming with Spring WebFlux
- All endpoints return reactive types (`Mono<ResponseEntity<>>`) for non-blocking I/O
- The scraping implementation (`ScrapingServiceImpl`) parses WhoScored pages and extracts player data from embedded scripts. It runs on a bounded elastic scheduler and returns a `Mono<List<Player>>`
- The `FootballDataServiceImpl` currently delegates to scraping. A reactive `WebClient` is initialized with `football.api.baseurl` and `football.api.token` for future use
- Security is fully reactive using Spring Security WebFlux filters and managers

### OpenAPI / Swagger

- OpenAPI is enabled via `springdoc-openapi` and an `@OpenAPIDefinition` in `FutbolProjectApplication`.
- Swagger UI is typically available at `/swagger-ui.html` or `/swagger-ui/index.html`.
- Given current security rules, Swagger endpoints may require authentication; adjust `SecurityConfig` if you want them public.

## Build, Test, Coverage, CI

- **Build** with Gradle Wrapper:
  - `./gradlew build -x test` (skips tests to avoid redundancy in CI)

- **Test Execution Strategy**:
  The project uses **JUnit 5 Tags** to classify tests into two profiles:

  1. **Unit Tests** (`@Tag("unit")`): Fast, isolated tests using mocks. Run on every PR.
     - Command: `./gradlew unitTest`
  2. **E2E Tests** (`@Tag("e2e")`): Slower integration tests (Database, WireMock). Run only on PRs to `main`.
     - Command: `./gradlew e2eTest`
  3. **All Tests**:
     - Command: `./gradlew test` (or simply `./gradlew unitTest e2eTest`)

- **Coverage with JaCoCo**:
  - Report: `./gradlew jacocoTestReport` (collects data from any executed test task)
  - Verification (min 80%): `./gradlew jacocoTestCoverageVerification`
  - HTML report output: `build/reports/jacoco/test/html/`

- **CI/CD (GitHub Actions)**:
  - **Unit Tests Job**: Runs on **ALL** Pull Requests.
  - **E2E Tests Job**: Runs **ONLY** on Pull Requests targeting `main`.
  - **Coverage Job**: Consolidates results from executed jobs and verifies >80% coverage.

## Run Locally

1. Java 17 is required.
2. Set environment variables as needed (or override properties in `application.properties`):
   - `JWT_SECRET_KEY` (base64-encoded)
   - `JWT_EXPIRATION` (optional)
   - `FOOTBALL_API_TOKEN` (optional)
3. Start the app:
   - `./gradlew bootRun`

### Quickstart (cURL)

1. Login to obtain a token:

   ```bash
   curl -s -X POST http://localhost:8080/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"user@example.com","password":"password"}'
   ```

2. Call the squads endpoint (replace TOKEN):

   ```bash
   curl -s http://localhost:8080/teams/England/manchester-united/squad \
     -H 'Authorization: Bearer TOKEN'
   ```

## Limitations & Next Steps

- Scraping depends on the structure of external pages and may break if the target site changes.
- Swagger endpoints are currently protected; consider allowing them for easier exploration in development.
- Extend `FootballDataService` to use the configured external API when needed.
