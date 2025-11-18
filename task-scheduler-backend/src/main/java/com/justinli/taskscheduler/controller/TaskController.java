package com.justinli.taskscheduler.controller;

import com.justinli.taskscheduler.dto.TaskRequest;
import com.justinli.taskscheduler.model.Task;
import com.justinli.taskscheduler.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest request) {
        Task task = new Task();
        task.setName(request.getTitle());
        task.setDescription(request.getDescription());

        if (request.getDurationHours() != null) {
            task.setDuration(request.getDurationHours().doubleValue());
        }

        if (request.getDueDate() != null && !request.getDueDate().isEmpty()) {
            task.setDueDate(LocalDateTime.parse(request.getDueDate() + "T23:59:59"));
        }

        if (request.getPriority() != null) {
            try {
                task.setPriority(Task.TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                task.setPriority(Task.TaskPriority.MEDIUM);
            }
        } else {
            task.setPriority(Task.TaskPriority.MEDIUM);
        }

        task.setMaxSessionHours(request.getMaxSessionHours() != null ? request.getMaxSessionHours() : 4.0);
        task.setWorkHoursStart(request.getWorkHoursStart() != null ? request.getWorkHoursStart() : 9);
        task.setWorkHoursEnd(request.getWorkHoursEnd() != null ? request.getWorkHoursEnd() : 17);
        task.setCalendarId(request.getCalendarId() != null ? request.getCalendarId() : "primary");

        Task createdTask = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasksForCurrentUser();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        try {
            Task updatedTask = new Task();
            updatedTask.setName(request.getTitle());
            updatedTask.setDescription(request.getDescription());

            if (request.getDurationHours() != null) {
                updatedTask.setDuration(request.getDurationHours().doubleValue());
            }

            if (request.getDueDate() != null && !request.getDueDate().isEmpty()) {
                updatedTask.setDueDate(LocalDateTime.parse(request.getDueDate() + "T23:59:59"));
            }

            if (request.getPriority() != null) {
                try {
                    updatedTask.setPriority(Task.TaskPriority.valueOf(request.getPriority().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Keep existing priority if invalid
                }
            }

            updatedTask.setMaxSessionHours(request.getMaxSessionHours());
            updatedTask.setWorkHoursStart(request.getWorkHoursStart());
            updatedTask.setWorkHoursEnd(request.getWorkHoursEnd());
            updatedTask.setCalendarId(request.getCalendarId());

            Task result = taskService.updateTask(id, updatedTask);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
