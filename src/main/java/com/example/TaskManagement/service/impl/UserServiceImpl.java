package com.example.TaskManagement.service.impl;

import com.example.TaskManagement.dto.request.ChangePasswordRequest;
import com.example.TaskManagement.dto.request.UpdateProfileRequest;
import com.example.TaskManagement.dto.response.UserResponse;
import com.example.TaskManagement.exception.BadRequestException;
import com.example.TaskManagement.model.User;
import com.example.TaskManagement.model.enums.Role;
import com.example.TaskManagement.repository.UserRepository;
import com.example.TaskManagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(Role role, Boolean active) {
        List<User> users = userRepository.findUsersByFilters(role, active);
        return users.stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsernameOrEmailIgnoreCase(username)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsernameOrEmailIgnoreCase(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new BadRequestException("Email already in use");
                }
                user.setEmail(newEmail);
            }
        }

        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsernameOrEmailIgnoreCase(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
