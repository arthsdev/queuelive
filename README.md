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

---

## 📦 Project Structure

```
src/main/java/br/com/artheus/queuelive/
├── config/
│   ├── SecurityConfig.java         # Spring Security + OAuth2
│   ├── WebSocketConfig.java        # STOMP + SockJS
│   └── QueueEventPublisher.java    # WebSocket event publishing
├── controller/
│   ├── UserController.java
│   ├── QueueController.java
│   └── QueueEntryController.java
├── dto/
│   ├── UserResponse.java
│   ├── QueueRequest.java
│   ├── QueueResponse.java
│   ├── QueueEntryResponse.java
│   └── QueueEventPayload.java
├── entity/
│   ├── User.java
│   ├── Queue.java
│   └── QueueEntry.java
├── enums/
│   ├── Role.java                   # CLIENT, STAFF
│   ├── QueueStatus.java            # OPEN, CLOSED
│   └── EntryStatus.java            # WAITING, CALLED, SERVED
├── repository/
│   ├── UserRepository.java
│   ├── QueueRepository.java
│   └── QueueEntryRepository.java
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
  "queueId": 7,
  "entries": [
    { "id": 6, "username": "Client 01", "position": 1, "status": "WAITING" }
  ]
}
```

### Event: staff calls the next user

```json
{
  "type": "QUEUE_UPDATED",
  "queueId": 7,
  "entries": [
    { "id": 6, "username": "Client 01", "position": 1, "status": "CALLED" }
  ]
}
```

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

---

## 🚀 Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21+
- Maven

### 1 — Clone the repository

```bash
git clone https://github.com/seu-usuario/queuelive.git
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
DB_URL=jdbc:mysql://localhost:3306/queuelive
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_CLIENT_SECRET=your-client-secret
KEYCLOAK_REALM=queuelive
KEYCLOAK_URL=http://localhost:8180
```

### 3 — Start the infrastructure

```bash
docker-compose up -d
```

This will start:
- **MySQL** on port `3306`
- **Keycloak** on port `8180`

### 4 — Configure Keycloak

Access [http://localhost:8180](http://localhost:8180) with `admin / admin123` and:

1. Create a **Realm** named `queuelive`
2. Create the **Roles**: `CLIENT` and `STAFF`
3. Create a **Client** named `queuelive-backend` with client authentication enabled
4. Copy the generated **Client Secret** and update it in your `.env`

### 5 — Run the application

```bash
mvn spring-boot:run
```

The API will be available at [http://localhost:8080](http://localhost:8080).

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

## 🗺️ Roadmap

Planned features for future versions:

- [ ] Dockerfile to run everything with a single `docker-compose up`
- [ ] Estimated wait time based on historical average
- [ ] Restrict users to one active queue at a time
- [ ] Push notifications when close to being called
- [ ] Admin dashboard with service metrics
- [ ] Redis for caching and horizontal scalability
- [ ] Keycloak backed by external MySQL in production
- [ ] Unit and integration tests
- [ ] Full React frontend

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.