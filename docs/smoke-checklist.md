# Trackify MVP Smoke Checklist

Manual acceptance pass for the MVP. Run end-to-end on a clean local environment after the
README runbook has been followed (Postgres up, backend on `:8080`, frontend on `:5173`,
at least one user seeded per the README "Creating the first user" section).

Tick each box as you complete the step. If a step fails, stop and record the first failing
step in `progress.txt` along with the observed behaviour — that is the input for TASK-083.

> Conventions
> - All API calls below assume the backend at `http://localhost:8080` and the frontend at
>   `http://localhost:5173`. Adjust origins if `.env` overrides them.
> - "Reload" means a full browser refresh (`Cmd/Ctrl+R`), not a SPA navigation.
> - Devtools steps assume Chromium-based browsers (Chrome, Edge, Brave); equivalents exist
>   in Firefox/Safari.

---

## 0. Pre-flight

- [ ] `docker compose ps` shows `trackify-db` `healthy`.
- [ ] `curl -s http://localhost:8080/api/health` returns HTTP 200 with a JSON payload.
- [ ] `http://localhost:5173/` loads without a console error.
- [ ] At least one user row exists in `users` (verified via `docker exec -it trackify-db
      psql -U trackify -d trackify -c 'select login from users;'`).

## 1. Authentication

- [ ] Visiting `http://localhost:5173/` while signed out redirects to `/sign-in`.
- [ ] Submitting wrong credentials shows an inline error and does **not** redirect.
- [ ] Submitting valid credentials redirects to `/` and the dashboard renders.
- [ ] After login, `GET /api/me` (in devtools Network) returns 200 with the signed-in
      user's `login`, `email`, `displayName`.
- [ ] A `SESSION` cookie is present in devtools (Application → Cookies → `localhost:5173`),
      `HttpOnly` true.
- [ ] Hard reload of `/` keeps you signed in (no redirect to `/sign-in`).
- [ ] The sidebar shows the signed-in user's `displayName` and `login`, plus a
      visible "Sign out" button.
- [ ] Clicking "Sign out" calls `POST /api/logout` (200) and the SPA redirects to
      `/sign-in`. Reloading any protected route stays on `/sign-in`.

## 2. Workspace load

- [ ] After first login, the sidebar `WorkspaceSwitcher` shows exactly one workspace
      (the auto-created personal workspace).
- [ ] `GET /api/workspaces` returns that workspace as the only entry for this user.
- [ ] Logging in a second time does **not** create a duplicate workspace
      (re-check `select count(*) from workspaces where owner_id = <userId>;` returns 1).
- [ ] If two users exist, signing in as user B shows only B's workspace, never A's.

## 3. Project creation

- [ ] Dashboard shows an "Empty" state when the workspace has no projects.
- [ ] The create-project control on the dashboard accepts a name, submits, and the new
      project appears in the list **without** a manual reload.
- [ ] The new project still appears after a full browser reload.
- [ ] Creating a project with an empty/whitespace name is rejected (inline error or
      disabled submit; no row is inserted).
- [ ] Each created project gets exactly three default board columns in order:
      `Todo`, `In Progress`, `Done` (verify via the board page or
      `select name, position from board_columns where project_id = '<id>' order by position;`).
- [ ] Clicking a project from the dashboard navigates to `/projects/<id>/board`.

## 4. Board usage

### Read

- [ ] `/projects/<id>/board` renders the three default columns left to right.
- [ ] Empty columns render an empty state (no crash, readable copy).
- [ ] `GET /api/projects/<id>/board` (Network) returns columns + tasks.

### Create task

- [ ] The "Add task" control inside a column accepts a title and creates a card
      immediately on submit (no full reload needed).
- [ ] The created task appears in the column it was added to.
- [ ] After a full reload the task is still there in the same column.

### Drag and drop

- [ ] Dragging a card to a different column visually moves it as you drag.
- [ ] Dropping a card in a new column updates its position immediately (optimistic).
- [ ] The new column persists after a full reload.
- [ ] Dragging a card within the **same** column to a new index persists after reload.
- [ ] Forcing a `PATCH /api/tasks/<id>/move` failure (devtools → Network → right-click the
      request → "Block request URL", then drag) snaps the card back to its previous
      position **and** raises an error toast.

## 5. Task editing (drawer)

- [ ] Clicking a card opens the right-side drawer for that task.
- [ ] Opening a drawer sets `?taskId=<id>` in the URL.
- [ ] Reloading the page with `?taskId=<id>` opens the same drawer pre-populated.
- [ ] Closing the drawer removes `?taskId` from the URL.
- [ ] Editing **title** and saving persists after reload.
- [ ] Editing **description** and saving persists after reload.
- [ ] Changing **priority** (e.g. `medium` → `high`) persists after reload and is
      reflected on the card.
- [ ] Changing **start date** and **due date** persists after reload.
- [ ] Submitting an invalid date range (`dueDate < startDate`) shows a validation error
      and is **not** saved (verify via `PATCH /api/tasks/<id>` → 400 in Network).
- [ ] A failed update raises an error toast (force a 4xx by editing while the backend is
      stopped, or block the network request).

## 6. Comments

- [ ] The drawer renders an empty-state when the task has no comments.
- [ ] `GET /api/tasks/<id>/comments` is called when the drawer opens.
- [ ] Submitting a non-empty comment via the composer adds it to the list immediately.
- [ ] After reload (drawer reopened) the comment is still present and shows the author
      `displayName` and timestamp.
- [ ] An empty/whitespace comment is rejected (disabled submit or inline error;
      no `POST /api/tasks/<id>/comments` request fires).
- [ ] A failed comment create raises an error toast (block the POST in Network).
- [ ] A second user (member of the same workspace) sees the comment after their next
      navigation to the task (no live updates expected — manual refresh required).

## 7. Timeline edits

- [ ] `/projects/<id>/timeline` renders the Gantt view for the project.
- [ ] Tasks **with** both `startDate` and `dueDate` show as bars.
- [ ] Tasks **without** dates are absent from the timeline (and that is OK — they exist
      on the board only).
- [ ] Dragging a bar horizontally updates `startDate` **and** `dueDate` together
      (duration preserved). `PATCH /api/tasks/<id>/schedule` is sent.
- [ ] After reload the new dates persist.
- [ ] Resizing a bar from the right edge changes only `dueDate`. After reload the new
      `dueDate` persists, `startDate` unchanged.
- [ ] Resizing a bar from the left edge changes only `startDate`. After reload it
      persists, `dueDate` unchanged.
- [ ] A failed schedule update (block the PATCH) rolls the bar back to its previous
      position **and** raises an error toast.
- [ ] The same task's dates on the board's task drawer match what the Gantt shows
      (single-source-of-truth check between the two views).

## 8. Dependency management

- [ ] Open the drawer on a task that has at least one other task in the same project.
- [ ] The dependency manager lists current dependencies (empty state if none).
- [ ] Adding a dependency from task A → B succeeds and the dependency appears in the
      drawer immediately.
- [ ] After reload, the dependency is still listed for A.
- [ ] On the timeline, a connector line is drawn from B's bar to A's bar.
- [ ] Removing the dependency from the drawer removes the line from the timeline after
      reload.
- [ ] Self-dependency (A → A) is rejected by the backend (verify `POST
      /api/tasks/<id>/dependencies` → 4xx).
- [ ] A duplicate dependency (re-adding A → B) is rejected by the backend
      (`POST /api/tasks/<id>/dependencies` → 4xx).
- [ ] Cross-project dependency (A from project P1, B from project P2) is rejected.

## 9. Authorization spot-checks

- [ ] Calling any protected endpoint without the `SESSION` cookie returns **401** with the
      standard `ApiError` JSON envelope (TASK-077).
- [ ] As user B (member of workspace W2), `GET /api/projects/<idFromW1>` returns **403**
      and **not** **404** with the standard envelope.
- [ ] As user B, `GET /api/tasks/<idFromW1>` returns 403 with the standard envelope.
- [ ] `GET /api/projects/<does-not-exist-uuid>` returns **404** with the standard envelope.

## 10. Read-only project settings

- [ ] `/projects/<id>/settings` shows project name, slug, description (or "No description"
      fallback), and human-formatted created/updated dates.
- [ ] The board columns table lists all three default columns in `position` order with
      1-based indices.
- [ ] No edit controls are present (read-only by design).

---

## Sign-off

- [ ] All sections above are checked.
- [ ] No console errors during the run that aren't already known (note any new ones in
      `progress.txt`).
- [ ] `docker compose ps` is still healthy at the end of the run.

If any step failed, capture: the failing step number, the request/response (if any),
the console error (if any), and the exact reproduction. That set is the input for
**TASK-083** (run the pass and fix the first blocking defect).
