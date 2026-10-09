package com.nexora.backend.repository;

import com.nexora.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(String status);

    List<Event> findByCategoryId(Long categoryId);

    List<Event> findByOrganizerId(Long organizerId);

    List<Event> findByTitleContainingIgnoreCase(String title);
}