package com.example.socio.service;

import com.example.socio.entity.User;
import com.example.socio.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User createAdmin(User admin) {
        validateAdminEmail(admin.getEmail());
        admin.setRole(User.Role.ADMIN);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setPasswordLastUpdated(new Date());
        return userRepository.save(admin);
    }

    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());

        if (user.getRole() == User.Role.ADMIN) {
            validateAdminEmail(user.getEmail());
        }

        return userRepository.save(user);
    }

    public User togglePrivacy(UUID userId, boolean isPrivate) {
        User user = getUserById(userId);
        user.setPrivate(isPrivate);
        return userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> importUsersFromFile(MultipartFile file) throws IOException {
        List<User> users = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                String email = getCellValueAsString(row.getCell(1));
                if (userRepository.findByEmail(email).isPresent()) {
                    continue; // Skip existing users
                }

                User user = User.builder()
                        .name(getCellValueAsString(row.getCell(0)))
                        .email(email)
                        .password(passwordEncoder.encode("defaultPassword"))
                        .role(User.Role.USER)
                        .isPrivate(false)
                        .passwordLastUpdated(new Date())
                        .build();
                users.add(user);
            }
            return userRepository.saveAll(users);
        }
    }

    private void validateAdminEmail(String email) {
        if (!email.endsWith("@socio.com")) {
            throw new RuntimeException("Admin email must end with @socio.com");
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }
}