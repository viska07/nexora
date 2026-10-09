package com.nexora.backend.service;

import com.nexora.backend.entity.TicketType;
import com.nexora.backend.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;

    public TicketTypeService(TicketTypeRepository ticketTypeRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public List<TicketType> getAllTicketTypes() {
        return ticketTypeRepository.findAll();
    }

    public Optional<TicketType> getTicketTypeById(Long id) {
        return ticketTypeRepository.findById(id);
    }

    public List<TicketType> getTicketTypesByEvent(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId);
    }

    public TicketType saveTicketType(TicketType ticketType) {
        return ticketTypeRepository.save(ticketType);
    }

    public void deleteTicketType(Long id) {
        ticketTypeRepository.deleteById(id);
    }
}