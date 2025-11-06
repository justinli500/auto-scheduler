package com.justinli.taskscheduler.repository;

import com.justinli.taskscheduler.model.CalendarEvent;
import com.justinli.taskscheduler.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByTask(Task task);

    List<CalendarEvent> findByTaskOrderBySessionNumberAsc(Task task);

    Optional<CalendarEvent> findByGoogleEventId(String googleEventId);

    void deleteByTask(Task task);
}
