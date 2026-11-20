package com.nexora.backend.controller;

import com.nexora.backend.dto.TicketTypeRequest;
import com.nexora.backend.dto.TicketTypeResponse;
import com.nexora.backend.entity.Event;
import com.nexora.backend.entity.TicketType;
import com.nexora.backend.entity.User;
import com.nexora.backend.service.EventService;
import com.nexora.backend.service.TicketTypeService;
import com.nexora.backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticket-types")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;
    private final EventService eventService;
    private final UserService userService;

    public TicketTypeController(
            TicketTypeService ticketTypeService,
            EventService eventService,
            UserService userService
    ) {
        this.ticketTypeService = ticketTypeService;
        this.eventService = eventService;
        this.userService = userService;
    }

    // =========================
    // GET ALL TICKET TYPES
    // =========================

    @GetMapping
    public ResponseEntity<List<TicketTypeResponse>> getAllTicketTypes() {

        List<TicketTypeResponse> response =
                ticketTypeService.getAllTicketTypes()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // GET TICKET TYPES BY EVENT
    // =========================

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketTypeResponse>> getTicketTypesByEvent(
            @PathVariable Long eventId
    ) {

        List<TicketTypeResponse> response =
                ticketTypeService.getTicketTypesByEvent(eventId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // CREATE TICKET TYPE
    // =========================

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> createTicketType(
            @Valid @RequestBody TicketTypeRequest request,
            Authentication authentication
    ) {

        Event event = eventService
                .getEventById(request.getEventId())
                .orElse(null);

        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Event not found"
                    ));
        }

        User authenticatedUser = getAuthenticatedUser(authentication);

        String role = getRole(authentication);

        /*
         * ADMIN dapat membuat ticket type
         * untuk event apa pun.
         *
         * ORGANIZER hanya dapat membuat
         * ticket type untuk event miliknya sendiri.
         */
        if (role.equals("ROLE_ORGANIZER")
                && !event.getOrganizer().getId()
                .equals(authenticatedUser.getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can only manage ticket types for your own events"
                    ));
        }

        LocalDateTime now = LocalDateTime.now();

        TicketType ticketType = new TicketType();

        ticketType.setEvent(event);
        ticketType.setName(request.getName());
        ticketType.setDescription(request.getDescription());
        ticketType.setPrice(request.getPrice());
        ticketType.setQuota(request.getQuota());

        /*
         * Saat ticket type pertama kali dibuat,
         * seluruh quota masih tersedia.
         */
        ticketType.setAvailableQuantity(request.getQuota());

        ticketType.setCreatedAt(now);
        ticketType.setUpdatedAt(now);

        TicketType savedTicket =
                ticketTypeService.saveTicketType(ticketType);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(savedTicket));
    }

    // =========================
    // UPDATE TICKET TYPE
    // =========================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> updateTicketType(
            @PathVariable Long id,
            @Valid @RequestBody TicketTypeRequest request,
            Authentication authentication
    ) {

        TicketType existingTicket =
                ticketTypeService
                        .getTicketTypeById(id)
                        .orElse(null);

        if (existingTicket == null) {
            return ResponseEntity.notFound().build();
        }

        Event event = existingTicket.getEvent();

        User authenticatedUser = getAuthenticatedUser(authentication);

        String role = getRole(authentication);

        /*
         * ADMIN dapat mengubah ticket type apa pun.
         *
         * ORGANIZER hanya dapat mengubah
         * ticket type dari event miliknya sendiri.
         */
        if (role.equals("ROLE_ORGANIZER")
                && !event.getOrganizer().getId()
                .equals(authenticatedUser.getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can only manage ticket types for your own events"
                    ));
        }

        /*
         * Event ticket type tidak boleh dipindahkan
         * ke event lain melalui update.
         *
         * eventId dari request harus sama dengan
         * event yang sudah dimiliki ticket type.
         */
        if (!event.getId().equals(request.getEventId())) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Ticket type cannot be moved to another event"
                    ));
        }

        /*
         * Hitung jumlah tiket yang sudah terjual.
         */
        int sold = existingTicket.getQuota()
                - existingTicket.getAvailableQuantity();

        /*
         * Quota baru tidak boleh lebih kecil
         * dari jumlah tiket yang sudah terjual.
         */
        if (request.getQuota() < sold) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Quota cannot be smaller than tickets already sold"
                    ));
        }

        existingTicket.setName(request.getName());
        existingTicket.setDescription(request.getDescription());
        existingTicket.setPrice(request.getPrice());
        existingTicket.setQuota(request.getQuota());

        /*
         * Stok tersedia dihitung ulang berdasarkan
         * quota baru dikurangi tiket yang sudah terjual.
         */
        existingTicket.setAvailableQuantity(
                request.getQuota() - sold
        );

        existingTicket.setUpdatedAt(LocalDateTime.now());

        TicketType updatedTicket =
                ticketTypeService.saveTicketType(existingTicket);

        return ResponseEntity.ok(toResponse(updatedTicket));
    }

    // =========================
    // DELETE TICKET TYPE
    // =========================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> deleteTicketType(
            @PathVariable Long id,
            Authentication authentication
    ) {

        TicketType ticketType =
                ticketTypeService
                        .getTicketTypeById(id)
                        .orElse(null);

        if (ticketType == null) {
            return ResponseEntity.notFound().build();
        }

        Event event = ticketType.getEvent();

        User authenticatedUser = getAuthenticatedUser(authentication);

        String role = getRole(authentication);

        /*
         * ADMIN dapat menghapus ticket type apa pun.
         *
         * ORGANIZER hanya dapat menghapus
         * ticket type dari event miliknya sendiri.
         */
        if (role.equals("ROLE_ORGANIZER")
                && !event.getOrganizer().getId()
                .equals(authenticatedUser.getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can only manage ticket types for your own events"
                    ));
        }

        ticketTypeService.deleteTicketType(id);

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
    // GET ROLE
    // =========================

    private String getRole(
            Authentication authentication
    ) {

        return authentication
                .getAuthorities()
                .stream()
                .map(authority ->
                        authority.getAuthority()
                )
                .findFirst()
                .orElse("");
    }

    // =========================
    // ENTITY → RESPONSE DTO
    // =========================

    private TicketTypeResponse toResponse(
            TicketType ticket
    ) {

        return new TicketTypeResponse(
                ticket.getId(),
                ticket.getEvent().getId(),
                ticket.getName(),
                ticket.getDescription(),
                ticket.getPrice(),
                ticket.getQuota(),
                ticket.getAvailableQuantity(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}