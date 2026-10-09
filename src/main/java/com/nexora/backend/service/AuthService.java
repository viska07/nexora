package com.nexora.backend.service;

import com.nexora.backend.dto.AuthResponse;
import com.nexora.backend.dto.LoginRequest;
import com.nexora.backend.dto.RegisterRequest;
import com.nexora.backend.dto.UserResponse;
import com.nexora.backend.entity.Role;
import com.nexora.backend.entity.User;
import com.nexora.backend.repository.RoleRepository;
import com.nexora.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "Email is already registered"
            );
        }

        /*
         * Public registration always creates an ATTENDEE.
         *
         * Users must not be able to choose ADMIN or ORGANIZER
         * during public registration.
         */
        Role attendeeRole = roleRepository
                .findByName("ATTENDEE")
                .orElseThrow(() ->
                        new RuntimeException(
                                "ATTENDEE role not found"
                        )
                );

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(attendeeRole);

        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        return toUserResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        UserResponse userResponse = toUserResponse(user);

        return new AuthResponse(
                token,
                "Bearer",
                userResponse
        );
    }

    private UserResponse toUserResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }
}