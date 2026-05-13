# QueueLive 🚀

> A real-time queue management system with instant position updates and WebSocket communication.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?style=flat-square&logo=springboot)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0-blue?style=flat-square&logo=keycloak)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-green?style=flat-square)

---

## 📋 About

**QueueLive** is a real-time queue management system built to simulate environments like banks, clinics, and restaurants. Multiple users can join queues simultaneously and track their position as it updates live whenever the attendant advances the queue.

---

## ✨ Features

- 🔐 **Keycloak Authentication** — OAuth2 with JWT, refresh token rotation, and session management
- 👥 **Two user roles** — Client (CLIENT) and Staff (STAFF) with role-based access control
- 📋 **Queue management** — Create, list, and close queues
- 🎟️ **Join queue** — Clients join and receive an automatically calculated position
- ⚡ **Real-time via WebSocket** — All connected clients receive instant updates when someone joins or gets called
- 🔄 **Automatic position recalculation** — When the next user is called, all positions are updated without gaps
- 🌐 **i18n error messages** — Error responses respect the `Accept-Language` header (en-US / pt-BR)
- 🚫 **Domain exceptions** — Structured error responses with error code, message and HTTP status
- 🐳 **Fully containerized** — Runs with a single `docker-compose up -d`
- 🧪 **Tested** — Unit and integration tests with Mockito and Spring Boot Test

---

## 🏗️ Architecture

```
┌─────────────────┐     REST + WebSocket     ┌─────────────────────┐
│   Frontend      │ ◄────────────────────── │   Spring Boot API   │
│   (React)       │                          │   port 8080         │
└─────────────────┘                          └────────┬────────────┘
                                                      │
                               ┌──────────────────────┼──────────────────────┐
                               │                      │                      │
                    ┌──────────▼──────┐    ┌──────────▼──────┐    ┌─────────▼───────┐
                    │    Keycloak     │    │     MySQL       │    │   WebSocket     │
                    │   port 8180     │    │   port 3306     │    │   STOMP/SockJS  │
                    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Authentication | Keycloak 24.0 + OAuth2 Resource Server |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA + Hibernate |
| Real-time | WebSocket (STOMP + SockJS) |
| Infrastructure | Docker + Docker Compose |
| Build | Maven |
| Tests | JUnit 5 + Mockito + Spring Boot Test |

---

## 📦 Project Structure

```
src/main/java/br/com/artheus/queuelive/
├── config/
│   ├── CorsConfig.java             # CORS configuration
│   ├── MessageSourceConfig.java    # i18n configuration
│   ├── SecurityConfig.java         # Spring Security + OAuth2 + JWT decoder
│   ├── SwaggerConfig.java          # OpenAPI / Swagger documentation
│   ├── WebSocketConfig.java        # STOMP + SockJS
│   └── QueueEventPublisher.java    # WebSocket event publishing
├── controller/
│   ├── UserController.java
│   ├── QueueController.java
│   └── QueueEntryController.java
├── dto/
│   ├── common/
│   │   └── ErrorResponse.java
│   ├── queue/
│   │   ├── QueueRequest.java
│   │   ├── QueueResponse.java
│   │   ├── QueueEntryResponse.java
│   │   └── QueueEventPayload.java
│   └── user/
│       └── UserResponse.java
├── entity/
│   ├── User.java
│   ├── Queue.java
│   └── QueueEntry.java
├── enums/
│   ├── Role.java                   # CLIENT, STAFF
│   ├── QueueStatus.java            # OPEN, CLOSED
│   └── EntryStatus.java            # WAITING, CALLED, SERVED
├── exception/
│   ├── ApiError.java               # Centralized error message keys
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── base/
│   │   └── BaseException.java
│   └── domain/
│       ├── QueueException.java
│       ├── QueueEntryException.java
│       └── UserException.java
├── repository/
│   ├── UserRepository.java
│   ├── QueueRepository.java        # @EntityGraph to avoid N+1
│   └── QueueEntryRepository.java   # @EntityGraph to avoid N+1
└── service/
    ├── UserService.java
    ├── QueueService.java
    └── QueueEntryService.java
```

---

## ⚡ WebSocket Events

When a client joins the queue or gets called, all connected users automatically receive an update via WebSocket.

**Subscription channel:** `/topic/queue/{queueId}`

### Event: user joins the queue

```json
{
  "type": "QUEUE_UPDATED",
  "queueId": "9cf9fbe7-3829-41b5-a2b1-01bbafc5dae2",
  "entries": [
    { "id": "c6fcb002-0785-40f4-8bb5-2f21f5024c2e", "username": "Client 01", "position": 1, "status": "WAITING" }
  ]
}
```

### Event: staff calls the next user

```json
{
  "type": "QUEUE_UPDATED",
  "queueId": "9cf9fbe7-3829-41b5-a2b1-01bbafc5dae2",
  "entries": [
    { "id": "c6fcb002-0785-40f4-8bb5-2f21f5024c2e", "username": "Client 01", "position": 1, "status": "CALLED" }
  ]
}
```

---

## ❌ Error Responses

All errors return a structured payload:

```json
{
  "timestamp": "2026-05-06T03:25:17.405",
  "status": 404,
  "error": "QUEUE_NOT_FOUND",
  "message": "Queue not found"
}
```

Responses respect the `Accept-Language` header. Supported languages: `en-US` (default) and `pt-BR`.

| Error | Status | Description |
|---|---|---|
| `QUEUE_NOT_FOUND` | 404 | Queue does not exist |
| `QUEUE_ALREADY_CLOSED` | 409 | Queue is already closed |
| `QUEUE_IS_CLOSED` | 409 | Queue is not accepting new entries |
| `QUEUE_NO_USERS_WAITING` | 404 | No users waiting in queue |
| `USER_ALREADY_IN_QUEUE` | 409 | User is already in this queue |
| `STAFF_CANNOT_JOIN_QUEUE` | 403 | Staff members cannot join a queue |
| `USER_NOT_FOUND` | 404 | User does not exist |

---

## 🔌 REST Endpoints

### User
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/users/me` | Authenticated | Returns the authenticated user's data |

### Queues
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/queues` | STAFF | Creates a new queue |
| GET | `/queues` | Authenticated | Lists all open queues |
| GET | `/queues/{id}` | Authenticated | Finds a queue by ID |
| PATCH | `/queues/{id}/close` | STAFF | Closes a queue |

### Queue Entries
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/queues/{id}/join` | CLIENT | Joins the queue |
| GET | `/queues/{id}/entries` | Authenticated | Lists all queue entries |
| POST | `/queues/{id}/next` | STAFF | Calls the next user in queue |

The full API documentation is available via Swagger UI at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

---

## 🚀 Getting Started

### Prerequisites

- Docker and Docker Compose

That's it. Everything else runs inside Docker.

### 1 — Clone the repository

```bash
git clone https://github.com/arthsdev/queuelive.git
cd queuelive
```

### 2 — Set up environment variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

```env
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=queuelive
MYSQL_USER=queuelive
MYSQL_PASSWORD=queuelive123
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_REALM=queuelive
KEYCLOAK_URL=http://keycloak:8080
```

### 3 — Start everything

```bash
docker-compose up -d
```

This will start:
- **MySQL** on port `3306`
- **Keycloak** on port `8180` — pre-configured with the `queuelive` realm, roles and client
- **Spring Boot API** on port `8080`

### 4 — Create a user in Keycloak

Access [http://localhost:8180](http://localhost:8180) with your admin credentials and create users with `CLIENT` or `STAFF` roles.

---

## 🔐 Authentication

QueueLive uses Keycloak as the Identity Provider. To obtain a token:

```bash
curl -X POST http://localhost:8180/realms/queuelive/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=queuelive-backend" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=your-username" \
  -d "password=your-password"
```

Use the returned `access_token` in all requests:

```
Authorization: Bearer <access_token>
```

---

## 🧪 Running Tests

```bash
mvn test
```

The test suite includes unit tests for all services with Mockito and integration tests for all controllers with Spring Boot Test.

---

## 🗺️ Roadmap

- [ ] Estimated wait time based on historical average
- [ ] Restrict users to one active queue at a time
- [ ] Push notifications when close to being called
- [ ] Admin dashboard with service metrics
- [ ] Redis for caching and horizontal scalability
- [ ] Keycloak backed by external MySQL in production
- [ ] Full React frontend

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.