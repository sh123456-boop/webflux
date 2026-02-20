# WebFlux + JDBC(Blocking) Benchmark Server

This project implements the **WebFlux + JDBC(Blocking)** variant for benchmark comparison.
MySQL is external (for example AWS RDS), so no DB container is included.

## Stack

- Java 21
- Spring Boot 3.x
- Spring WebFlux (`spring-boot-starter-webflux`)
- JDBC + Hikari (`spring-boot-starter-jdbc`)
- MySQL driver (`mysql-connector-j`)
- Gradle Wrapper + `bootJar`

## Important Runtime Rule

JDBC is blocking and must never run on the Netty event loop.
All JDBC calls are offloaded using:

- fixed `ExecutorService`
- Reactor `Scheduler`
- `Mono.fromCallable(...).subscribeOn(jdbcOffloadScheduler)`

Transactional logic is isolated in `TxService` (`@Transactional`) and executed entirely inside one `Callable`.

## DB Schema

```sql
CREATE TABLE bench_items (
  id BIGINT PRIMARY KEY,
  payload VARCHAR(100) NOT NULL,
  cnt BIGINT NOT NULL DEFAULT 0
);
```

## API Spec

### 1) GET `/api/v1/ping`

```json
{"ok":true}
```

### 2) GET `/api/v1/io/db/read?id=123&sleepMs=80`

Behavior:
1. `SELECT SLEEP(sec)`
2. `SELECT id, payload, cnt FROM bench_items WHERE id = ?`

```json
{"id":123,"payload":"...","cnt":0,"sleptMs":80}
```

### 3) POST `/api/v1/io/db/tx?sleepMs=30`

Request body:

```json
{"id":123,"delta":1}
```

Behavior (transaction):
1. optional `SELECT SLEEP(sec)`
2. `UPDATE bench_items SET cnt = cnt + ? WHERE id = ?`
3. `SELECT cnt FROM bench_items WHERE id = ?`
4. commit

```json
{"id":123,"cnt":10,"delta":1,"sleptMs":30}
```

## Configuration

Required DB env vars:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASS`

Hikari env vars (defaults):
- `DB_POOL_MAX` (`50`)
- `DB_POOL_MIN` (`50`)
- `DB_CONN_TIMEOUT_MS` (`2000`)

JDBC offload env var:
- `JDBC_OFFLOAD_THREADS` (`50`)

JVM options default in Dockerfile:
- `-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError`

## Build

```bash
./gradlew clean bootJar
```

## Run (local)

```bash
DB_HOST=... DB_PORT=3306 DB_NAME=bench DB_USER=... DB_PASS=... \
DB_POOL_MAX=50 DB_POOL_MIN=50 JDBC_OFFLOAD_THREADS=50 \
./gradlew bootRun
```

## Docker

Build image:

```bash
docker build -t webflux-jdbc-bench:latest .
```

Unified run example:

```bash
docker run --rm -p 8080:8080 --cpus=1 --memory=1g --memory-swap=1g \
  -e DB_HOST=... -e DB_PORT=3306 -e DB_NAME=bench -e DB_USER=... -e DB_PASS=... \
  -e DB_POOL_MAX=50 -e DB_POOL_MIN=50 -e JDBC_OFFLOAD_THREADS=50 \
  -e JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError" \
  <image>
```
