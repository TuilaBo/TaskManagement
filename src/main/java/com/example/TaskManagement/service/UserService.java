package com.example.TaskManagement.service;

import com.example.TaskManagement.dto.request.ChangePasswordRequest;
import com.example.TaskManagement.dto.request.UpdateProfileRequest;
import com.example.TaskManagement.dto.response.UserResponse;
import com.example.TaskManagement.model.enums.Role;

import java.util.List;

public interface UserService {
    List<UserResponse> getUsers(Role role, Boolean active);
    UserResponse getCurrentUser(String username);
    UserResponse updateProfile(String username, UpdateProfileRequest request);
    void changePassword(String username, ChangePasswordRequest request);
}
