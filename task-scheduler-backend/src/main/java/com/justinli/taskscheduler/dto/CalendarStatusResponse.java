package com.justinli.taskscheduler.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarStatusResponse {
    private boolean connected;
    private String calendarEmail;
    private String lastSynced;
    private String message;
}
