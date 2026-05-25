package com.streakstudy.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.dto.AuthResponse;
import com.streakstudy.application.dto.ForgotPasswordRequest;
import com.streakstudy.application.dto.LoginRequest;
import com.streakstudy.application.dto.RefreshTokenRequest;
import com.streakstudy.application.dto.RegisterRequest;
import com.streakstudy.application.dto.ResetPasswordRequest;
import com.streakstudy.application.service.AuthService;
import com.streakstudy.application.service.PasswordResetService;
import com.streakstudy.infrastructure.ratelimit.PasswordResetRateLimiter;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;

import jakarta.validation.Valid;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetRateLimiter passwordResetRateLimiter;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          PasswordResetRateLimiter passwordResetRateLimiter) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.passwordResetRateLimiter = passwordResetRateLimiter;
    }

    @Operation(summary = "Registrar usuario")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse resp = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Operation(summary = "Iniciar sesión")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Renovar access token")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return authService.refresh(req);
    }

    @Operation(summary = "Cerrar sesión")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                       @Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(principal.userId(), req);
        return ResponseEntity.noContent().build();
    }

    /**
     * Solicita un email para restablecer la contrasena. Responde 202 SIEMPRE
     * (existe o no el email) para evitar user enumeration.
     */
    @Operation(summary = "Solicitar recuperación de contraseña")
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        if (!passwordResetRateLimiter.tryAcquire(req.email())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        passwordResetService.requestReset(req.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Restablecer contraseña")
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.confirmReset(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
