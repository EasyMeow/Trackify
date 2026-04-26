# Trackify

Project management app with a Kanban board and a Gantt timeline backed by the same tasks. See [`architecture.md`](architecture.md) for the design.

## Repository layout

This is a monorepo:

- [`docker-compose.yml`](docker-compose.yml) — local PostgreSQL for development.
- [`backend/`](backend/) — Spring Boot (Java 21) API. JSON APIs, business rules, persistence, sessions. _Not yet scaffolded — see TASK-006._
- [`frontend/`](frontend/) — React + TypeScript SPA (Vite). _Not yet scaffolded — see TASK-013._
- [`.env.example`](.env.example) — required environment variables. Copy to `.env` and fill in.

## Bootstrap order

Start the stack from the bottom up. Each layer depends on the previous one being healthy.

1. **PostgreSQL** — from the repo root, run `docker compose up -d db` and wait for `docker compose ps` to report the `trackify-db` container as `healthy`. Stop with `docker compose down` (data persists in the named volume `trackify-db-data`); use `docker compose down -v` to wipe it.
2. **Backend** — once `backend/` is scaffolded, start it from `backend/` with `./mvnw spring-boot:run`. It connects to PostgreSQL using `SPRING_DATASOURCE_*` from your `.env` and serves the API on `SERVER_PORT` (default `8080`).
3. **Frontend** — once `frontend/` is scaffolded, start it from `frontend/` with `npm run dev`. It calls the backend at `VITE_API_BASE_URL` (default `http://localhost:8080`) and is served at `FRONTEND_ORIGIN` (default `http://localhost:5173`).

## Configuration

Copy [`.env.example`](.env.example) to `.env` at the repo root and fill in values. The same file documents every variable the frontend, backend, database, session store, and auth layer expect.

## More

- Architecture and module boundaries: [`architecture.md`](architecture.md)
- A full local-setup runbook will land with TASK-081.
