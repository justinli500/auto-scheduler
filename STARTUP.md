# Local Development Servers

Run backend and frontend in separate terminals so the UI can reach the API.

## 1. Spring Boot backend

```bash
cd task-scheduler-backend
mvn spring-boot:run
```

This starts the API on `http://localhost:8080`.

## 2. React frontend

```bash
cd task-scheduler-frontend
npm install   # first run only
npm start
```

This starts the UI on `http://localhost:3000`, which proxies API calls to the backend.

## 3. Processes still running

```bash
lsof -i :3000
kill -9 9234 (or pid)
Or you can chain it in one line
kill -9 $(lsof -t -i:3000)
```

## Maybe these do somethign

(pkill -f "task-scheduler" && sleep 2)
