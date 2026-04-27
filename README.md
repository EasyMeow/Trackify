# Trackify

Project management app with a Kanban board and a Gantt timeline backed by the same tasks. See [`architecture.md`](architecture.md) for the design.

## Repository layout

This is a monorepo:

- [`docker-compose.yml`](docker-compose.yml) — local PostgreSQL 16 for business data and Spring Session storage.
- [`backend/`](backend/) — Spring Boot 3.4 (Java 21) API. JSON endpoints, business rules, persistence, sessions.
- [`frontend/`](frontend/) — React 19 + TypeScript SPA built with Vite.
- [`.env.example`](.env.example) — required environment variables. Copy to `.env` and fill in.

## Prerequisites

Install these once:

- **JDK 21** (e.g. Temurin 21). The Maven Wrapper resolves Maven itself, but the JDK must be on `PATH`.
- **Node.js 20+** and **npm 10+** for the SPA.
- **Docker** with Compose v2 for the local PostgreSQL container.

Verify:

```bash
java -version          # 21.x
node -v                # v20.x or newer
docker compose version # v2.x
```

## First-time local setup

Run from the repo root unless noted.

### 1. Configure environment

```bash
cp .env.example .env
```

The defaults in `.env.example` work out of the box for a local stack: frontend on `http://localhost:5173`, backend on `http://localhost:8080`, PostgreSQL on `localhost:5432` with database/user/password `trackify` / `trackify` / `changeme`. Change the password before exposing the stack to anything beyond your laptop.

`.env` is git-ignored. `.env.example` documents every variable the frontend, backend, database, session store, and auth layer expect. The complete list:

| Group | Variable | Purpose |
| --- | --- | --- |
| Frontend | `FRONTEND_ORIGIN` | Origin the SPA is served from. Used by backend CORS. |
| Frontend | `VITE_API_BASE_URL` | Base URL the SPA uses to call the backend. |
| Backend | `SPRING_PROFILES_ACTIVE` | Spring profile (`local` for dev). |
| Backend | `SERVER_PORT` | HTTP port the backend listens on. |
| PostgreSQL | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Container/database settings. |
| PostgreSQL | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | JDBC values Spring reads directly. |
| Session | `SPRING_SESSION_STORE_TYPE` | Where session state lives (`jdbc` keeps it in PostgreSQL). |
| Session | `SERVER_SERVLET_SESSION_TIMEOUT` | ISO-8601 inactivity timeout (e.g. `PT8H`). |
| Session | `SERVER_SERVLET_SESSION_COOKIE_NAME`, `_HTTP_ONLY`, `_SECURE`, `_SAME_SITE` | Cookie hardening. Use `true` and `strict` over HTTPS. |
| Auth | `AUTH_PASSWORD_ENCODER_STRENGTH` | BCrypt cost factor (10–12 typical). |
| Auth | `AUTH_PASSWORD_MIN_LENGTH` | Minimum password length enforced at registration. |
| Auth | `AUTH_BOOTSTRAP_ADMIN_LOGIN`, `_EMAIL`, `_PASSWORD` | Reserved for a future seed-admin runner. Currently bound but inert — see [Creating the first user](#creating-the-first-user). |

### 2. Start PostgreSQL

```bash
docker compose up -d db
docker compose ps        # wait until trackify-db reports (healthy)
```

The container is named `trackify-db`. Data persists in the named volume `trackify-db-data`, so `docker compose down` keeps your data; use `docker compose down -v` to wipe it.

### 3. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

The first run resolves Maven, downloads dependencies, then runs Flyway migrations against PostgreSQL before opening port `8080`. Logs should show `Successfully applied N migrations` and `Started TrackifyApplication`. Health probe:

```bash
curl http://localhost:8080/api/health
# {"status":"UP"}
```

Always use the wrapper (`./mvnw`) — never a global Maven install.

### 4. Start the frontend

In a second terminal:

```bash
cd frontend
npm install              # first time only
npm run dev
```

Open http://localhost:5173 in a browser. The SPA calls the backend at `VITE_API_BASE_URL` and sends the session cookie with every request (`credentials: 'include'`).

## Creating the first user

Trackify uses login/password authentication via Spring Security with server-side sessions stored in PostgreSQL (`SPRING_SESSION_*`, table `SPRING_SESSION`). The session cookie (`TRACKIFY_SESSION` by default) is HTTP-only; the SPA only knows that a session exists.

There is **no public registration endpoint or sign-up screen** in the MVP. The `AUTH_BOOTSTRAP_ADMIN_*` properties exist as configuration but no startup runner consumes them yet, so the only way to seed a user today is to insert one directly with a BCrypt hash.

1. Generate a BCrypt hash for the password you want. The simplest portable option is `htpasswd` from `apache2-utils` / `httpd-tools`:

   ```bash
   htpasswd -bnBC 10 "" 'mySecretPassword' | tr -d ':\n'
   ```

   The output should start with `$2y$10$` or `$2a$10$` — both are accepted by Spring Security's `BCryptPasswordEncoder`. The cost (`10`) must match `AUTH_PASSWORD_ENCODER_STRENGTH` in your `.env`.

2. Insert the row (replace the placeholders):

   ```bash
   docker exec -it trackify-db psql -U trackify -d trackify
   ```

   ```sql
   INSERT INTO users (login, email, password_hash, display_name)
   VALUES ('admin', 'admin@example.com', '$2a$10$REPLACE_WITH_REAL_HASH', 'Admin');
   ```

3. Sign in at http://localhost:5173. On first successful login the backend automatically creates a personal workspace owned by the user — every later login is a no-op.

`POST /api/auth/login` accepts `{ "login": "...", "password": "..." }`; `POST /api/logout` invalidates the session; `GET /api/me` returns the current user.

## Common commands

### Database

```bash
docker compose up -d db          # start
docker compose ps                # status / health
docker compose logs -f db        # tail logs
docker compose down              # stop (data persists)
docker compose down -v           # stop + wipe volume
```

### Backend (`backend/`)

```bash
./mvnw spring-boot:run                         # run on SERVER_PORT (default 8080)
./mvnw test                                    # full test suite
./mvnw -Dtest=ClassName test                   # one class
./mvnw -Dtest=ClassName#method test            # one method
./mvnw verify                                  # compile + tests + checks
./mvnw clean package                           # build the jar
```

JPA uses `ddl-auto: validate`; **all schema changes are Flyway migrations** under `backend/src/main/resources/db/migration/` as new `Vn__description.sql` files. Never edit a committed migration.

### Frontend (`frontend/`)

```bash
npm install
npm run dev      # Vite dev server on FRONTEND_ORIGIN (default 5173)
npm run build    # tsc -b && vite build
npm run lint     # ESLint
npm run preview  # serve the production build
```

TypeScript `strict` mode is on; do not disable it.

## Troubleshooting

- **Backend exits on startup with a Flyway error** — PostgreSQL is not reachable or the database is in an inconsistent state. Check `docker compose ps`; if the container is missing or unhealthy, restart it (`docker compose up -d db`). To start from a clean DB: `docker compose down -v && docker compose up -d db`.
- **`401` on every API call from the SPA** — the session cookie was not sent. Confirm `VITE_API_BASE_URL` matches the backend origin and that you are not running the SPA from a different origin without updating `FRONTEND_ORIGIN` (CORS). Cookies set with `SameSite=Lax` require both apps to share a registrable parent domain in production.
- **`Could not resolve dependency` on `npm install`** — delete `frontend/node_modules` and `frontend/package-lock.json`, then re-run `npm install`.
- **Login returns `INVALID_CREDENTIALS` immediately** — the `users` row is missing or the hash does not match what the BCrypt encoder produces. Re-do [Creating the first user](#creating-the-first-user) and confirm the hash starts with `$2a$10$` (matching `AUTH_PASSWORD_ENCODER_STRENGTH=10`).

## Production hardening notes

The defaults in `.env.example` are tuned for local dev. Before exposing the stack:

- Flip `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` and prefer `SAME_SITE=strict`.
- Replace every `changeme` / placeholder credential.
- Serve the SPA and the API behind the same TLS-terminating proxy so the session cookie can stay first-party.
- Set `AUTH_PASSWORD_ENCODER_STRENGTH` to 12 if request latency on login allows it.

## More

- Architecture and module boundaries: [`architecture.md`](architecture.md)
