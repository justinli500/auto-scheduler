package com.justinli.taskscheduler.repository;

import com.justinli.taskscheduler.model.Task;
import com.justinli.taskscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    List<Task> findByUserOrderByCreatedAtDesc(User user);

    List<Task> findByUserAndStatus(User user, Task.TaskStatus status);

    Optional<Task> findByIdAndUser(Long id, User user);
}
