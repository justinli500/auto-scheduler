package com.justinli.taskscheduler.service;

import com.justinli.taskscheduler.model.Task;
import com.justinli.taskscheduler.model.User;
import com.justinli.taskscheduler.repository.TaskRepository;
import com.justinli.taskscheduler.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no authentication or anonymous, create/get a default user for testing
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return userRepository.findByEmail("default@test.com")
                .orElseGet(() -> {
                    User defaultUser = new User();
                    defaultUser.setEmail("default@test.com");
                    defaultUser.setName("Default User");
                    defaultUser.setPassword("password"); // Required field
                    return userRepository.save(defaultUser);
                });
        }

        String username = authentication.getName();
        return userRepository.findByEmail(username)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    public Task createTask(Task task) {
        User currentUser = getCurrentUser();
        task.setUser(currentUser);
        return taskRepository.save(task);
    }

    public List<Task> getAllTasksForCurrentUser() {
        User currentUser = getCurrentUser();
        return taskRepository.findByUser(currentUser);
    }

    public Optional<Task> getTaskById(Long id) {
        User currentUser = getCurrentUser();
        return taskRepository.findById(id)
            .filter(task -> task.getUser().getId().equals(currentUser.getId()));
    }

    public Task updateTask(Long id, Task updatedTask) {
        User currentUser = getCurrentUser();
        Task existingTask = taskRepository.findById(id)
            .filter(task -> task.getUser().getId().equals(currentUser.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Task not found or unauthorized"));

        if (updatedTask.getName() != null) {
            existingTask.setName(updatedTask.getName());
        }
        if (updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }
        if (updatedTask.getDuration() != null) {
            existingTask.setDuration(updatedTask.getDuration());
        }
        if (updatedTask.getMaxSessionHours() != null) {
            existingTask.setMaxSessionHours(updatedTask.getMaxSessionHours());
        }
        if (updatedTask.getWorkHoursStart() != null) {
            existingTask.setWorkHoursStart(updatedTask.getWorkHoursStart());
        }
        if (updatedTask.getWorkHoursEnd() != null) {
            existingTask.setWorkHoursEnd(updatedTask.getWorkHoursEnd());
        }
        if (updatedTask.getStatus() != null) {
            existingTask.setStatus(updatedTask.getStatus());
        }
        if (updatedTask.getCalendarId() != null) {
            existingTask.setCalendarId(updatedTask.getCalendarId());
        }

        return taskRepository.save(existingTask);
    }

    public void deleteTask(Long id) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
            .filter(t -> t.getUser().getId().equals(currentUser.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Task not found or unauthorized"));

        taskRepository.delete(task);
    }
}
