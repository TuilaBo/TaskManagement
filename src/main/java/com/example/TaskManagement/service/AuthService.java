package com.example.TaskManagement.service;

import com.example.TaskManagement.dto.request.LoginRequest;
import com.example.TaskManagement.dto.request.RegisterRequest;
import com.example.TaskManagement.dto.request.SendVerificationCodeRequest;
import com.example.TaskManagement.dto.response.AuthResponse;

import com.example.TaskManagement.dto.request.VerifyRequest;

public interface AuthService {

    void sendVerificationCode(SendVerificationCodeRequest request);

    void register(RegisterRequest request);

    void verifyAccount(VerifyRequest request);

    AuthResponse login(LoginRequest request);
}
