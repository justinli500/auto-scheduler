package com.justinli.taskscheduler.dto;

import com.justinli.taskscheduler.model.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String name;
    private String description;
    private Double duration;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private Integer sessionCount;
    private List<SessionInfo> sessions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private Integer sessionNumber;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Double duration;
        private String googleEventId;
    }

    public static TaskResponse fromTask(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDescription(task.getDescription());
        response.setDuration(task.getDuration());
        response.setStatus(task.getStatus().name());
        response.setCreatedAt(task.getCreatedAt());
        response.setScheduledAt(task.getScheduledAt());

        if (task.getCalendarEvents() != null) {
            response.setSessionCount(task.getCalendarEvents().size());
            response.setSessions(
                task.getCalendarEvents().stream()
                    .map(event -> new SessionInfo(
                        event.getSessionNumber(),
                        event.getStartTime(),
                        event.getEndTime(),
                        event.getDurationHours(),
                        event.getGoogleEventId()
                    ))
                    .toList()
            );
        } else {
            response.setSessionCount(0);
            response.setSessions(List.of());
        }

        return response;
    }
}
