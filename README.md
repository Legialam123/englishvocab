# English Vocab – Interactive Vocabulary Trainer

A web application designed to help users expand and practice their English vocabulary through interactive learning flows and quizzes. The project is built with Java Spring Boot, Thymeleaf, PostgreSQL, and Redis, delivering a fully server-rendered experience with an intuitive interface for every learner.

## Key features

- **Rich dictionary explorer** – Browse curated dictionaries, drill into word details, filter by CEFR level, and search instantly across thousands of entries.
- **Personal vocabulary collections** – Create, edit, and review private word lists with custom meanings, IPA, and notes tailored to your study plan.
- **Multiple learning modes** – Switch between alphabetical review, topic-based playlists, or hand-picked flashcard decks to keep sessions fresh.
- **Gamified quizzes & spaced repetition** – Answer smart flashcards, track streaks, and let the Leitner-like SRS engine schedule the next review for long-term retention.
- **Progress analytics dashboard** – Visualize learned words, daily goals, review backlog, and mastery status for every category at a glance.
- **Redis-backed session caching** – Cache active learning sessions, in-progress quizzes, and frequently accessed lookups in Redis to keep transitions instant and reduce database load.
- **Role-based administration** – Manage dictionaries, topics, and user accounts from a secure admin console with granular permissions.
- **Secure authentication** – Support classic login, OAuth sign-in, remember-me tokens, and CSRF protection out of the box.

## Tech stack

- **Backend:** Spring Boot 3.x, Spring Security, Spring Data JPA
- **Frontend:** Thymeleaf, Bootstrap 5, Vanilla JS
- **Database:** PostgreSQL (primary storage)
- **Caching & session state:** Redis
- **Build & tooling:** Maven Wrapper (`mvnw`), Docker Compose for local infrastructure

## Getting started

1. Copy `.env.example` (if present) to `.env` and fill in PostgreSQL/Redis connection details.
2. Launch the infrastructure services:

```powershell
docker-compose up -d
```

3. Run the application in development mode:

```powershell
./mvnw spring-boot:run
```

4. Visit [http://localhost:8080](http://localhost:8080) and log in with a seeded user (see `DataInitializer`).

## Optional commands

- Rebuild the project without running tests:

```powershell
./mvnw -DskipTests package
```

- Shut down local PostgreSQL and Redis containers:

```powershell
docker-compose down
```

Enjoy building your vocabulary one session at a time! 🎯
