package org.store.security.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.LoginRequest;
import org.store.security.application.dto.RefreshTokenRequest;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.dto.ForgotPasswordRequest;
import org.store.security.application.dto.ResetPasswordConfirmRequest;
import org.store.security.application.service.ILoginService;
import org.store.security.application.service.IPasswordResetService;
import org.store.security.application.service.IRefreshTokenService;
import org.store.security.application.service.IRegisterPropertyService;

@RestController
@RequestMapping(AuthController.BASE_PATH)
public class AuthController {

    public static final String BASE_PATH = "/api/v1/auth";

    private final IRegisterPropertyService registerPropertyService;
    private final ILoginService loginService;
    private final IRefreshTokenService refreshTokenService;
    private final IPasswordResetService passwordResetService;

    public AuthController(IRegisterPropertyService registerPropertyService,
                          ILoginService loginService,
                          IRefreshTokenService refreshTokenService,
                          IPasswordResetService passwordResetService) {
        this.registerPropertyService = registerPropertyService;
        this.loginService = loginService;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterPropertyRequest request) {
        AuthResponse response = registerPropertyService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordConfirmRequest request) {
        passwordResetService.confirmReset(request);
        return ResponseEntity.noContent().build();
    }
}
