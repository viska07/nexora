package com.nexora.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGeneratorTest {

    @Test
    void generatePasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("ORGANIZER = " + encoder.encode("NexoraOrganizer2026!"));
        System.out.println("ATTENDEE  = " + encoder.encode("NexoraAttendee2026!"));
        System.out.println("ADMIN     = " + encoder.encode("NexoraAdmin2026!"));
    }
} 