# Getting Started with Spring Boot Migration

Step-by-step guide to set up your Java Spring Boot backend.

## Step 1: Prerequisites

### Check if you have Java installed:
```bash
java -version
```

You need **Java 17 or higher**. If not installed:

**macOS:**
```bash
brew install openjdk@17
```

**Linux:**
```bash
sudo apt-get install openjdk-17-jdk
```

**Windows:**
Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use [SDKMAN](https://sdkman.io/)

### Verify Maven (build tool):
```bash
mvn -version
```

If not installed:
```bash
# macOS
brew install maven

# Linux
sudo apt-get install maven

# Windows
Download from https://maven.apache.org/download.cgi
```

## Step 2: Create Spring Boot Project

### Option A: Using Spring Initializr (Recommended)

1. Go to https://start.spring.io/

2. Configure the project:
   - **Project:** Maven
   - **Language:** Java
   - **Spring Boot:** 3.2.x (latest stable)
   - **Group:** com.yourname
   - **Artifact:** task-scheduler
   - **Name:** task-scheduler
   - **Package name:** com.yourname.taskscheduler
   - **Packaging:** Jar
   - **Java:** 17

3. Add Dependencies (click "Add Dependencies"):
   - Spring Web
   - Spring Data JPA
   - PostgreSQL Driver
   - Spring Security
   - Lombok
   - Validation
   - H2 Database (for testing)

4. Click "Generate" - downloads a ZIP file

5. Extract and move to your project location:
```bash
cd ~/Desktop/University/Projects
unzip ~/Downloads/task-scheduler.zip
cd task-scheduler
```

### Option B: Using Command Line (Alternative)

```bash
cd ~/Desktop/University/Projects
curl https://start.spring.io/starter.zip \
  -d dependencies=web,data-jpa,postgresql,security,lombok,validation,h2 \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.2.0 \
  -d baseDir=task-scheduler-backend \
  -d groupId=com.yourname \
  -d artifactId=task-scheduler \
  -o task-scheduler.zip

unzip task-scheduler.zip
cd task-scheduler-backend
```

## Step 3: Add Google Calendar Dependency

Edit `pom.xml` and add inside the `<dependencies>` section:

```xml
<!-- Google Calendar API -->
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-calendar</artifactId>
    <version>v3-rev20231123-2.0.0</version>
</dependency>
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
    <version>1.34.1</version>
</dependency>
```

## Step 4: Project Structure Setup

Create the following directory structure:

```bash
cd src/main/java/com/yourname/taskscheduler
mkdir -p config controller service repository model dto exception
```

Your structure should look like:
```
src/
├── main/
│   ├── java/com/yourname/taskscheduler/
│   │   ├── TaskSchedulerApplication.java
│   │   ├── config/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   └── exception/
│   └── resources/
│       ├── application.properties
│       └── application-dev.properties
└── test/
```

## Step 5: Configure Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=task-scheduler
server.port=8080

# Database (H2 for now - we'll switch to PostgreSQL later)
spring.datasource.url=jdbc:h2:mem:taskdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Google Calendar
google.calendar.credentials.file.path=credentials.json
google.calendar.application.name=Task Scheduler
```

## Step 6: Copy Google Credentials

Copy your Google credentials from the Python project:

```bash
cp ../task-scheduler/credentials.json src/main/resources/
```

## Step 7: Test the Setup

Run the application:

```bash
mvn spring-boot:run
```

You should see output like:
```
Started TaskSchedulerApplication in 2.5 seconds
```

Visit http://localhost:8080 - you should see a login page (from Spring Security)

## Step 8: Verify Everything Works

Test Maven build:
```bash
mvn clean install
```

Test the app:
```bash
mvn spring-boot:run
```

## Step 9: Open in IDE

**IntelliJ IDEA (Recommended):**
1. File → Open
2. Select the `task-scheduler-backend` folder
3. Wait for Maven to download dependencies

**VS Code:**
1. Install "Extension Pack for Java"
2. File → Open Folder
3. Select the `task-scheduler-backend` folder

## What's Next?

After setup is complete, we'll build:

1. ✅ Domain Models (User, Task, CalendarEvent entities)
2. ✅ Repository layer (Spring Data JPA)
3. ✅ Service layer (business logic)
4. ✅ REST Controllers (API endpoints)
5. ✅ Google Calendar integration
6. ✅ Security & Authentication

## Troubleshooting

### "mvn command not found"
- Maven isn't installed or not in PATH
- Install using brew/apt or download manually

### "Unsupported class file major version"
- Java version mismatch
- Make sure you're using Java 17+

### "Port 8080 already in use"
- Change port in application.properties:
  ```properties
  server.port=8081
  ```

### Dependencies won't download
- Check internet connection
- Try: `mvn clean install -U` (force update)

## Ready?

Once you've completed these steps and can run `mvn spring-boot:run` successfully, you're ready to start building!

Let me know when you're done with setup and I'll help you create the first models and repositories.
