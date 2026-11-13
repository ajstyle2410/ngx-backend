# Nexora Workspace API

Spring Boot REST API for the role-based dashboard, backed by MySQL 8. Schema changes are applied with Flyway at startup and Hibernate validates the migrated schema.

## Requirements

- JDK 17 or newer
- MySQL 8.0.16 or newer
- Maven (or the included `mvnw.cmd`)

## Create the database

Create a database and an application user in MySQL:

```sql
CREATE DATABASE npx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'npx_app'@'localhost' IDENTIFIED BY 'use-a-unique-password';
GRANT ALL PRIVILEGES ON npx_db.* TO 'npx_app'@'localhost';
```

Flyway creates the tables and indexes on the first successful startup. Do not edit an applied migration; add a new versioned migration instead.

## Configure environment

Set these variables in the backend process environment. Do not commit credentials or production secrets.

| Variable | Required | Default / purpose |
|---|---:|---|
| `DB_URL` | Yes for a configured database | JDBC URL, e.g. `jdbc:mysql://localhost:3306/npx_db?serverTimezone=UTC` |
| `DB_USER` | Yes | MySQL application user |
| `DB_PASSWORD` | Yes | MySQL application password |
| `JWT_SECRET` | Yes | Random secret of at least 32 characters |
| `JWT_EXPIRY_MINUTES` | No | `60` |
| `CORS_ORIGIN` | No | `http://localhost:4200` |
| `ADMIN_EMAIL` | Recommended | Creates the first administrator when no account with that email exists |
| `ADMIN_PASSWORD` | Recommended | Initial administrator password; hashed with BCrypt |
| `ADMIN_NAME` | No | `Workspace Administrator` |
| `DB_POOL_SIZE` | No | `5` |
| `PORT` | No | `8080` |

PowerShell example for a local session:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/npx_db?serverTimezone=UTC"
$env:DB_USER = "npx_app"
$env:DB_PASSWORD = "use-a-unique-password"
$env:JWT_SECRET = (New-Guid).ToString() + (New-Guid).ToString()
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "set-a-unique-initial-password"
$env:CORS_ORIGIN = "http://localhost:4200"
```

If `ADMIN_EMAIL` or `ADMIN_PASSWORD` is missing, automatic account creation is skipped; there is no default password.

## Run and test

```powershell
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
.\mvnw.cmd clean verify
```

The test suite uses an in-memory H2 database for application context and role-route checks. Runtime uses MySQL; configure the environment above before starting the API.

## API overview

- `POST /api/auth/login` — email/password login; returns JWT and account role.
- `/api/admin/**` — user management, project and participant management, task assignment, summary, audit log and CSV reports.
- `/api/developer/**` — assigned projects/tasks, task status, time logging and dashboard summary.
- `/api/user/**` — projects, project progress, tasks and time totals for participating users.
- `/api/messages/**`, `/api/notifications/**` — authenticated shared communication endpoints. Messages are only allowed between an administrator and a project participant, never directly between a user and developer.

The frontend API base URL is configured in `../npx-frontend/src/environments/environment.ts`. Set backend `CORS_ORIGIN` to the exact frontend origin. Use HTTPS for deployed frontend/API traffic.

## Current scope

The API persists user accounts, projects, participants, tasks, time logs, messages, notifications and audit entries. CSV export is implemented. Email delivery, scheduled deadline reminders, PDF export and password-reset flows are not included yet.
