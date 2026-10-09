package com.nexora.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleTestController {

    @GetMapping("/api/test-attendee")
    @PreAuthorize("hasRole('ATTENDEE')")
    public String testAttendeeRole() {
        return "ATTENDEE role verified";
    }

    @GetMapping("/api/test-organizer")
    @PreAuthorize("hasRole('ORGANIZER')")
    public String testOrganizerRole() {
        return "ORGANIZER role verified";
    }

    @GetMapping("/api/test-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String testAdminRole() {
        return "ADMIN role verified";
    }
}