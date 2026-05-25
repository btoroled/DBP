package com.streakstudy.infrastructure.web.advice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.streakstudy.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.streakstudy.application.service.InstitutionService.InstitutionCodeAlreadyExistsException;

/**
 * Mapea excepciones de dominio y de framework a respuestas HTTP estandar.
 *
 * <p>Cada body incluye {@code timestamp}, {@code status}, {@code error},
 * {@code message} y {@code path} (URI de la request).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), req);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> emailExists(EmailAlreadyExistsException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "email_already_exists", ex.getMessage(), req);
    }

    @ExceptionHandler(InstitutionCodeAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> codeExists(InstitutionCodeAlreadyExistsException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "institution_code_already_exists", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> invalidCreds(InvalidCredentialsException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, "invalid_credentials", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> invalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, "invalid_refresh_token", ex.getMessage(), req);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> refreshExpired(RefreshTokenExpiredException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, "refresh_token_expired", ex.getMessage(), req);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<Map<String, Object>> refreshRevoked(RefreshTokenRevokedException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, "refresh_token_revoked", ex.getMessage(), req);
    }

    @ExceptionHandler(TenantViolationException.class)
    public ResponseEntity<Map<String, Object>> tenantViolation(TenantViolationException ex, HttpServletRequest req) {
        // Devolvemos 403 para que sea visible que es un problema de aislamiento
        return body(HttpStatus.FORBIDDEN, "tenant_violation", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
            .toList();
        Map<String, Object> resp = baseBody(HttpStatus.BAD_REQUEST, "validation_error", "Datos invalidos", req);
        resp.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> malformedJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "malformed_json", "JSON malformado o tipo invalido", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, "forbidden", "No tiene permiso para esta operacion", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientXpException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientXp(InsufficientXpException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "insufficient_xp", ex.getMessage(), req);
    }

    @ExceptionHandler(MaxStreakFreezesReachedException.class)
    public ResponseEntity<Map<String, Object>> handleMaxStreakFreezes(MaxStreakFreezesReachedException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "max_streak_freezes_reached", ex.getMessage(), req);
    }

    @ExceptionHandler(BadgeAlreadyOwnedException.class)
    public ResponseEntity<Map<String, Object>> handleBadgeAlreadyExists(BadgeAlreadyOwnedException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "badge_already_owned", ex.getMessage(), req); // 409 es ideal para duplicados
    }

    // Fallback intencionalmente NO declarado para no enmascarar bugs en dev.

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(baseBody(status, code, message, req));
    }

    private Map<String, Object> baseBody(HttpStatus status, String code, String message, HttpServletRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", Instant.now().toString());
        map.put("status", status.value());
        map.put("error", code);
        map.put("message", message);
        map.put("path", req.getRequestURI());
        return map;
    }
}
