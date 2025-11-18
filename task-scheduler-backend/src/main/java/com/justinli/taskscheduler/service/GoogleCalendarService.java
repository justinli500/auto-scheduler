package com.justinli.taskscheduler.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.justinli.taskscheduler.dto.CalendarStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoogleCalendarService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String DEFAULT_USER = "default-user";

    private final GoogleAuthorizationCodeFlow authorizationCodeFlow;
    private final String redirectUri;
    private final String applicationName;

    public GoogleCalendarService(
        @Value("${google.calendar.credentials.file.path}") String credentialsFilePath,
        @Value("${app.oauth.redirect-uri:http://localhost:8080/api/calendars/oauth2callback}") String redirectUri,
        @Value("${app.oauth.tokens-dir:${user.home}/.task-scheduler-google}") String tokensDirectory,
        @Value("${google.calendar.application.name:Task Scheduler}") String applicationName
    ) {
        this.redirectUri = redirectUri;
        this.applicationName = applicationName;

        try (FileInputStream inputStream = new FileInputStream(credentialsFilePath)) {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            File tokensDir = new File(tokensDirectory);
            if (!tokensDir.exists()) {
                tokensDir.mkdirs();
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(inputStream)
            );

            this.authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR)
            )
                .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize Google Calendar service: " + e.getMessage(), e);
        }
    }

    public String buildAuthorizationUrl() {
        AuthorizationCodeRequestUrl url = authorizationCodeFlow.newAuthorizationUrl();
        url.setRedirectUri(redirectUri);
        return url.build();
    }

    public void handleOAuthCallback(String code) throws IOException {
        TokenResponse tokenResponse = authorizationCodeFlow
            .newTokenRequest(code)
            .setRedirectUri(redirectUri)
            .execute();

        authorizationCodeFlow.createAndStoreCredential(tokenResponse, DEFAULT_USER);
    }

    public CalendarStatusResponse getConnectionStatus() {
        CalendarStatusResponse response = new CalendarStatusResponse();
        try {
            Credential credential = authorizationCodeFlow.loadCredential(DEFAULT_USER);
            if (credential == null || credential.getAccessToken() == null) {
                response.setConnected(false);
                response.setMessage("No Google Calendar connection found.");
                return response;
            }

            boolean refreshAttempted = false;
            if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
                refreshAttempted = credential.refreshToken();
            }

            response.setConnected(true);
            response.setLastSynced(LocalDateTime.now().toString());
            response.setMessage(refreshAttempted ? "Token refreshed successfully." : "Token is valid.");
        } catch (IOException e) {
            response.setConnected(false);
            response.setMessage("Unable to read stored credentials: " + e.getMessage());
        }
        return response;
    }

    private Calendar getCalendarService() throws IOException, GeneralSecurityException {
        Credential credential = authorizationCodeFlow.loadCredential(DEFAULT_USER);
        if (credential == null) {
            throw new IllegalStateException("Not connected to Google Calendar. Please connect first.");
        }

        return new Calendar.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON_FACTORY,
            credential
        )
            .setApplicationName(applicationName)
            .build();
    }

    public List<CalendarListEntry> getCalendarList() throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService();
        CalendarList calendarList = service.calendarList().list().execute();
        return calendarList.getItems();
    }

    public Event createEvent(String calendarId, String summary, String description,
                            ZonedDateTime startTime, ZonedDateTime endTime)
            throws IOException, GeneralSecurityException {

        Calendar service = getCalendarService();

        Event event = new Event()
            .setSummary(summary)
            .setDescription(description);

        EventDateTime start = new EventDateTime()
            .setDateTime(new com.google.api.client.util.DateTime(Date.from(startTime.toInstant())))
            .setTimeZone(startTime.getZone().getId());
        event.setStart(start);

        EventDateTime end = new EventDateTime()
            .setDateTime(new com.google.api.client.util.DateTime(Date.from(endTime.toInstant())))
            .setTimeZone(endTime.getZone().getId());
        event.setEnd(end);

        return service.events().insert(calendarId, event).execute();
    }

    public Event updateEvent(String calendarId, String eventId, String summary,
                            String description, ZonedDateTime startTime, ZonedDateTime endTime)
            throws IOException, GeneralSecurityException {

        Calendar service = getCalendarService();

        Event event = service.events().get(calendarId, eventId).execute();

        if (summary != null) {
            event.setSummary(summary);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (startTime != null) {
            EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(Date.from(startTime.toInstant())))
                .setTimeZone(startTime.getZone().getId());
            event.setStart(start);
        }
        if (endTime != null) {
            EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(Date.from(endTime.toInstant())))
                .setTimeZone(endTime.getZone().getId());
            event.setEnd(end);
        }

        return service.events().update(calendarId, eventId, event).execute();
    }

    public void deleteEvent(String calendarId, String eventId)
            throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService();
        service.events().delete(calendarId, eventId).execute();
    }

    public List<Event> getEvents(String calendarId, ZonedDateTime timeMin, ZonedDateTime timeMax)
            throws IOException, GeneralSecurityException {

        Calendar service = getCalendarService();

        Events events = service.events().list(calendarId)
            .setTimeMin(new com.google.api.client.util.DateTime(Date.from(timeMin.toInstant())))
            .setTimeMax(new com.google.api.client.util.DateTime(Date.from(timeMax.toInstant())))
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute();

        return events.getItems();
    }

    public List<Event> getEventsFromCalendars(List<String> calendarIds, ZonedDateTime timeMin, ZonedDateTime timeMax)
            throws IOException, GeneralSecurityException {

        Calendar service = getCalendarService();
        List<Event> allEvents = new ArrayList<>();

        System.out.println("Fetching events from " + calendarIds.size() + " calendar(s) for conflict detection");

        // Fetch events from each specified calendar
        for (String calendarId : calendarIds) {
            try {
                Events events = service.events().list(calendarId)
                    .setTimeMin(new com.google.api.client.util.DateTime(Date.from(timeMin.toInstant())))
                    .setTimeMax(new com.google.api.client.util.DateTime(Date.from(timeMax.toInstant())))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

                List<Event> calendarEvents = events.getItems();
                if (calendarEvents != null && !calendarEvents.isEmpty()) {
                    System.out.println("  Found " + calendarEvents.size() + " events in calendar: " + calendarId);
                    allEvents.addAll(calendarEvents);
                }
            } catch (Exception e) {
                System.err.println("  Error fetching events from calendar " + calendarId + ": " + e.getMessage());
                // Continue with other calendars even if one fails
            }
        }

        // Filter out all-day events (they don't have dateTime, only date)
        allEvents = allEvents.stream()
            .filter(e -> e.getStart().getDateTime() != null)
            .collect(Collectors.toList());

        // Sort all events by start time
        allEvents.sort(Comparator.comparing(e ->
            ZonedDateTime.parse(
                e.getStart().getDateTime().toString(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
        ));

        System.out.println("Total events found: " + allEvents.size() + " (excluding all-day events)");

        return allEvents;
    }

    public List<Map<String, ZonedDateTime>> findFreeTimeSlots(
            String calendarId,
            ZonedDateTime searchStart,
            ZonedDateTime searchEnd,
            int workStartHour,
            int workEndHour,
            int minSlotDurationMinutes,
            List<String> conflictCalendarIds,
            int breakTimeMinutes
    ) throws IOException, GeneralSecurityException {

        // Fetch events from the specified calendars for conflict detection
        List<Event> events;
        if (conflictCalendarIds != null && !conflictCalendarIds.isEmpty()) {
            System.out.println("Checking specific calendars for conflicts: " + conflictCalendarIds);
            events = getEventsFromCalendars(conflictCalendarIds, searchStart, searchEnd);
        } else {
            System.out.println("Checking only target calendar: " + calendarId);
            events = getEvents(calendarId, searchStart, searchEnd);
        }

        List<Map<String, ZonedDateTime>> freeSlots = new ArrayList<>();

        ZonedDateTime current = searchStart;

        while (current.isBefore(searchEnd)) {
            ZonedDateTime dayStart = current.withHour(workStartHour).withMinute(0).withSecond(0);
            ZonedDateTime dayEnd;

            // Handle overnight work hours (e.g., 11 AM to midnight)
            if (workEndHour == 0) {
                // workEndHour of 0 means midnight (end of day), so add 24 hours
                dayEnd = current.withHour(23).withMinute(59).withSecond(59);
            } else if (workEndHour < workStartHour) {
                // Overnight shift (e.g., 10 PM to 6 AM)
                dayEnd = current.plusDays(1).withHour(workEndHour).withMinute(0).withSecond(0);
            } else {
                // Normal same-day hours
                dayEnd = current.withHour(workEndHour).withMinute(0).withSecond(0);
            }

            if (dayStart.isBefore(searchStart)) {
                dayStart = searchStart;
            }
            if (dayEnd.isAfter(searchEnd)) {
                dayEnd = searchEnd;
            }

            final ZonedDateTime currentDay = current;
            List<Event> dayEvents = events.stream()
                .filter(e -> {
                    // Skip all-day events (they use 'date' instead of 'dateTime')
                    if (e.getStart().getDateTime() == null) {
                        return false;
                    }
                    ZonedDateTime eventStart = ZonedDateTime.parse(
                        e.getStart().getDateTime().toString(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    );
                    return eventStart.toLocalDate().equals(currentDay.toLocalDate());
                })
                .sorted(Comparator.comparing(e ->
                    ZonedDateTime.parse(
                        e.getStart().getDateTime().toString(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    )
                ))
                .collect(Collectors.toList());

            ZonedDateTime slotStart = dayStart;

            for (Event event : dayEvents) {
                ZonedDateTime eventStart = ZonedDateTime.parse(
                    event.getStart().getDateTime().toString(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                );
                ZonedDateTime eventEnd = ZonedDateTime.parse(
                    event.getEnd().getDateTime().toString(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                );

                if (slotStart.isBefore(eventStart)) {
                    long minutes = java.time.Duration.between(slotStart, eventStart).toMinutes();
                    if (minutes >= minSlotDurationMinutes) {
                        Map<String, ZonedDateTime> slot = new HashMap<>();
                        slot.put("start", slotStart);
                        slot.put("end", eventStart);
                        freeSlots.add(slot);
                    }
                }

                // Add break time after the event ends
                if (eventEnd.isAfter(slotStart)) {
                    slotStart = eventEnd.plusMinutes(breakTimeMinutes);
                }
            }

            if (slotStart.isBefore(dayEnd)) {
                long minutes = java.time.Duration.between(slotStart, dayEnd).toMinutes();
                if (minutes >= minSlotDurationMinutes) {
                    Map<String, ZonedDateTime> slot = new HashMap<>();
                    slot.put("start", slotStart);
                    slot.put("end", dayEnd);
                    freeSlots.add(slot);
                }
            }

            current = current.plusDays(1);
        }

        return freeSlots;
    }

    public List<Event> scheduleTask(
            String calendarId,
            String taskSummary,
            String taskDescription,
            double totalHours,
            double maxSessionHours,
            ZonedDateTime searchStart,
            ZonedDateTime searchEnd,
            int workStartHour,
            int workEndHour,
            List<String> conflictCalendarIds,
            int breakTimeMinutes
    ) throws IOException, GeneralSecurityException {

        int totalMinutes = (int) (totalHours * 60);
        int maxSessionMinutes = (int) (maxSessionHours * 60);
        int remainingMinutes = totalMinutes;

        // Minimum slot duration should be at least 30 minutes or the smallest session we need
        int minSlotDuration = Math.min(30, Math.min(remainingMinutes, maxSessionMinutes));

        // Check specified calendars for conflicts, but schedule in the target calendar
        List<Map<String, ZonedDateTime>> freeSlots = findFreeTimeSlots(
            calendarId, searchStart, searchEnd, workStartHour, workEndHour, minSlotDuration, conflictCalendarIds, breakTimeMinutes
        );

        // Log free slots found for debugging
        System.out.println("=== Task Scheduling Debug ===");
        System.out.println("Total task duration: " + totalHours + " hours (" + totalMinutes + " minutes)");
        System.out.println("Max session: " + maxSessionHours + " hours");
        System.out.println("Work hours: " + workStartHour + ":00 - " + workEndHour + ":00");
        System.out.println("Search range: " + searchStart + " to " + searchEnd);
        System.out.println("Found " + freeSlots.size() + " free slots:");
        for (int i = 0; i < freeSlots.size(); i++) {
            Map<String, ZonedDateTime> slot = freeSlots.get(i);
            ZonedDateTime start = slot.get("start");
            ZonedDateTime end = slot.get("end");
            long duration = java.time.Duration.between(start, end).toMinutes();
            System.out.println("  Slot " + (i + 1) + ": " + start + " to " + end + " (" + duration + " minutes)");
        }

        List<Event> createdEvents = new ArrayList<>();
        int sessionNumber = 1;
        ZonedDateTime lastSessionEnd = null; // Track when the last session ended

        for (Map<String, ZonedDateTime> slot : freeSlots) {
            if (remainingMinutes <= 0) {
                break;
            }

            ZonedDateTime slotStart = slot.get("start");
            ZonedDateTime slotEnd = slot.get("end");

            // If we have a previous session, ensure break time between sessions
            if (lastSessionEnd != null) {
                ZonedDateTime earliestNextStart = lastSessionEnd.plusMinutes(breakTimeMinutes);
                if (slotStart.isBefore(earliestNextStart)) {
                    // Adjust slot start to respect break time
                    slotStart = earliestNextStart;

                    // If adjusted start is beyond slot end, skip this slot
                    if (!slotStart.isBefore(slotEnd)) {
                        System.out.println("  Skipping slot (no room after break time): " + slot.get("start") + " to " + slotEnd);
                        continue;
                    }
                }
            }

            long slotDuration = java.time.Duration.between(slotStart, slotEnd).toMinutes();

            // Calculate what we actually need for this session
            int neededSessionDuration = Math.min(remainingMinutes, maxSessionMinutes);

            // Skip slots that can't fit the full session we need
            // Only use slots that can fit at least 30 minutes AND the session we're trying to schedule
            if (slotDuration < neededSessionDuration) {
                System.out.println("  Skipping slot (too small): " + slotStart + " to " + slotEnd +
                                 " (" + slotDuration + " min available, " + neededSessionDuration + " min needed)");
                continue; // Skip this slot - not big enough for our needed session
            }

            // Now we know the slot can fit our session
            int sessionDuration = neededSessionDuration;
            System.out.println("  Using slot: " + slotStart + " to " + slotEnd +
                             " (scheduling " + sessionDuration + " minute session)");

            ZonedDateTime sessionEnd = slotStart.plusMinutes(sessionDuration);

            String eventSummary = String.format("%s (Session %d)", taskSummary, sessionNumber);
            String eventDescription = String.format(
                "%s\n\nSession %d of task - %.1f hours remaining",
                taskDescription != null ? taskDescription : "",
                sessionNumber,
                remainingMinutes / 60.0
            );

            Event event = createEvent(calendarId, eventSummary, eventDescription, slotStart, sessionEnd);
            createdEvents.add(event);

            remainingMinutes -= sessionDuration;
            lastSessionEnd = sessionEnd; // Update last session end time
            sessionNumber++;
        }

        if (remainingMinutes > 0) {
            throw new IllegalStateException(
                String.format("Could not schedule entire task. %.1f hours remaining unscheduled.",
                remainingMinutes / 60.0)
            );
        }

        return createdEvents;
    }
}
