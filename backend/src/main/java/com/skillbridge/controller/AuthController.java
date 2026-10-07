package com.skillbridge.controller;

import com.skillbridge.entity.User;
import com.skillbridge.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already registered"));
        }

        String role = request.role == null || request.role.isBlank()
                ? "STUDENT"
                : request.role.toUpperCase();

        // ADMIN accounts are created by the application seed, not through public registration.
        if (!role.equals("STUDENT") && !role.equals("TEACHER")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role must be STUDENT or TEACHER"));
        }

        User user = new User();
        user.setName(request.name);
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole(role);
        user.setPhone(request.phone);
        user.setAddress(request.address);

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSafeUser(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.email)
                .map(user -> {
                    if (!passwordEncoder.matches(request.password, user.getPassword())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Invalid email or password"));
                    }
                    return ResponseEntity.ok(toSafeUser(user));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid email or password")));
    }

    private Map<String, Object> toSafeUser(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        return response;
    }

    public static class RegisterRequest {
        @NotBlank public String name;
        @NotBlank @Email public String email;
        @NotBlank public String password;
        public String role;
        public String phone;
        public String address;
    }

    public static class LoginRequest {
        @NotBlank @Email public String email;
        @NotBlank public String password;
    }
}
