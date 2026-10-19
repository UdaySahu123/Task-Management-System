# TaskFlow — Task Management System

A full-stack task manager: a **React + Vite** single-page app talking to a
**Spring Boot 3 / Java 21** REST API backed by **MySQL 8**. Authentication is
stateless JWT, the schema is owned by Flyway, and every task is scoped to its
owner on the server.

---

## Table of contents

1. [Features](#features)
2. [Tech stack](#tech-stack)
3. [Project structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Quick start](#quick-start)
6. [Configuration](#configuration)
7. [API reference](#api-reference)
8. [Testing](#testing)
9. [Building for production](#building-for-production)
10. [Design decisions](#design-decisions)

---

## Features

- **Authentication** — register, login, and a validated JWT session that is
  restored on page reload. Passwords are hashed with BCrypt.
- **Task management** — create, read, update, delete; change status and
  priority inline.
- **Ownership enforced server-side** — the owner is taken from the JWT, never
  from the request. Another user's task returns `404` (not `403`), so IDs cannot
  be probed.
- **Search, filter, sort, paginate** — full-text search over title/description,
  filters for status/priority/overdue, seven sort orders, page navigation.
- **Dashboard** — totals, completion progress, recent tasks and upcoming
  deadlines, all computed from live data.
- **Profile** — update name/email and change password (requires the current
  password).
- **Polished UX** — responsive layout, toast notifications, loading skeletons,
  empty states, accessible dialogs and keyboard focus styles.

---

## Tech stack

| Layer     | Choice                                                              |
| --------- | ------------------------------------------------------------------- |
| Frontend  | React 18, Vite 6, React Router 6, Axios, lucide-react, plain CSS    |
| Backend   | Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA       |
| Auth      | JWT (HS256) via `jjwt` 0.12, BCrypt password hashing                |
| Database  | MySQL 8, schema managed by Flyway                                   |
| Testing   | JUnit 5 + Mockito + MockMvc + H2 (backend), Vitest + Testing Library (frontend) |
| Build     | Maven (backend), npm/Vite (frontend)                                |

---

## Project structure

```
task-management-system/
├── backend/                     # Spring Boot API
│   ├── src/main/java/com/example/taskmanager/
│   │   ├── config/              # SecurityConfig (CORS, JWT wiring)
│   │   ├── controller/          # Auth, User, Task, Dashboard REST endpoints
│   │   ├── dto/                 # Request/response records
│   │   ├── entity/              # User, Task, TaskStatus, TaskPriority
│   │   ├── exception/           # Central error handling
│   │   ├── mapper/              # Entity <-> DTO
│   │   ├── repository/          # Spring Data repositories
│   │   ├── security/            # JWT filter, principal, entry points
│   │   └── service/             # Business logic
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/        # Flyway SQL
│   ├── src/test/…               # Unit + integration tests (H2)
│   ├── scripts/api-smoke-test.ps1
│   └── .env.example
└── frontend/                    # React SPA
    ├── src/
    │   ├── api/                 # Axios client + endpoint wrappers
    │   ├── components/          # Layout, cards, modals, badges…
    │   ├── context/             # Auth + Toast providers
    │   ├── pages/               # Login, Dashboard, Tasks, Detail, Profile, 404
    │   ├── utils/               # Formatters and enum labels
    │   └── index.css            # Design system (tokens + components)
    └── .env.example
```

---

## Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **Node.js 20+** and npm
- **MySQL 8**

---

## Quick start

### 1. Database

Create the database and an application user (as a MySQL admin):

```sql
CREATE DATABASE task_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'taskapp'@'localhost' IDENTIFIED BY 'your-strong-password';
GRANT ALL PRIVILEGES ON task_management.* TO 'taskapp'@'localhost';
FLUSH PRIVILEGES;
```

Flyway creates the tables on first startup.

### 2. Backend

```bash
cd backend
cp .env.example .env          # then edit .env with real values
mvn package                    # or: mvn spring-boot:run
java -jar target/task-management-backend.jar
```

The API listens on **http://localhost:8080**. Flyway migrates the schema on
boot and Hibernate validates the entities against it.

### 2b. Backend environment file

`backend/.env` (git-ignored) is loaded automatically. Minimum contents:

```properties
DB_URL=jdbc:mysql://localhost:3306/task_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=taskapp
DB_PASSWORD=your-db-password
JWT_SECRET=at-least-32-random-characters
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

The dev server port (`5173`) is in the backend CORS allow-list by default.

---

## Configuration

### Backend (environment variables)

| Variable               | Default                                    | Notes                                        |
| ---------------------- | ------------------------------------------ | -------------------------------------------- |
| `DB_URL`               | `jdbc:mysql://localhost:3306/task_management…` | JDBC URL                                 |
| `DB_USERNAME`          | `taskapp`                                  |                                              |
| `DB_PASSWORD`          | *(empty)*                                  | required                                     |
| `JWT_SECRET`           | *(empty)*                                  | **required**, ≥ 32 chars                     |
| `JWT_EXPIRATION_MS`    | `86400000`                                 | token lifetime (24h)                         |
| `SERVER_PORT`          | `8080`                                     |                                              |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | comma-separated frontend origins          |

Values are read from the shell or from an untracked `.env` file.

### Frontend

| Variable              | Default                     |
| --------------------- | --------------------------- |
| `VITE_API_BASE_URL`   | `http://localhost:8080/api` |

---

## API reference

Base URL: `http://localhost:8080` · Auth header: `Authorization: Bearer <token>`

### Authentication

| Method | Path                 | Auth | Description                    |
| ------ | -------------------- | ---- | ------------------------------ |
| POST   | `/api/auth/register` | —    | Create an account, returns JWT |
| POST   | `/api/auth/login`    | —    | Authenticate, returns JWT      |
| POST   | `/api/auth/logout`   | —    | No-op (client discards token)  |
| GET    | `/api/auth/me`       | ✓    | Current user profile           |

### Tasks

| Method | Path                        | Description                              |
| ------ | --------------------------- | ---------------------------------------- |
| POST   | `/api/tasks`                | Create a task                            |
| GET    | `/api/tasks`                | List with search/filter/sort/pagination  |
| GET    | `/api/tasks/{id}`           | Fetch one task                           |
| PUT    | `/api/tasks/{id}`           | Full update                              |
| PATCH  | `/api/tasks/{id}/status`    | Update status                            |
| PATCH  | `/api/tasks/{id}/priority`  | Update priority                          |
| DELETE | `/api/tasks/{id}`           | Delete                                   |

**List query parameters:** `search`, `status` (`TODO`/`IN_PROGRESS`/`COMPLETED`),
`priority` (`LOW`/`MEDIUM`/`HIGH`), `overdue` (`true`/`false`),
`sort` (`field,dir` — default `createdAt,desc`), `page` (0-based), `size`.

### Dashboard & profile

| Method | Path                        | Description                     |
| ------ | --------------------------- | ------------------------------- |
| GET    | `/api/dashboard/stats`      | Aggregated statistics           |
| GET    | `/api/users/me`             | Current profile                 |
| PUT    | `/api/users/me`             | Update name/email               |
| PUT    | `/api/users/me/password`    | Change password                 |

### Error shape

```json
{
  "timestamp": "2026-01-01T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "fieldErrors": [{ "field": "title", "message": "Title is required" }]
}
```

---

## Testing

### Backend

```bash
cd backend
mvn test
```

Runs unit tests (services, JWT) and an H2-backed MockMvc integration test for the
task endpoints. Flyway is disabled in tests and Hibernate creates the schema.

An end-to-end smoke test against a **running** server + MySQL:

```powershell
powershell -ExecutionPolicy Bypass -File backend/scripts/api-smoke-test.ps1
```

### Frontend

```bash
cd frontend
npm test           # Vitest, single run
npm run test:watch # watch mode
```

---

## Building for production

### Backend

```bash
cd backend
mvn clean package
java -jar target/task-management-backend.jar
```

The resulting JAR is self-contained. Provide configuration through environment
variables (see [Configuration](#configuration)). Health check:
`GET /actuator/health`.

### Frontend

```bash
cd frontend
npm run build      # outputs static assets to frontend/dist
npm run preview    # serve the build locally to verify
```

Deploy `dist/` to any static host. Set `VITE_API_BASE_URL` at build time to the
deployed API URL, and add the frontend origin to the backend's
`CORS_ALLOWED_ORIGINS`.

---

## Design decisions

- **Stateless JWT instead of sessions.** The token is returned in the response
  body and sent back as a Bearer header; there is no server-side session and no
  CSRF surface. `logout` is a client-side action.
- **Flyway owns the schema.** Hibernate runs in `validate` mode so entities and
  the database can never silently drift apart.
- **`404` for other users' tasks.** Returning `404` rather than `403` avoids
  confirming that a given task ID exists, preventing enumeration.
- **Server-computed `overdue`.** The flag is calculated once on the backend so
  every client shows the same value.
- **No Lombok, no magic.** DTOs are records; entities use explicit
  getters/setters. Fewer annotations means clearer stack traces and simpler
  onboarding.
- **Dependency-light frontend.** Axios for HTTP, a small hand-written design
  system in `index.css`, and context providers for auth and toasts — no heavy
  UI framework.
