package com.example.TaskManagement.controller;

import com.example.TaskManagement.dto.request.LoginRequest;
import com.example.TaskManagement.dto.request.RegisterRequest;
import com.example.TaskManagement.dto.request.SendVerificationCodeRequest;
import com.example.TaskManagement.dto.request.VerifyRequest;
import com.example.TaskManagement.dto.response.ApiResponse;
import com.example.TaskManagement.dto.response.AuthResponse;
import com.example.TaskManagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng nhập, đăng ký Staff (xác minh email)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-verification-code")
    @Operation(summary = "Gửi lại mã xác minh kích hoạt", description = "Gửi lại mã 6 số tới email của tài khoản chưa được kích hoạt (hiệu lực 10 phút)")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", null));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký Staff (chưa kích hoạt)", description = "Đăng ký tài khoản Staff mới. Mã xác minh sẽ tự động được gửi qua email.")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff account registered successfully. Please check your email to activate your account.", null));
    }

    @PostMapping("/verify")
    @Operation(summary = "Xác nhận mã kích hoạt tài khoản", description = "Xác minh mã gửi về email để kích hoạt tài khoản Staff và cho phép đăng nhập.")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyRequest request) {
        authService.verifyAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully. You can now log in.", null));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Manager mặc định: username=manager, password=manager123")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
