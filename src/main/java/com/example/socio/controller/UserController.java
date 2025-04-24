package com.example.socio.controller;

import com.example.socio.entity.User;
import com.example.socio.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createAdmin(@Valid @RequestBody User admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createAdmin(admin));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot create admin user through this endpoint");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.updateUser(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable UUID id,
                                           @Valid @RequestBody User user) {
        User existingUser = userService.getUserById(id);

        // Prevent role changes through this endpoint
        user.setRole(existingUser.getRole());

        // Do not allow password updates through this endpoint
        user.setPassword(existingUser.getPassword());
        user.setId(id);

        return ResponseEntity.ok(userService.updateUser(user));
    }

    @PutMapping("/{id}/privacy")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<User> togglePrivacy(@PathVariable UUID id,
                                              @RequestParam boolean isPrivate) {
        return ResponseEntity.ok(userService.togglePrivacy(id, isPrivate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> importUsers(@RequestParam("file") MultipartFile file) {
        try {
            List<User> importedUsers = userService.importUsersFromFile(file);
            return ResponseEntity.ok(importedUsers);
        } catch (IOException e) {
            throw new RuntimeException("Failed to import users: " + e.getMessage());
        }
    }
}