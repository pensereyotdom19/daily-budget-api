# Daily Budget API – notes for Claude Code

Backend for the Daily Budget iPhone app (repo: ../daily-budget). The owner is learning Spring Boot with this project,
so keep code plain and commented, and explain new concepts when adding them.

- Stack: Java 21, Spring Boot 4.1, Maven (use ./mvnw), Spring Data JPA, Flyway, Spring Security with JWT.
- Database: H2 file on the Mac and in-memory in tests (PostgreSQL mode); PostgreSQL in production (profile `prod`).
- Database changes only through new Flyway files `src/main/resources/db/migration/V<n>__*.sql`. Never edit one that has run.
- Money is integer cents (BIGINT / long). Month keys look like "2026-11". Categories use the app's raw names ("skinCare").
- Ids are UUIDs made by the phone; PUT is create-or-replace. Deletes are soft (deleted_at) so /api/sync can report them.
- Every query is filtered by the logged-in user's id. Every endpoint gets a test in ApiTest.
- No secrets in code: production reads DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET from the environment.
- Ask before adding libraries outside Spring Boot's starters.
