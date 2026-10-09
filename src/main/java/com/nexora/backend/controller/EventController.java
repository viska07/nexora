package com.nexora.backend.controller;

import com.nexora.backend.dto.EventRequest;
import com.nexora.backend.dto.EventResponse;
import com.nexora.backend.entity.Category;
import com.nexora.backend.entity.Event;
import com.nexora.backend.entity.User;
import com.nexora.backend.service.CategoryService;
import com.nexora.backend.service.EventService;
import com.nexora.backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final CategoryService categoryService;
    private final UserService userService;

    public EventController(
            EventService eventService,
            CategoryService categoryService,
            UserService userService
    ) {
        this.eventService = eventService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    // =========================
    // GET ALL EVENTS
    // =========================

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {

        List<EventResponse> responses = eventService
                .getAllEvents()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================
    // SEARCH EVENTS BY TITLE
    // =========================

    @GetMapping("/search")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @RequestParam String title
    ) {

        List<EventResponse> responses = eventService
                .searchEventsByTitle(title)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================
    // GET EVENTS BY STATUS
    // =========================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EventResponse>> getEventsByStatus(
            @PathVariable String status
    ) {

        List<EventResponse> responses = eventService
                .getEventsByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================
    // GET EVENTS BY CATEGORY
    // =========================

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<EventResponse>> getEventsByCategory(
            @PathVariable Long categoryId
    ) {

        List<EventResponse> responses = eventService
                .getEventsByCategory(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================
    // GET EVENTS BY ORGANIZER
    // =========================

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<EventResponse>> getEventsByOrganizer(
            @PathVariable Long organizerId
    ) {

        List<EventResponse> responses = eventService
                .getEventsByOrganizer(organizerId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================
    // GET EVENT BY ID
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable Long id
    ) {

        return eventService
                .getEventById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // CREATE EVENT
    // =========================

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            Authentication authentication
    ) {

        Category category = categoryService
                .getCategoryById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category not found")
                );

        User organizer = getAuthenticatedUser(authentication);

        Event event = new Event();

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(category);
        event.setOrganizer(organizer);
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setImage(request.getImage());

        // Event baru selalu dimulai sebagai DRAFT.
        event.setStatus("DRAFT");

        Event savedEvent = eventService.saveEvent(event);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(savedEvent));
    }

    // =========================
    // UPDATE EVENT
    // =========================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            Authentication authentication
    ) {

        Event event = eventService
                .getEventById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        User authenticatedUser =
                getAuthenticatedUser(authentication);

        /*
         * Organizer can only update their own event.
         */
        if (!event.getOrganizer().getId()
                .equals(authenticatedUser.getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }

        Category category = categoryService
                .getCategoryById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category not found")
                );

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(category);
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setImage(request.getImage());

        Event updatedEvent = eventService.saveEvent(event);

        return ResponseEntity.ok(toResponse(updatedEvent));
    }

    // =========================
    // DELETE EVENT
    // =========================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Event event = eventService
                .getEventById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        String role = authentication
                .getAuthorities()
                .stream()
                .map(authority ->
                        authority.getAuthority()
                )
                .findFirst()
                .orElse("");

        /*
         * ADMIN can delete any event.
         *
         * ORGANIZER can only delete their own event.
         */
        if (role.equals("ROLE_ORGANIZER")) {

            User authenticatedUser =
                    getAuthenticatedUser(authentication);

            if (!event.getOrganizer().getId()
                    .equals(authenticatedUser.getId())) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .build();
            }
        }

        eventService.deleteEvent(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // GET AUTHENTICATED USER
    // =========================

    private User getAuthenticatedUser(
            Authentication authentication
    ) {

        return userService
                .getUserByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }

    // =========================
    // ENTITY → RESPONSE DTO
    // =========================

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