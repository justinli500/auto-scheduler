# Spring Boot Task Scheduler - Complete Implementation Plan

## 📋 Overview

This plan outlines the remaining work to complete the Task Scheduler Spring Boot backend, transforming it from the current authentication-only system to a fully functional task scheduling API with Google Calendar integration.

---

## 🎯 Current Status (50-55% Complete)

### ✅ Completed
- [x] Project setup with Maven
- [x] Domain models (User, Task, CalendarEvent)
- [x] Repository layer (Spring Data JPA)
- [x] DTOs for API communication
- [x] JWT authentication system
- [x] User registration and login
- [x] Security configuration
- [x] Global exception handling

### ⏳ Remaining Work
- [ ] Task scheduling algorithm
- [ ] Task management services
- [ ] Task CRUD endpoints
- [ ] Google Calendar integration
- [ ] Analytics endpoints (optional)
- [ ] Comprehensive testing
- [ ] Deployment configuration

---

## 📅 Phase 1: Core Scheduling Logic (3-4 hours)

### Objective
Port the Python scheduling algorithm to Java and implement task management business logic.

### Tasks

#### 1.1 Create SchedulingService (2-3 hours)
**File:** `src/main/java/com/justinli/taskscheduler/service/SchedulingService.java`

**Responsibilities:**
- Find free time slots in calendar
- Split large tasks into multiple sessions
- Prioritize earlier time slots
- Avoid weekends
- Respect work hour constraints

**Key Methods:**
```java
public List<TimeSlot> findFreeSlots(
    LocalDateTime startDate,
    LocalDateTime endDate,
    int workHoursStart,
    int workHoursEnd,
    List<CalendarEvent> existingEvents
)

public List<SessionInfo> splitTask(
    double totalDuration,
    double maxSessionHours
)

public List<ScheduleProposal> scheduleTask(
    Task task,
    List<TimeSlot> availableSlots
)
```

**Algorithm Steps:**
1. Scan calendar for next N days (lookahead period)
2. Identify free blocks during work hours (skip weekends)
3. Calculate required sessions based on max session length
4. Assign sessions to earliest available slots
5. Return schedule proposal

**Port from Python:**
- Extract logic from `task_scheduler.py`
- Convert Python datetime to Java LocalDateTime
- Adapt to Java's functional programming style

#### 1.2 Create TaskService (1 hour)
**File:** `src/main/java/com/justinli/taskscheduler/service/TaskService.java`

**Responsibilities:**
- CRUD operations for tasks
- Call SchedulingService to find slots
- Persist task and calendar events
- Update task status

**Key Methods:**
```java
public TaskResponse createTask(TaskRequest request, User user)
public TaskResponse getTask(Long taskId, User user)
public List<TaskResponse> getAllTasks(User user)
public TaskResponse updateTask(Long taskId, TaskRequest request, User user)
public void deleteTask(Long taskId, User user)
public List<TaskResponse> getTasksByStatus(TaskStatus status, User user)
```

**Business Logic:**
1. Validate task request (duration > 0, valid work hours)
2. Set defaults (max session = 4hrs, lookahead = 14 days)
3. Call SchedulingService to find slots
4. If no slots found → throw exception
5. Create Task entity and CalendarEvent entities
6. Save to database
7. Return TaskResponse with session info

#### 1.3 Add DTOs for Scheduling
**Files:**
- `dto/TimeSlot.java` - Represents a free time slot
- `dto/SessionInfo.java` - Session details (already in TaskResponse)
- `dto/ScheduleProposal.java` - Proposed schedule before creation

**TimeSlot.java:**
```java
public class TimeSlot {
    private LocalDateTime start;
    private LocalDateTime end;
    private double durationHours;
}
```

---

## 📅 Phase 2: Task Management API (1-2 hours)

### Objective
Create REST endpoints for task management (CRUD operations).

### Tasks

#### 2.1 Create TaskController (1 hour)
**File:** `src/main/java/com/justinli/taskscheduler/controller/TaskController.java`

**Endpoints:**
```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    // Create new task
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
        @Valid @RequestBody TaskRequest request
    )

    // Get all tasks for current user
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
        @RequestParam(required = false) String status
    )

    // Get specific task
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(
        @PathVariable Long id
    )

    // Update task
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
        @PathVariable Long id,
        @Valid @RequestBody TaskRequest request
    )

    // Delete task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
        @PathVariable Long id
    )

    // Get tasks for today
    @GetMapping("/today")
    public ResponseEntity<List<TaskResponse>> getTodaysTasks()
}
```

**Features:**
- All endpoints require authentication (automatic via JWT filter)
- Use `@CurrentUser` annotation to get authenticated user
- Return proper HTTP status codes (201, 200, 204, 404)
- Validate request bodies
- Handle exceptions gracefully

#### 2.2 Add Custom Exceptions (30 min)
**Files:**
- `exception/TaskNotFoundException.java`
- `exception/NoFreeSlotsException.java`
- `exception/InvalidTaskException.java`

**Update GlobalExceptionHandler:**
```java
@ExceptionHandler(TaskNotFoundException.class)
public ResponseEntity<ErrorResponse> handleTaskNotFound(...)

@ExceptionHandler(NoFreeSlotsException.class)
public ResponseEntity<ErrorResponse> handleNoFreeSlots(...)

@ExceptionHandler(InvalidTaskException.class)
public ResponseEntity<ErrorResponse> handleInvalidTask(...)
```

#### 2.3 Add CurrentUser Annotation (30 min)
**File:** `security/CurrentUser.java`

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {}
```

**Usage in controllers:**
```java
public ResponseEntity<TaskResponse> createTask(
    @Valid @RequestBody TaskRequest request,
    @CurrentUser User user
) {
    return taskService.createTask(request, user);
}
```

---

## 📅 Phase 3: Google Calendar Integration (2-3 hours)

### Objective
Integrate with Google Calendar API to actually create events when tasks are scheduled.

### Tasks

#### 3.1 Create GoogleCalendarService (2 hours)
**File:** `src/main/java/com/justinli/taskscheduler/service/GoogleCalendarService.java`

**Responsibilities:**
- Initialize Google Calendar API client
- Create calendar events
- Delete calendar events
- List user's calendars
- Fetch existing events

**Key Methods:**
```java
public Calendar getCalendarService(User user)
public List<CalendarListEntry> listCalendars(User user)
public Event createCalendarEvent(
    User user,
    String calendarId,
    String title,
    String description,
    LocalDateTime start,
    LocalDateTime end
)
public void deleteCalendarEvent(User user, String calendarId, String eventId)
public List<Event> getEventsInRange(
    User user,
    String calendarId,
    LocalDateTime start,
    LocalDateTime end
)
```

**Implementation Steps:**
1. Load credentials from `credentials.json`
2. Use stored Google tokens from User entity
3. Handle token refresh if expired
4. Create Calendar service instance
5. Make API calls to create/delete events

**Integration with SchedulingService:**
- Update SchedulingService to call GoogleCalendarService
- Fetch existing events to avoid conflicts
- Create calendar events for each session
- Store Google event IDs in CalendarEvent entities

#### 3.2 Create GoogleCalendarConfig (30 min)
**File:** `config/GoogleCalendarConfig.java`

```java
@Configuration
public class GoogleCalendarConfig {

    @Value("${google.calendar.credentials.file.path}")
    private String credentialsFilePath;

    @Value("${google.calendar.application.name}")
    private String applicationName;

    @Bean
    public GoogleCredential googleCredential() {
        // Load and configure Google credentials
    }
}
```

#### 3.3 Add Google OAuth Flow (1 hour)
**File:** `controller/CalendarController.java`

**Endpoints:**
```java
@RestController
@RequestMapping("/api/calendars")
public class CalendarController {

    // Initiate Google OAuth
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> initiateGoogleOAuth()

    // OAuth callback
    @GetMapping("/oauth2callback")
    public ResponseEntity<String> handleOAuthCallback(@RequestParam String code)

    // List user's Google Calendars
    @GetMapping
    public ResponseEntity<List<CalendarInfo>> listCalendars()
}
```

**OAuth Flow:**
1. User clicks "Connect Google Calendar"
2. Backend generates OAuth URL
3. User authorizes in browser
4. Google redirects to callback with code
5. Backend exchanges code for tokens
6. Tokens saved to User entity
7. User can now schedule tasks

---

## 📅 Phase 4: Testing & Refinement (2-3 hours)

### Objective
Ensure all features work correctly through comprehensive testing.

### Tasks

#### 4.1 Manual Testing (1 hour)
**Test Scenarios:**

1. **Authentication Flow:**
   - Register new user
   - Login
   - Access protected endpoint

2. **Task Scheduling Flow:**
   - Create task (3 hours, max session 2 hours)
   - Verify task split into 2 sessions
   - Check database for Task and CalendarEvent entries
   - Verify events created in Google Calendar

3. **Task Management:**
   - List all tasks
   - Get specific task
   - Update task duration
   - Delete task
   - Verify calendar events deleted

4. **Edge Cases:**
   - Try to schedule task with no free slots
   - Schedule task with invalid duration (negative)
   - Try to access another user's task
   - Expired JWT token
   - Invalid Google Calendar credentials

**Create Test Script:**
```bash
#!/bin/bash
# test-api.sh

# 1. Register
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@test.com","password":"pass123"}' \
  | jq -r '.token')

# 2. Create task
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Write Report","duration":3,"maxSessionHours":2}'

# 3. List tasks
curl http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN"

# ... more tests
```

#### 4.2 Unit Tests (1-2 hours)
**Files:**
- `service/SchedulingServiceTest.java`
- `service/TaskServiceTest.java`
- `controller/TaskControllerTest.java`

**Example Test:**
```java
@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Test
    public void testCreateTask() {
        TaskRequest request = new TaskRequest();
        request.setName("Test Task");
        request.setDuration(3.0);

        TaskResponse response = taskService.createTask(request, testUser);

        assertNotNull(response);
        assertEquals("Test Task", response.getName());
        assertEquals(2, response.getSessionCount()); // 3hrs / 2hr max
    }

    @Test
    public void testNoFreeSlotsException() {
        // Test with impossible constraints
        assertThrows(NoFreeSlotsException.class, () -> {
            taskService.createTask(impossibleRequest, testUser);
        });
    }
}
```

#### 4.3 Integration Tests (Optional, 1 hour)
Test complete flows end-to-end:
- User registration → Task creation → Google Calendar event

---

## 📅 Phase 5: Documentation & Deployment (1-2 hours)

### Objective
Prepare the application for production and create comprehensive documentation.

### Tasks

#### 5.1 Update README (30 min)
**Add sections:**
- API endpoint documentation
- Authentication flow diagram
- Example cURL commands for all endpoints
- Environment variables
- Deployment instructions

#### 5.2 Add Swagger/OpenAPI Documentation (30 min)
**Dependencies:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

**Configuration:**
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Task Scheduler API")
                .version("1.0")
                .description("REST API for intelligent task scheduling"));
    }
}
```

**Access:** http://localhost:8080/swagger-ui.html

#### 5.3 Production Configuration (30 min)
**File:** `application-prod.properties`

```properties
# PostgreSQL for production
spring.datasource.url=jdbc:postgresql://localhost:5432/taskscheduler
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Security
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# Google Calendar
google.calendar.credentials.file.path=${GOOGLE_CREDENTIALS_PATH}
```

#### 5.4 Docker Configuration (30 min)
**File:** `Dockerfile`

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/task-scheduler-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**File:** `docker-compose.yml`

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=postgres
      - DB_PASSWORD=secret
    depends_on:
      - db
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: taskscheduler
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## 📅 Optional Enhancements (If Time Permits)

### Analytics Endpoints
**File:** `controller/AnalyticsController.java`

```java
@GetMapping("/api/analytics")
public ResponseEntity<AnalyticsResponse> getAnalytics() {
    // Total tasks created
    // Tasks completed vs pending
    // Total hours scheduled
    // Average session length
    // Productivity trends
}
```

### Task Categories/Tags
```java
// Add to Task entity
@ElementCollection
private List<String> tags;

// Filter endpoint
@GetMapping("/api/tasks")
public List<TaskResponse> getTasks(@RequestParam List<String> tags)
```

### Recurring Tasks
```java
// Add to Task entity
private RecurrencePattern recurrence; // DAILY, WEEKLY, MONTHLY

// Service method
public List<Task> createRecurringTasks(TaskRequest request, int occurrences)
```

### Email Notifications
```java
// Send email when task is due soon
@Scheduled(cron = "0 0 8 * * *") // 8 AM daily
public void sendTaskReminders()
```

---

## 📊 Implementation Timeline

### Recommended Approach: Iterative Development

**Session 1 (3-4 hours): Core Scheduling**
- SchedulingService with algorithm
- Basic TaskService
- TimeSlot DTOs
- Manual testing with hardcoded calendar data

**Session 2 (2 hours): Task API**
- TaskController with all endpoints
- Exception handling
- CurrentUser annotation
- Test all CRUD operations

**Session 3 (2-3 hours): Google Calendar**
- GoogleCalendarService
- OAuth flow
- Calendar event creation
- End-to-end testing

**Session 4 (1-2 hours): Polish**
- Unit tests
- Documentation
- Bug fixes
- Deployment prep

**Total Time: ~8-11 hours**

---

## 🎯 Success Criteria

### Must Have (MVP)
- [x] User authentication working
- [ ] Create task endpoint functional
- [ ] Tasks saved to database
- [ ] Basic scheduling algorithm works
- [ ] List tasks endpoint works
- [ ] Delete task endpoint works
- [ ] Google Calendar events created
- [ ] No critical bugs

### Should Have
- [ ] Update task endpoint
- [ ] Task filtering by status
- [ ] Comprehensive error handling
- [ ] Unit tests for core logic
- [ ] API documentation
- [ ] OAuth flow for Google Calendar

### Nice to Have
- [ ] Analytics endpoint
- [ ] Task categories/tags
- [ ] Email notifications
- [ ] Docker deployment
- [ ] High test coverage (>80%)

---

## 🔄 Iterative Development Strategy

1. **Start Simple:** Get basic task creation working without Google Calendar
2. **Add Complexity:** Integrate scheduling algorithm
3. **External Integration:** Add Google Calendar
4. **Polish:** Tests, docs, deployment

**Don't:**
- Try to build everything at once
- Optimize prematurely
- Over-engineer the solution

**Do:**
- Test each feature as you build it
- Commit code frequently
- Keep the Python version as reference
- Ask for help when stuck

---

## 📚 Resources & References

### Spring Boot Docs
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring REST](https://spring.io/guides/gs/rest-service/)

### Google Calendar API
- [Java Quickstart](https://developers.google.com/calendar/api/quickstart/java)
- [Events: insert](https://developers.google.com/calendar/api/v3/reference/events/insert)
- [OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)

### Testing
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito](https://site.mockito.org/)

### Deployment
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
- [Railway](https://railway.app/) - Easy deployment
- [Render](https://render.com/) - Free tier available

---

## ✅ Checklist

Use this checklist to track progress:

### Phase 1: Core Scheduling
- [ ] Create SchedulingService.java
- [ ] Implement findFreeSlots() method
- [ ] Implement splitTask() method
- [ ] Implement scheduleTask() method
- [ ] Create TimeSlot.java DTO
- [ ] Create TaskService.java
- [ ] Implement createTask() logic
- [ ] Test scheduling algorithm manually

### Phase 2: Task API
- [ ] Create TaskController.java
- [ ] Implement POST /api/tasks
- [ ] Implement GET /api/tasks
- [ ] Implement GET /api/tasks/{id}
- [ ] Implement PUT /api/tasks/{id}
- [ ] Implement DELETE /api/tasks/{id}
- [ ] Add custom exceptions
- [ ] Update GlobalExceptionHandler
- [ ] Create @CurrentUser annotation
- [ ] Test all endpoints with Postman/cURL

### Phase 3: Google Calendar
- [ ] Add Google Calendar dependencies
- [ ] Create GoogleCalendarConfig.java
- [ ] Create GoogleCalendarService.java
- [ ] Implement calendar client initialization
- [ ] Implement createCalendarEvent()
- [ ] Implement deleteCalendarEvent()
- [ ] Implement listCalendars()
- [ ] Create CalendarController.java
- [ ] Implement OAuth flow
- [ ] Integrate with TaskService
- [ ] Test end-to-end task scheduling

### Phase 4: Testing
- [ ] Write manual test script
- [ ] Test all happy paths
- [ ] Test error cases
- [ ] Write unit tests for SchedulingService
- [ ] Write unit tests for TaskService
- [ ] Write controller tests
- [ ] Fix any bugs found

### Phase 5: Documentation & Deployment
- [ ] Update README with API docs
- [ ] Add Swagger/OpenAPI
- [ ] Create production config
- [ ] Create Dockerfile
- [ ] Create docker-compose.yml
- [ ] Test local deployment
- [ ] Deploy to cloud (optional)

---

## 🎓 Learning Outcomes

By completing this project, you'll have hands-on experience with:

✅ RESTful API design
✅ Spring Boot application development
✅ JWT authentication
✅ Spring Security configuration
✅ JPA entity relationships
✅ Service layer architecture
✅ Exception handling
✅ External API integration (Google Calendar)
✅ OAuth 2.0 flow
✅ Algorithm implementation
✅ Unit testing with JUnit
✅ Docker containerization
✅ Database design
✅ API documentation

**Perfect for backend engineering interviews!**

---

**Next Step:** Start with Phase 1 (Core Scheduling Logic)

**Estimated Completion:** 8-11 hours total, spread across multiple sessions

Good luck! 🚀
