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

    @NotBlank(message = "Task name is required")
    private String name;

    private String description;

    @Positive(message = "Duration must be positive")
    private Double duration; // in hours

    private Double maxSessionHours; // defaults to 4.0 if not provided

    private Integer workHoursStart; // defaults to 9 if not provided

    private Integer workHoursEnd; // defaults to 17 if not provided

    private String calendarId; // defaults to "primary" if not provided
}
