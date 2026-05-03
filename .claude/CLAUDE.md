# CLAUDE.md – Developer Context

## Who I Am

I am a solo developer / freelancer working primarily in the **SaaS / B2B software** domain.
I build REST APIs, LLM applications / AI agents, data pipelines / ETL, dashboards / internal tools, and CLI scripts.
I deploy exclusively via **Docker / Docker Compose** (self-hosted).

---

## Tech Stack

| Layer | Tools |
|---|---|
| **Languages** | Python (primary), TypeScript/JavaScript, Kotlin, SQL |
| **Backend** | Flask |
| **Databases** | PostgreSQL, SQLite |
| **ORM / SQL** | ORM for simple queries, raw SQL for complex ones |
| **AI / LLM** | Instructor / Structured Outputs (direct SDK integration, no LangChain) |
| **Data** | Data Engineering / Pipelines, Analytics / BI |
| **Package Managers** | `uv` (preferred), `pip + venv` (fallback), `npm` / `pnpm` / `yarn` (TS) |
| **Linting / Formatting** | `black` + `ruff` (Python), `prettier` + `eslint` (TypeScript) |

---

## Coding Style & Philosophy

- **Experimental first:** Deliver working code first — even if everything is in one function. I will extract and refactor into reusable subfunctions myself.
- **Minimal by default:** No over-engineering, no unnecessary abstractions. Only introduce complexity when it earns its place.
- **Flat dependencies:** Prefer stdlib and minimal external packages. Only introduce a new dependency when it is clearly justified.
- **Language conventions:** `snake_case` in Python, `camelCase` in TypeScript / Kotlin. No mixing.
- **Everything in English:** All variable names, function names, comments, and docstrings must be in English.
- **Type hints everywhere:** Use type hints consistently on all functions and variables in Python.
- **Data modeling:** Use `dataclasses` as the default. Pydantic only where validation or serialization is explicitly needed.
- **async/await:** Use when the project calls for it (e.g., I/O-bound services). Do not force it into synchronous contexts.
- **Lambdas:** Use freely when they make code more compact and readable.

---

## Project Structure

Use **feature-based layout**: one folder per feature, containing all related files (routes, models, services, etc.).

```
project/
├── feature_a/
│   ├── __init__.py
│   ├── routes.py
│   ├── models.py
│   └── service.py
├── feature_b/
│   └── ...
├── main.py
├── config.py
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## Automatic File Generation

When creating a new project or service, **always** generate the following without being asked:

- `README.md` – minimal: project description, setup steps, example invocation
- `Dockerfile` + `docker-compose.yml`
- `.gitignore` – appropriate for the tech stack
- `.env.example` – all required environment variable keys, no values

---

## Configuration & Secrets

- During development: config files (YAML / TOML)
- Towards production: environment variables
- Always provide `.env.example` with all required keys (no values)

---

## Database & Migrations

- Always include **Alembic migration scripts** when defining database schemas — do not just provide the model definition.

---

## Documentation

- **Docstrings** on all public functions and classes (Google or NumPy style, be consistent within a file)
- **README** with example requests for any API surface
- No inline comments that just restate what the code does

---

## Error Handling

- Use **generic exceptions with meaningful, descriptive messages**
- No custom exception class hierarchies unless explicitly requested
- No silent failures — errors should be visible and informative

---

## Logging

No fixed standard — adapt to the project context. Do not impose a logging setup unless asked.

---

## Testing

- Write tests **after implementation**, only when time and context call for it
- No fixed test framework preference — use whatever fits the project
- Focus on critical / complex logic when writing tests

---

## Git

- Use **Conventional Commits**: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`, `test:`, etc.

---

## How I Work With Claude

- I provide a **rough outline / goal** — Claude fills in the gaps independently
- For **ambiguous requirements**: ask clarifying questions first, then implement
- For **incomplete implementations**: before using `# TODO: implement`, ask me whether a TODO placeholder is acceptable or whether a simplified but working version should be provided instead
- For **existing code**: suggest refactoring if relevant, but only execute it when explicitly asked. Touch only what was requested — do not refactor unrelated parts.
- For **code changes**: show the full file for small changes; show only the modified block / function for larger files
- After **plan mode**: all permissions are granted until the plan is done

---

## Response Style

- **Technical questions:** Short and direct — no preamble, no basics explanations
- **Conceptual / architectural questions:** Detailed, with context, trade-offs, and reasoning
- Code first, brief explanation after
- No filler phrases ("Great question!", "Of course!", "Certainly!")
- No security warnings for obviously harmless operations
- No unsolicited alternative approaches — if I ask for X, deliver X
- Do not explain every small change — only flag what materially affects behavior or architecture
- Summarize potential risks / edge cases **at the end** of a response, not inline during implementation

---

## What Good Work Looks Like

1. **Runs on first try** — no silent assumptions, no missing imports, no placeholder logic
2. **Immediately readable** — clear naming, no cryptic one-liners without context
3. **Minimal** — no unnecessary code, no speculative abstractions
4. **Extensible** — structured so the next logical feature can be added without rewriting the core
