package com.justinli.taskscheduler.dto;

import java.util.List;

public class ScheduleTaskRequest {
    private String calendarId;
    private String taskSummary;
    private String taskDescription;
    private double totalHours;
    private double maxSessionHours;
    private String searchStartTime;
    private String searchEndTime;
    private String timeZone;
    private int workStartHour;
    private int workEndHour;
    private List<String> conflictCalendarIds;
    private int breakTimeMinutes;

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getTaskSummary() {
        return taskSummary;
    }

    public void setTaskSummary(String taskSummary) {
        this.taskSummary = taskSummary;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }

    public double getMaxSessionHours() {
        return maxSessionHours;
    }

    public void setMaxSessionHours(double maxSessionHours) {
        this.maxSessionHours = maxSessionHours;
    }

    public String getSearchStartTime() {
        return searchStartTime;
    }

    public void setSearchStartTime(String searchStartTime) {
        this.searchStartTime = searchStartTime;
    }

    public String getSearchEndTime() {
        return searchEndTime;
    }

    public void setSearchEndTime(String searchEndTime) {
        this.searchEndTime = searchEndTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public int getWorkStartHour() {
        return workStartHour;
    }

    public void setWorkStartHour(int workStartHour) {
        this.workStartHour = workStartHour;
    }

    public int getWorkEndHour() {
        return workEndHour;
    }

    public void setWorkEndHour(int workEndHour) {
        this.workEndHour = workEndHour;
    }

    public List<String> getConflictCalendarIds() {
        return conflictCalendarIds;
    }

    public void setConflictCalendarIds(List<String> conflictCalendarIds) {
        this.conflictCalendarIds = conflictCalendarIds;
    }

    public int getBreakTimeMinutes() {
        return breakTimeMinutes;
    }

    public void setBreakTimeMinutes(int breakTimeMinutes) {
        this.breakTimeMinutes = breakTimeMinutes;
    }
}
