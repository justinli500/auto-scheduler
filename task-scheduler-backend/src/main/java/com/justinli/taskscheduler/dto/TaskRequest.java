package com.justinli.taskscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    private Integer durationHours; // in hours

    private String dueDate; // ISO date format

    private String priority; // HIGH, MEDIUM, LOW

    private Double maxSessionHours; // defaults to 4.0 if not provided

    private Integer workHoursStart; // defaults to 9 if not provided

    private Integer workHoursEnd; // defaults to 17 if not provided

    private String calendarId; // defaults to "primary" if not provided
}
