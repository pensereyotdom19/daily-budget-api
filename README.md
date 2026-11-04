# Daily Budget API

A Spring Boot backend for the [Daily Budget](https://github.com/pensereyotdom19/daily-budget) iPhone app.
It stores your plan, bills and expenses in a database, behind a login.

## Run it on your Mac

```bash
./mvnw spring-boot:run      # starts on http://localhost:8080
./mvnw test                 # runs the tests
```

The database is a file in `./data` (H2). Delete that folder to start empty.

Open http://localhost:8080/ for the web dashboard: sign in with your app account to see today's limit,
this month vs the plan, a month-by-month chart, spending by category and recent expenses. It is three
plain files in `src/main/resources/static` (no frameworks); the login token stays in the tab's sessionStorage.

Try it:

```bash
# 1. Create an account → you get a token
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"a-long-password"}'

# 2. Use the token on every other request
TOKEN=paste-token-here
curl -X PUT localhost:8080/api/expenses/$(uuidgen) -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"amountCents":125,"category":"coffee","note":"Iced latte","spentAt":"2026-11-04T01:00:00Z"}'
curl "localhost:8080/api/expenses/months/2026-11?zone=Asia/Phnom_Penh" -H "Authorization: Bearer $TOKEN"
```

## Endpoints

| Method | Path | What it does |
|---|---|---|
| POST | `/api/auth/register` | Create an account, returns a token |
| POST | `/api/auth/login` | Log in, returns a token |
| GET / PUT | `/api/settings` | Read / save the 6-month plan |
| GET | `/api/bills` | All bills |
| PUT / DELETE | `/api/bills/{id}` | Save / delete one bill |
| GET | `/api/expenses?from=…&to=…` | Expenses between two times, newest first |
| PUT / DELETE | `/api/expenses/{id}` | Save / delete one expense |
| GET | `/api/expenses/months/{2026-11}?zone=…` | Month totals: living, bills, engagement, per category |
| GET | `/api/sync?since=…` | Everything changed since the last sync, including deletions |
| GET | `/actuator/health` | Is the server up? (no login needed) |
| GET | `/` | Web dashboard (static page, no login needed to load it; data needs sign-in) |

Money is always whole cents: `125` = $1.25.

## How the code is organised

One folder per feature, each with the same layers. A request flows top to bottom:

```
Controller   receives HTTP + JSON, checks the input (@Valid), reads who is logged in
   ↓
Service      the rules (e.g. "you can only change your own expenses"), one database transaction
   ↓
Repository   talks to the database; Spring writes the SQL from method names
   ↓
Entity       a Java class that maps to one table row
```

| Folder | Contents |
|---|---|
| `auth/` | Register, login, JWT tokens, `SecurityConfig` (who may call what) |
| `user/` | The `users` table |
| `settings/` | The plan (one row per user) |
| `bill/` | Bills, with due and paid months in their own tables |
| `expense/` | Expenses, categories, month totals (a hand-written JPQL query) |
| `sync/` | One endpoint the phone calls to catch up |
| `resources/db/migration/` | The database tables, as SQL run by Flyway |

## Ideas in this project worth learning

- **Dependency injection**: classes ask for what they need in their constructor (`ExpenseService(ExpenseRepository repo, Clock clock)`) and Spring provides it.
- **DTOs vs entities**: `ExpenseRequest` / `ExpenseResponse` (records) are the JSON; `Expense` is the table. Keeping them apart means the database can change without breaking the app.
- **Validation**: `@Positive`, `@Pattern`, `@NotNull` on request records; bad input returns 400 automatically.
- **JWT login**: the server signs a token with a secret key. Every request sends it back, and the server checks the signature, so no session is stored.
- **Passwords**: only a BCrypt hash is saved.
- **Migrations**: the database structure lives in versioned SQL files, so every computer and server builds it the same way.
- **Idempotent PUT with client ids**: the phone makes the UUID, so retrying a request on bad internet never creates duplicates.
- **Soft delete + sync**: deleted rows keep a `deleted_at` time, so other devices can learn about the deletion.

## Deploying (free)

The `Dockerfile` builds and runs the server with the `prod` profile. It needs these environment variables:

| Variable | Example |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<host>/<db>?sslmode=require` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from your Postgres host |
| `JWT_SECRET` | 32+ random characters: `openssl rand -base64 48` |
| `REGISTRATION_ENABLED` | `true` to create your account, then set to `false` |

A free Postgres database: Neon or Supabase. A free place to run the Docker image: Render (sleeps when idle) or an Oracle Cloud Always Free VM.
