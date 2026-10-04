# 🐞 Bug Tracker — Enterprise Bug Tracking Platform

A production-style bug tracking application built with **Spring Boot 3 / Java 17** and a
dependency-free **vanilla JavaScript** single-page frontend. It combines a strict lifecycle
state machine, row-level role authorization, secure file attachments (evidence) and a full
audit trail for every ticket.

---

## ✨ Features

| Area | Highlights |
| --- | --- |
| **Lifecycle workflow** | Enforced state machine `OPEN → IN_PROGRESS → RESOLVED → CLOSED` with `REOPENED` re-verification loop; administrators may short-circuit to `CLOSED` for duplicates. Illegal transitions return **HTTP 409**. |
| **Resolution gate** | A bug cannot be resolved or closed without a resolution summary. |
| **Row-level authorization** | Administrators, QA testers and developers have distinct capabilities; developers may only act on bugs assigned to them. |
| **Pessimistic-lock bug codes** | Gap-free, human-readable identifiers (`BUG-0001`) allocated through a locked sequence counter. |
| **Audit history** | Every change (title, description, assignee, status, priority, severity, resolution, comments, attachments) is recorded with actor and timestamp. |
| **Attachments** | Evidence upload with extension/content-type allow-list, path-traversal protection and 5 MB limit; streamed inline preview or download. |
| **Search & triage** | Full-text search across code/title/description plus status, severity, priority, assignee, reporter and unassigned filters, sorting and pagination. |
| **Dashboard** | Aggregated metrics and status/severity/priority distributions, plus “assigned to me” workload. |
| **Stateless JWT security** | Bearer-token authentication, BCrypt password hashing, CORS for local development, JSON error contract. |
| **Automated tests** | 33 JUnit 5 / MockMvc / AssertJ tests covering the workflow matrix, permissions, HTTP flows and error mapping (404/405/415). |

## 🧱 Technology Stack

| Layer | Technology |
| --- | --- |
| Runtime | Java 17 |
| Framework | Spring Boot 3.2.5 (Web, Data JPA, Security, Validation) |
| Persistence | MySQL 8 (`mysql-connector-j`) · H2 for tests |
| Security | Spring Security, JJWT 0.11.5, BCrypt |
| Build | Maven 3.9+ |
| Frontend | HTML5, CSS3 and dependency-free vanilla JavaScript (no build step, no npm) |

## 📁 Project Structure

```
QA/
├── frontend/                  # Static SPA served by Spring Boot (file:./frontend/)
│   ├── index.html             # Sign in / create account
│   ├── dashboard.html         # Metrics and charts
│   ├── bugs.html              # Searchable, filterable bug directory
│   ├── bug-form.html          # Create / edit a bug
│   ├── bug-detail.html        # Detail view, workflow actions, comments, evidence
│   ├── users.html             # Administrator user directory
│   ├── css/styles.css         # Design system (tokens, components, layout)
│   └── js/
│       ├── api.js             # Session storage + typed REST client
│       ├── ui.js              # DOM helpers, badges, modals, toasts, shell, pagination
│       ├── auth.js            # Login / registration screen
│       ├── dashboard.js       # Dashboard page controller
│       ├── bugs.js            # Bug directory page controller
│       ├── bug-form.js        # Bug create/edit controller
│       ├── bug-detail.js      # Bug detail controller
│       └── users.js           # User administration controller
├── src/main/java/com/bugtracker/
│   ├── config/                # Security, CORS, properties, data seeder
│   ├── controller/            # REST endpoints (auth, bugs, dashboard, users)
│   ├── dto/                   # Request/response records
│   ├── entity/                # JPA entities and enums
│   ├── exception/             # Domain exceptions + global handler
│   ├── mapper/                # Entity → DTO mappers
│   ├── repository/            # Spring Data repositories + specifications
│   ├── security/              # JWT service, filter, principal, handlers
│   └── service/               # Business rules (workflow, authorization, storage)
├── src/test/java/com/bugtracker/
│   ├── integration/           # MockMvc end-to-end API tests (H2)
│   └── service/               # Lifecycle state-machine unit tests
├── uploads/                   # Attachment storage (git-ignored, .gitkeep kept)
├── .env.example               # Environment variable template
└── pom.xml
```
## ✅ Prerequisites

* **JDK 17** or newer (`java -version`)
* **Maven 3.9+** (`mvn -version`)
* **MySQL 8** (optional — only needed to run the app; tests use in-memory H2)

## ⚡ Quick Start

### 1. Run it (MySQL server available)

```bash
mvn spring-boot:run
```

Then open **<http://localhost:8080>** and sign in with `admin@bugtracker.dev` / `Admin@12345`.

The Hibernate schema is created/updated automatically, and on a **fresh** database the demo
accounts plus ten sample bugs are inserted once — later restarts detect the existing rows and log
`Bug data already present, skipping bug seeding.`

### 2. First-time MySQL setup

The application only creates the empty *database*; the MySQL account must exist beforehand. Run
this once against your server:

```sql
CREATE DATABASE IF NOT EXISTS bugtracker_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'bugtracker'@'localhost' IDENTIFIED BY 'BugTracker@2026';
GRANT ALL PRIVILEGES ON bugtracker_db.* TO 'bugtracker'@'localhost';
FLUSH PRIVILEGES;
```

Prefer your own credentials (for example `root`)? Override them for the current shell instead of
editing any file:

```powershell
$env:DB_USER = 'root'; $env:DB_PASSWORD = 'your-password'; mvn spring-boot:run
```

### 3. No MySQL? Boot it against in-memory H2

H2 ships on the test classpath, so the full app runs without any database server:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'test'    # H2 datasource, schema generated from the entities
$env:APP_SEED_DATA          = 'true'    # create the demo accounts and sample bugs
mvn spring-boot:run "-Dspring-boot.run.useTestClasspath=true"
```

Data lives only for the lifetime of the process. (On macOS/Linux use `export NAME=value`.)

### 4. Other useful commands

```bash
mvn test                                         # 33 automated tests (H2, no MySQL required)
mvn clean package                                # build target/bug-tracker-backend-1.0.0.jar
java -jar target/bug-tracker-backend-1.0.0.jar   # run the packaged jar (needs MySQL)
```

* Different port: `$env:SERVER_PORT = '9090'` before starting.
* Stop the server with `Ctrl + C` (or kill the JVM listening on the port).

### 5. Resetting the demo data

```sql
DROP DATABASE bugtracker_db;      -- recreate + reseed on the next start
```

Uploaded evidence lives in `uploads/` (`UPLOAD_DIR`) and is safe to delete manually.

## ⚙️ Configuration

All settings are externalised; nothing sensitive is hardcoded. Copy `.env.example` and export
the variables (or edit `src/main/resources/application.properties` defaults).

| Variable | Property | Default | Purpose |
| --- | --- | --- | --- |
| `SERVER_PORT` | `server.port` | `8080` | HTTP port |
| `DB_HOST` | `spring.datasource.url` | `localhost` | MySQL host |
| `DB_PORT` | `spring.datasource.url` | `3306` | MySQL port |
| `DB_NAME` | `spring.datasource.url` | `bugtracker_db` | Database name (auto-created) |
| `DB_USER` | `spring.datasource.username` | `bugtracker` | Database user |
| `DB_PASSWORD` | `spring.datasource.password` | `BugTracker@2026` | Database password |
| `JWT_SECRET` | `app.jwt.secret` | dev key | HS256 signing key (≥ 32 bytes) |
| `JWT_EXPIRATION_MS` | `app.jwt.expiration-ms` | `86400000` | Token lifetime (24 h) |
| `UPLOAD_DIR` | `app.upload-dir` | `./uploads` | Attachment storage directory |
| `MAX_FILE_SIZE` | `spring.servlet.multipart.max-file-size` | `5MB` | Per-file upload limit |
| `MAX_REQUEST_SIZE` | `spring.servlet.multipart.max-request-size` | `10MB` | Multipart request limit |
| `APP_SEED_DATA` | `app.seed-data` | `true` | Create demo accounts and sample bugs on a fresh database |

> ⚠️ **Before deploying:** replace `JWT_SECRET` with a strong random value and change the seeded
> demo passwords (or set `APP_SEED_DATA=false`).

## 🚀 Running the Application

```bash
# 1. Point the app at MySQL (or let it create the schema automatically)
export DB_USER=bugtracker
export DB_PASSWORD='BugTracker@2026'

# 2. Start the server — the frontend is served from ./frontend
mvn spring-boot:run
```

Open <http://localhost:8080> and sign in. The Hibernate schema is created/updated automatically
(`spring.jpa.hibernate.ddl-auto=update`), and with `APP_SEED_DATA=true` the demo data below is
inserted once on a fresh database.

```bash
# Package a runnable jar
mvn clean package
java -jar target/bug-tracker-backend-1.0.0.jar
```

## 🔑 Demo Accounts (seeded)

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@bugtracker.dev` | `Admin@12345` |
| QA Tester | `tester@bugtracker.dev` | `Tester@12345` |
| QA Tester | `tester2@bugtracker.dev` | `Tester2@12345` |
| Developer | `developer@bugtracker.dev` | `Developer@12345` |
| Developer | `developer2@bugtracker.dev` | `Developer2@12345` |

The sign-in screen can autofill the first three accounts with a single click.

## 👥 Roles & Permission Matrix

| Capability | `ROLE_ADMIN` | `ROLE_TESTER` | `ROLE_DEVELOPER` |
| --- | :---: | :---: | :---: |
| Report a bug | ✅ | ✅ | ✅ |
| View bugs / dashboard | ✅ | ✅ | ✅ |
| Edit bug fields | ✅ | ✅ | assigned bugs only |
| Change status | any transition | verify: close/reopen a resolved bug | assigned bugs only |
| Change priority / severity | ✅ | ✅ | ❌ |
| Assign a developer | ✅ | ❌ | ❌ |
| Delete a bug | ✅ | ❌ | ❌ |
| Comment / upload evidence | ✅ | ✅ | ✅ |
| Delete comment / attachment | author/uploader or admin | author/uploader | author/uploader |
| User directory & role changes | ✅ | ❌ | ❌ |

Self-registration can only create **tester** or **developer** accounts; an administrator can
later promote a user through `PATCH /api/users/{id}/role`.

## 🔄 Lifecycle State Machine

```
        ┌──────────────────────────────────────────┐
        ▼                                          │
      OPEN ──▶ IN_PROGRESS ──▶ RESOLVED ──▶ CLOSED │
        │            ▲             │               │
        │            │             ▼               │
        └────────────┴──────── REOPENED ───────────┘
```

* Allowed: `OPEN→IN_PROGRESS`, `OPEN→RESOLVED`, `IN_PROGRESS→OPEN`, `IN_PROGRESS→RESOLVED`,
  `RESOLVED→CLOSED`, `RESOLVED→REOPENED`, `REOPENED→IN_PROGRESS`, `REOPENED→RESOLVED`,
  `CLOSED→REOPENED` — plus the administrator shortcut `OPEN→CLOSED`.
* `RESOLVED` requires a resolution summary; `CLOSED` requires the bug to have been resolved.
* Any other combination returns **HTTP 409**.

## 🔌 REST API

All endpoints are JSON and (except sign-in/registration) require
`Authorization: Bearer <token>`.

### Authentication — `/api/auth`

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Self-service registration (tester/developer) → `201` + token |
| `POST` | `/api/auth/login` | Exchange credentials for a JWT → `200` |
| `GET` | `/api/auth/me` | Profile of the caller (session restore) |
| `POST` | `/api/auth/logout` | Advisory sign-out acknowledgement (tokens are stateless) |

### Bugs — `/api/bugs`

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/bugs` | Search/filter/sort/paginate: `search`, `status`, `severity`, `priority`, `assigneeId`, `reporterId`, `unassigned`, `page`, `size`, `sortBy`, `sortDirection` |
| `GET` | `/api/bugs/{id}` | Full detail incl. comments, attachments and history |
| `POST` | `/api/bugs` | Report a bug → `201` |
| `PUT` | `/api/bugs/{id}` | Update bug fields (triage/assignee restricted) |
| `DELETE` | `/api/bugs/{id}` | Delete bug, comments, attachments and files (admin) |
| `PATCH` | `/api/bugs/{id}/status` | Workflow transition `{ "status": "...", "resolution": "..." }` |
| `PATCH` | `/api/bugs/{id}/priority` | `{ "priority": "LOW\|MEDIUM\|HIGH\|URGENT" }` |
| `PATCH` | `/api/bugs/{id}/severity` | `{ "severity": "LOW\|MEDIUM\|HIGH\|CRITICAL" }` |
| `PATCH` | `/api/bugs/{id}/assign` | `{ "assignedDeveloperId": 3 }` (send `null` to unassign) |
| `GET` / `POST` | `/api/bugs/{id}/comments` | List / add comments |
| `DELETE` | `/api/bugs/{id}/comments/{commentId}` | Delete a comment (author or admin) |
| `GET` / `POST` | `/api/bugs/{id}/attachments` | List / upload evidence (`multipart/form-data`, part name `file`) |
| `GET` | `/api/bugs/{id}/attachments/{attachmentId}/download` | Stream the file (also accepts `?token=` for `<img>` previews) |
| `DELETE` | `/api/bugs/{id}/attachments/{attachmentId}` | Remove an attachment (uploader or admin) |

### Dashboard & Users

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/dashboard/statistics` | Counts plus status/severity/priority distributions and `assignedToMe` |
| `GET` | `/api/users` | Full user directory (admin only) |
| `GET` | `/api/users/assignable` | Developers + administrators (assignee picker) |
| `GET` | `/api/users/developers` | Developer accounts only |
| `PATCH` | `/api/users/{id}/role` | `{ "role": "ROLE_ADMIN\|ROLE_TESTER\|ROLE_DEVELOPER" }` (admin only) |
| `PATCH` | `/api/users/{id}/status` | `{ "enabled": false }` (admin only) |

### Example requests

```bash
# Sign in
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@bugtracker.dev","password":"Tester@12345"}' | jq -r .token)

# Report a bug
curl -s -X POST http://localhost:8080/api/bugs \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
        "title": "Checkout button is unresponsive",
        "description": "Clicking Pay does nothing on Safari 17.",
        "stepsToReproduce": "1. Add any item\n2. Tap Pay",
        "expectedResult": "The payment sheet opens.",
        "actualResult": "Nothing happens.",
        "environment": "Safari 17 / iOS 17",
        "severity": "HIGH",
        "priority": "URGENT"
      }'

# Search open, unassigned, high severity bugs
curl -s "http://localhost:8080/api/bugs?status=OPEN&unassigned=true&severity=HIGH&page=0&size=10&sortBy=createdAt&sortDirection=desc" \
  -H "Authorization: Bearer $TOKEN"
```

### Response envelopes

| Payload | Shape |
| --- | --- |
| Paged lists | `{ "content": [...], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5, "first": true, "last": false, "sortBy": "createdAt", "sortDirection": "desc" }` |
| Authentication | `{ "token": "…", "tokenType": "Bearer", "expiresIn": 86400000, "user": { "id": 1, "fullName": "…", "email": "…", "role": "ROLE_ADMIN", "enabled": true, "createdAt": "…" } }` |
| Errors | `{ "timestamp": "…", "status": 409, "error": "Conflict", "message": "Invalid status transition from CLOSED to IN_PROGRESS.", "path": "/api/bugs/1/status", "fieldErrors": { "password": "…" } }` |

Error status codes: `400` validation/bad request, `401` unauthenticated or bad credentials,
`403` authenticated but not allowed, `404` missing resource, `409` invalid workflow transition or
duplicate email.

## 📎 Attachment Policy

* Maximum **5 MB** per file (`MAX_FILE_SIZE`), 10 MB per request.
* Allowed extensions: `png jpg jpeg gif webp bmp pdf txt log json csv md rtf doc docx xls xlsx`.
* Executables and scripts (`exe dll bat cmd sh js jar msi com scr ps1 vbs php jsp py rb pl apk dmg html htm`)
  are rejected, and the declared content type must match the extension group.
* Files are renamed to a UUID on disk inside `UPLOAD_DIR`; the original name is only used for
  display and download headers, and every path is normalised inside the upload root.
* `GET …/download` streams images inline (for previews) and everything else as an attachment.
  Because browsers cannot set headers on `<img>` requests, a `?token=<jwt>` query parameter is
  accepted for this read-only endpoint.

## 🧪 Testing

```bash
mvn test          # 33 tests: unit + MockMvc integration (in-memory H2)
mvn clean package # compiles, tests and builds target/bug-tracker-backend-1.0.0.jar
```

| Suite | Coverage |
| --- | --- |
| `BugWorkflowServiceTest` (24 tests) | Transition matrix, administrator shortcut, resolution gates and role helpers |
| `BugTrackerApiIntegrationTest` (9 tests) | Registration/login/401s, admin-only user management, the full bug lifecycle with 403/400/409 paths, triage & delete permissions, comments, multipart uploads (including rejected executables), search/filter/pagination, dashboard statistics and the 404/405/415 error mapping |

Tests boot the real application context, hit the REST API through the JWT filter chain and commit
each request — the same path a browser takes — against H2 in MySQL compatibility mode.

## 🗒️ Notes & Conventions

* **Frontend** is plain HTML/CSS/JS with no build step: pages include `js/api.js` (REST client +
  session) and `js/ui.js` (components) before the page controller, which renders into `#app`.
* **Roles in the UI** are compared in their short form (`ADMIN`, `TESTER`, `DEVELOPER`); the REST
  layer returns the Spring form (`ROLE_ADMIN`, …) and `Session.role` strips the prefix once.
* **JWT storage**: tokens live in `localStorage` for simplicity. Move to http-only cookies with a
  refresh endpoint for internet-facing deployments.
* `spring.jpa.hibernate.ddl-auto=update` keeps local setup friction-free; use Flyway/Liquibase
  migrations for production.
* Uploaded files are **not** virus-scanned — the allow-list only limits file types.



