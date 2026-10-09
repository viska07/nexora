package com.nexora.backend.config;

import com.nexora.backend.entity.Role;
import com.nexora.backend.entity.User;
import com.nexora.backend.repository.RoleRepository;
import com.nexora.backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DevelopmentDataInitializer {

    @Bean
    CommandLineRunner initializeDevelopmentUsers(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            createOrUpdateUser(
                    userRepository,
                    roleRepository,
                    passwordEncoder,
                    "Nexora Organizer",
                    "organizer@nexora.local",
                    "NexoraOrganizer123",
                    "ORGANIZER"
            );

            createOrUpdateUser(
                    userRepository,
                    roleRepository,
                    passwordEncoder,
                    "Nexora Admin",
                    "admin@nexora.local",
                    "NexoraAdmin123",
                    "ADMIN"
            );

            createOrUpdateUser(
                    userRepository, 
                    roleRepository, 
                    passwordEncoder,
                    "Nexora Organizer 2",
                    "organizer2@nexora.local",
                    "NexoraOrganizer2",
                    "ORGANIZER"
            );
        };
    }

    private void createOrUpdateUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String fullName,
            String email,
            String plainPassword,
            String roleName
    ) {

        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Role not found: " + roleName
                        )
                );

        User user = userRepository
                .findByEmail(email)
                .orElseGet(User::new);

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(plainPassword)
        );
        user.setRole(role);

        LocalDateTime now = LocalDateTime.now();

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }

        user.setUpdatedAt(now);

        userRepository.save(user);
    }
}