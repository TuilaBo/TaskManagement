package com.example.TaskManagement.service.impl;

import com.example.TaskManagement.dto.request.LoginRequest;
import com.example.TaskManagement.dto.request.RegisterRequest;
import com.example.TaskManagement.dto.request.SendVerificationCodeRequest;
import com.example.TaskManagement.dto.request.VerifyRequest;
import com.example.TaskManagement.dto.response.AuthResponse;
import com.example.TaskManagement.exception.BadRequestException;
import com.example.TaskManagement.model.EmailVerification;
import com.example.TaskManagement.model.User;
import com.example.TaskManagement.model.enums.Role;
import com.example.TaskManagement.repository.EmailVerificationRepository;
import com.example.TaskManagement.repository.UserRepository;
import com.example.TaskManagement.security.CustomUserDetails;
import com.example.TaskManagement.security.JwtService;
import com.example.TaskManagement.service.AuthService;
import com.example.TaskManagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${verification.code.expiration-minutes}")
    private int verificationExpirationMinutes;

    @Override
    @Transactional
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email is not registered"));

        if (user.isActive()) {
            throw new BadRequestException("Account is already activated");
        }

        emailVerificationRepository.deleteByEmail(email);

        String code = generateVerificationCode();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationExpirationMinutes))
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationCodeEmail(email, code);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .role(Role.STAFF)
                .active(false)
                .build();

        userRepository.save(user);

        // Tự động gửi mã xác minh sau khi đăng ký
        emailVerificationRepository.deleteByEmail(email);
        String code = generateVerificationCode();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationExpirationMinutes))
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationCodeEmail(email, code);
    }

    @Override
    @Transactional
    public void verifyAccount(VerifyRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.isActive()) {
            throw new BadRequestException("Account is already activated");
        }

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndCode(email, request.getVerificationCode())
                .orElseThrow(() -> new BadRequestException("Invalid verification code"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verification);
            throw new BadRequestException("Verification code has expired. Please request a new code");
        }

        user.setActive(true);
        userRepository.save(user);
        emailVerificationRepository.delete(verification);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsernameOrEmailIgnoreCase(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
