# Contest

A small Spring Boot app for creating contests with placement prizes and viewing
what's been saved. Contests are persisted to Postgres.

## Stack

- Java 17 · Spring Boot 4.1 · Spring MVC · Spring Data JPA
- Postgres 16 (via Docker Compose for local dev; H2 in-memory for tests)
- Gradle (wrapper included — no local Gradle install needed)
- Vanilla HTML/CSS/JS frontend served from `src/main/resources/static/`

## Prerequisites

- JDK 17
- Docker (for local Postgres)

## Quick start

```sh
# 1. Start Postgres in the background
docker compose up -d

# 2. Run the app (Hibernate creates the schema on first boot)
./gradlew bootRun

# 3. Open the UI
open http://localhost:8080
```

Create a contest through the form. It's saved to Postgres and appears in the
"All contests" section below the form. Restart the app and the list persists.

## Stopping

```sh
# Stop the app: Ctrl-C in the bootRun terminal
docker compose down          # stop Postgres (data persists in volume)
docker compose down -v       # stop Postgres AND delete all data
```

## API

The frontend uses these endpoints — you can also hit them directly.

| Method | Path        | Body                                                        | Response                 |
|--------|-------------|-------------------------------------------------------------|--------------------------|
| GET    | `/contests` | —                                                           | `200 OK` — list of contests |
| POST   | `/contests` | form-encoded: `contestName`, `firstPrize`, `secondPrize?`, `thirdPrize?` | `201 Created` — created contest |

Example:

```sh
curl -X POST http://localhost:8080/contests \
  -d 'contestName=Summer Hackathon' \
  -d 'firstPrize=Gold medal' \
  -d 'secondPrize=Silver medal' \
  -d 'thirdPrize=Bronze medal'

curl http://localhost:8080/contests
```

## Configuration

Defaults live in `src/main/resources/application.properties`:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/contest
spring.datasource.username=contest
spring.datasource.password=contest
```

Override any of these with environment variables (Spring Boot maps
`SPRING_DATASOURCE_URL` etc. automatically) or by editing the file.

Docker Compose provisions Postgres with matching credentials — no changes
needed for the default setup.

## Development

```sh
./gradlew test          # runs against in-memory H2 — no Docker needed
./gradlew build         # full build + tests
./gradlew bootJar       # produces build/libs/Contest-0.0.1-SNAPSHOT.jar
```

The production jar is runnable directly, provided a Postgres instance is
reachable at the configured URL:

```sh
java -jar build/libs/Contest-0.0.1-SNAPSHOT.jar
```

## Inspecting the database

```sh
docker exec -it contest-postgres psql -U contest -d contest

contest=# \dt
contest=# select * from contest;
contest=# select * from contest_prizes;
```

## Project layout

```
src/main/java/org/example/contest/
  ContestApplication.java   # Spring Boot entry point
  ContestController.java    # REST endpoints at /contests
  ContestService.java       # create + list, uses the repository
  ContestRepository.java    # Spring Data JPA
  Contest.java              # @Entity — id, name, prizes
  Prize.java                # @Embeddable — value, place
  Place.java                # enum: FIRST, SECOND, THIRD
  ContestForm.java          # request DTO for the form POST

src/main/resources/
  application.properties    # datasource + JPA config
  static/index.html         # single-page frontend

docker-compose.yml          # local Postgres 16
```
