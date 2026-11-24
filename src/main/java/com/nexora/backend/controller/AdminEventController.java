package com.nexora.backend.controller;

import com.nexora.backend.dto.AdminEventStatusRequest;
import com.nexora.backend.dto.EventResponse;
import com.nexora.backend.entity.Event;
import com.nexora.backend.service.EventService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {

        List<EventResponse> response = eventService
                .getAllEvents()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(
            @PathVariable Long id
    ) {

        Event event = eventService
                .getEventById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Event not found"
                    ));
        }

        return ResponseEntity.ok(toResponse(event));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EventResponse>> getEventsByStatus(
            @PathVariable String status
    ) {

        List<EventResponse> response = eventService
                .getEventsByStatus(status.toUpperCase())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateEventStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminEventStatusRequest request
    ) {

        Event event = eventService
                .getEventById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Event not found"
                    ));
        }

        String newStatus = request.status()
                .trim()
                .toUpperCase();

        if (!newStatus.equals("DRAFT")
                && !newStatus.equals("PUBLISHED")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid event status. Allowed values: DRAFT, PUBLISHED"
                    ));
        }

        event.setStatus(newStatus);

        Event updatedEvent =
                eventService.saveEvent(event);

        return ResponseEntity.ok(toResponse(updatedEvent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable Long id
    ) {

        Event event = eventService
                .getEventById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Event not found"
                    ));
        }

        try {

            eventService.deleteEvent(id);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Event cannot be deleted because it is still referenced by other data"
                    ));
        }
    }

    private EventResponse toResponse(Event event) {

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getOrganizer().getId(),
                event.getOrganizer().getFullName(),
                event.getLocation(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getImage(),
                event.getStatus()
        );
    }
}