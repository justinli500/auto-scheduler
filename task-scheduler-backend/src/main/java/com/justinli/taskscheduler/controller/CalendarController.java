package com.justinli.taskscheduler.controller;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.justinli.taskscheduler.dto.CalendarStatusResponse;
import com.justinli.taskscheduler.dto.CreateEventRequest;
import com.justinli.taskscheduler.dto.ScheduleTaskRequest;
import com.justinli.taskscheduler.service.GoogleCalendarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendars")
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final String frontendBaseUrl;

    public CalendarController(
        GoogleCalendarService googleCalendarService,
        @Value("${app.frontend.url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.googleCalendarService = googleCalendarService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> initiateGoogleOAuth() {
        String authorizationUrl = googleCalendarService.buildAuthorizationUrl();
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<CalendarStatusResponse> getConnectionStatus() {
        CalendarStatusResponse status = googleCalendarService.getConnectionStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/oauth2callback")
    public ResponseEntity<Void> handleOAuthCallback(@RequestParam("code") String code) {
        try {
            googleCalendarService.handleOAuthCallback(code);
            URI redirectUri = URI.create(frontendBaseUrl + "/?google=connected");
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
        } catch (IOException e) {
            URI redirectUri = URI.create(frontendBaseUrl + "/?google=error");
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCalendarList() {
        try {
            List<CalendarListEntry> calendars = googleCalendarService.getCalendarList();
            return ResponseEntity.ok(calendars);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch calendars: " + e.getMessage()));
        }
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody CreateEventRequest request) {
        try {
            ZoneId zoneId = request.getTimeZone() != null
                ? ZoneId.of(request.getTimeZone())
                : ZoneId.systemDefault();

            ZonedDateTime startTime = ZonedDateTime.parse(
                request.getStartTime(),
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            ZonedDateTime endTime = ZonedDateTime.parse(
                request.getEndTime(),
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            Event event = googleCalendarService.createEvent(
                request.getCalendarId() != null ? request.getCalendarId() : "primary",
                request.getSummary(),
                request.getDescription(),
                startTime,
                endTime
            );

            return ResponseEntity.ok(event);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create event: " + e.getMessage()));
        }
    }

    @DeleteMapping("/events/{calendarId}/{eventId}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable String calendarId,
            @PathVariable String eventId) {
        try {
            googleCalendarService.deleteEvent(calendarId, eventId);
            return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete event: " + e.getMessage()));
        }
    }

    @GetMapping("/events/{calendarId}")
    public ResponseEntity<?> getEvents(
            @PathVariable String calendarId,
            @RequestParam String timeMin,
            @RequestParam String timeMax,
            @RequestParam(required = false) String timeZone) {
        try {
            ZoneId zoneId = timeZone != null ? ZoneId.of(timeZone) : ZoneId.systemDefault();

            ZonedDateTime startTime = ZonedDateTime.parse(
                timeMin,
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            ZonedDateTime endTime = ZonedDateTime.parse(
                timeMax,
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            List<Event> events = googleCalendarService.getEvents(calendarId, startTime, endTime);
            return ResponseEntity.ok(events);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch events: " + e.getMessage()));
        }
    }

    @PostMapping("/free-slots")
    public ResponseEntity<?> findFreeTimeSlots(
            @RequestParam String calendarId,
            @RequestParam String searchStart,
            @RequestParam String searchEnd,
            @RequestParam(defaultValue = "9") int workStartHour,
            @RequestParam(defaultValue = "17") int workEndHour,
            @RequestParam(defaultValue = "30") int minSlotDurationMinutes,
            @RequestParam(required = false) String timeZone) {
        try {
            ZoneId zoneId = timeZone != null ? ZoneId.of(timeZone) : ZoneId.systemDefault();

            ZonedDateTime startTime = ZonedDateTime.parse(
                searchStart,
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            ZonedDateTime endTime = ZonedDateTime.parse(
                searchEnd,
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            List<Map<String, ZonedDateTime>> slots = googleCalendarService.findFreeTimeSlots(
                calendarId, startTime, endTime, workStartHour, workEndHour, minSlotDurationMinutes, null, 0
            );

            return ResponseEntity.ok(slots);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to find free slots: " + e.getMessage()));
        }
    }

    @PostMapping("/schedule-task")
    public ResponseEntity<?> scheduleTask(@RequestBody ScheduleTaskRequest request) {
        try {
            ZoneId zoneId = request.getTimeZone() != null
                ? ZoneId.of(request.getTimeZone())
                : ZoneId.systemDefault();

            ZonedDateTime searchStart = ZonedDateTime.parse(
                request.getSearchStartTime(),
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            ZonedDateTime searchEnd = ZonedDateTime.parse(
                request.getSearchEndTime(),
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId);

            List<Event> events = googleCalendarService.scheduleTask(
                request.getCalendarId() != null ? request.getCalendarId() : "primary",
                request.getTaskSummary(),
                request.getTaskDescription(),
                request.getTotalHours(),
                request.getMaxSessionHours(),
                searchStart,
                searchEnd,
                request.getWorkStartHour(),
                request.getWorkEndHour(),
                request.getConflictCalendarIds(),
                request.getBreakTimeMinutes()
            );

            return ResponseEntity.ok(Map.of(
                "message", "Task scheduled successfully",
                "eventsCreated", events.size(),
                "events", events
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to schedule task: " + e.getMessage()));
        }
    }
}
