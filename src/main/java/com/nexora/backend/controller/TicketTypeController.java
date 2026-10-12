
package com.nexora.backend.controller;

import com.nexora.backend.entity.TicketType;
import com.nexora.backend.service.TicketTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticket-types")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping
    public ResponseEntity<List<TicketType>> getAllTicketTypes() {
        return ResponseEntity.ok(
                ticketTypeService.getAllTicketTypes()
        );
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketType>> getTicketTypesByEvent(
            @PathVariable Long eventId) {

        return ResponseEntity.ok(
                ticketTypeService.getTicketTypesByEvent(eventId)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> createTicketType(
            @RequestBody TicketType ticketType) {

        if (ticketType.getEvent() == null
                || ticketType.getEvent().getId() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Event ID wajib diisi.")
            );
        }

        if (ticketType.getName() == null
                || ticketType.getName().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Nama tiket wajib diisi.")
            );
        }

        if (ticketType.getPrice() == null
                || ticketType.getPrice().signum() < 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Harga tiket tidak boleh negatif.")
            );
        }

        if (ticketType.getQuota() == null
                || ticketType.getQuota() <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Kuota tiket harus lebih dari nol.")
            );
        }

        ticketType.setAvailableQuantity(ticketType.getQuota());

        TicketType savedTicket =
                ticketTypeService.saveTicketType(ticketType);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedTicket);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<?> updateTicketType(
            @PathVariable Long id,
            @RequestBody TicketType request) {

        return ticketTypeService.getTicketTypeById(id)
                .map(existing -> {
                    if (request.getName() == null
                            || request.getName().isBlank()
                            || request.getPrice() == null
                            || request.getPrice().signum() < 0
                            || request.getQuota() == null
                            || request.getQuota() <= 0) {
                        return ResponseEntity.badRequest().body(
                                Map.of("message",
                                        "Data tiket tidak valid.")
                        );
                    }

                    existing.setName(request.getName());
                    existing.setPrice(request.getPrice());

                    int sold = existing.getQuota()
                            - existing.getAvailableQuantity();

                    if (request.getQuota() < sold) {
                        return ResponseEntity.badRequest().body(
                                Map.of("message",
                                        "Kuota tidak boleh lebih kecil dari tiket yang sudah terjual.")
                        );
                    }

                    existing.setQuota(request.getQuota());
                    existing.setAvailableQuantity(
                            request.getQuota() - sold
                    );

                    return ResponseEntity.ok(
                            ticketTypeService.saveTicketType(existing)
                    );
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicketType(
            @PathVariable Long id) {

        if (ticketTypeService.getTicketTypeById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ticketTypeService.deleteTicketType(id);
        return ResponseEntity.noContent().build();
    }
}