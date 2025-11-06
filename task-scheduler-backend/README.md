# Task Scheduler Backend (Spring Boot)

RESTful API for scheduling tasks into Google Calendar with intelligent time-slot finding and task splitting.

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │ (Postman, cURL, or Future Frontend)
└──────┬──────┘
       │ HTTP/JSON
┌──────▼──────────────────────────────────┐
│     Spring Boot Backend                 │
│  ┌────────────────────────────────────┐ │
│  │  Controllers (REST API)            │ │
│  └────────┬───────────────────────────┘ │
│  ┌────────▼───────────────────────────┐ │
│  │  Services (Business Logic)         │ │
│  │  - Task scheduling algorithm       │ │
│  │  - Google Calendar integration     │ │
│  └────────┬───────────────────────────┘ │
│  ┌────────▼───────────────────────────┐ │
│  │  Repositories (Data Access)        │ │
│  └────────┬───────────────────────────┘ │
│  ┌────────▼───────────────────────────┐ │
│  │  H2 Database (Development)         │ │
│  │  PostgreSQL (Production)           │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
           │
    ┌──────▼──────┐
    │   Google    │
    │  Calendar   │
    │     API     │
    └─────────────┘
```

## 🚀 Current Status

### ✅ Completed (Foundation - ~40%)

- **Project Setup:** Maven, dependencies, configuration
- **Domain Models:** User, Task, CalendarEvent entities
- **Repository Layer:** Spring Data JPA repositories
- **DTOs:** Request/Response objects for API
- **Compiles Successfully!** ✓

### ⏳ Next Steps (To Complete MVP)

1. **JWT Security** - Token generation and authentication
2. **Service Layer** - Business logic and scheduling algorithm
3. **Controllers** - REST API endpoints
4. **Google Calendar Integration** - API client setup
5. **Testing** - Postman/cURL tests

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.2.0 |
| Language | Java 17 |
| Database | H2 (dev), PostgreSQL (prod) |
| ORM | Spring Data JPA |
| Security | Spring Security + JWT |
| External API | Google Calendar API |
| Build Tool | Maven |

## 📁 Project Structure

```
src/main/java/com/justinli/taskscheduler/
├── TaskSchedulerApplication.java      # Main application
├── model/
│   ├── User.java                      # ✅ User entity
│   ├── Task.java                      # ✅ Task entity
│   └── CalendarEvent.java             # ✅ Calendar event entity
├── repository/
│   ├── UserRepository.java            # ✅ User data access
│   ├── TaskRepository.java            # ✅ Task data access
│   └── CalendarEventRepository.java   # ✅ Event data access
├── dto/
│   ├── TaskRequest.java               # ✅ Task creation request
│   ├── TaskResponse.java              # ✅ Task response with sessions
│   ├── AuthRequest.java               # ✅ Login/register request
│   └── AuthResponse.java              # ✅ JWT token response
├── service/                           # ⏳ TODO
│   ├── UserService.java
│   ├── TaskService.java
│   ├── SchedulingService.java
│   └── GoogleCalendarService.java
├── controller/                        # ⏳ TODO
│   ├── AuthController.java
│   ├── TaskController.java
│   └── CalendarController.java
├── security/                          # ⏳ TODO
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
├── config/                            # ⏳ TODO
│   └── GoogleCalendarConfig.java
└── exception/                         # ⏳ TODO
    └── GlobalExceptionHandler.java
```

## 🎯 Planned API Endpoints

Once complete, the API will support:

### Authentication
```
POST   /api/auth/register    # Create new user
POST   /api/auth/login       # Login and get JWT token
POST   /api/auth/google      # Connect Google Calendar
```

### Tasks
```
GET    /api/tasks            # List all user's tasks
POST   /api/tasks            # Create and schedule new task
GET    /api/tasks/{id}       # Get specific task details
PUT    /api/tasks/{id}       # Update task
DELETE /api/tasks/{id}       # Delete task and calendar events
```

### Calendars
```
GET    /api/calendars        # List available Google Calendars
```

### Analytics (Future)
```
GET    /api/analytics        # Get scheduling statistics
```

## 🔧 Build & Run

### Compile
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Start Application
```bash
mvn spring-boot:run
```

Server runs on: http://localhost:8080

### Package
```bash
mvn clean package
```

Creates: `target/task-scheduler-0.0.1-SNAPSHOT.jar`

## 📊 Database Schema

### Users Table
```sql
users (
    id BIGINT PRIMARY KEY,
    name VARCHAR NOT NULL,
    email VARCHAR NOT NULL UNIQUE,
    password VARCHAR NOT NULL,
    google_access_token VARCHAR(1000),
    google_refresh_token VARCHAR(1000),
    google_token_expiry TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
```

### Tasks Table
```sql
tasks (
    id BIGINT PRIMARY KEY,
    name VARCHAR NOT NULL,
    description VARCHAR(1000),
    duration DOUBLE NOT NULL,
    max_session_hours DOUBLE,
    work_hours_start INT,
    work_hours_end INT,
    status VARCHAR NOT NULL,
    calendar_id VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    scheduled_at TIMESTAMP,
    user_id BIGINT FOREIGN KEY -> users(id)
)
```

### Calendar Events Table
```sql
calendar_events (
    id BIGINT PRIMARY KEY,
    google_event_id VARCHAR NOT NULL,
    calendar_id VARCHAR NOT NULL,
    session_number INT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_hours DOUBLE NOT NULL,
    created_at TIMESTAMP,
    task_id BIGINT FOREIGN KEY -> tasks(id)
)
```

## 🔐 Security

- **JWT Tokens:** Bearer token authentication
- **Password Encryption:** BCrypt hashing
- **OAuth 2.0:** Google Calendar API integration
- **CORS:** Configurable for frontend access

## 📝 Example Usage (Once Complete)

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Justin", "email": "justin@example.com", "password": "secure123"}'
```

### Schedule Task
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Write Essay",
    "duration": 3.0,
    "description": "CS assignment",
    "maxSessionHours": 2.0
  }'
```

## 🎓 Learning Outcomes

This project demonstrates:
- ✅ Spring Boot REST API development
- ✅ JPA entity relationships and database design
- ✅ Repository pattern with Spring Data
- ⏳ JWT authentication and Spring Security
- ⏳ External API integration (Google Calendar)
- ⏳ Algorithm implementation (scheduling logic)
- ⏳ Exception handling and validation

## 📚 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)
- [Google Calendar API](https://developers.google.com/calendar/api)

## 🐛 Troubleshooting

### Lombok not working
- Make sure annotation processing is enabled in your IDE
- Maven compiler plugin is configured (already done in pom.xml)

### Port 8080 already in use
```properties
# In application.properties
server.port=8081
```

### Database connection issues
- H2 console available at: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: (empty)

## 📈 Next Steps

**📋 Complete Implementation Plan:** See [docs/plans/IMPLEMENTATION_PLAN.md](docs/plans/IMPLEMENTATION_PLAN.md)

**Current Status:** Authentication Complete ✅ | Task Scheduling In Progress ⏳

**Immediate next:**
1. ✅ JWT authentication (COMPLETE)
2. ✅ User registration & login (COMPLETE)
3. ⏳ Implement core scheduling algorithm
4. ⏳ Create task management endpoints
5. ⏳ Integrate Google Calendar API

**📚 Planning Documents:** All planning docs are organized in [docs/plans/](docs/plans/)

---

**Status:** Authentication Layer Complete (50-55%) | Est. ~8-11 hours remaining
