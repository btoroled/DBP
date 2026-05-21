package com.streakstudy.infrastructure.web.advice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.streakstudy.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.streakstudy.application.service.InstitutionService.InstitutionCodeAlreadyExistsException;

/**
 * Mapea excepciones de dominio y de framework a respuestas HTTP estandar.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "not_found", ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> emailExists(EmailAlreadyExistsException ex) {
        return body(HttpStatus.CONFLICT, "email_already_exists", ex.getMessage());
    }

    @ExceptionHandler(InstitutionCodeAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> codeExists(InstitutionCodeAlreadyExistsException ex) {
        return body(HttpStatus.CONFLICT, "institution_code_already_exists", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> invalidCreds(InvalidCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, "invalid_credentials", ex.getMessage());
    }

    @ExceptionHandler(TenantViolationException.class)
    public ResponseEntity<Map<String, Object>> tenantViolation(TenantViolationException ex) {
        // Devolvemos 403 para que sea visible que es un problema de aislamiento
        return body(HttpStatus.FORBIDDEN, "tenant_violation", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
            .toList();
        Map<String, Object> resp = baseBody(HttpStatus.BAD_REQUEST, "validation_error", "Datos invalidos");
        resp.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArg(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage());
    }

    @ExceptionHandler(InsufficientXpException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientXp(InsufficientXpException ex) {
        return body(HttpStatus.BAD_REQUEST, "insufficient_xp", ex.getMessage());
    }

    @ExceptionHandler(MaxStreakFreezesReachedException.class)
    public ResponseEntity<Map<String, Object>> handleMaxStreakFreezes(MaxStreakFreezesReachedException ex) {
        return body(HttpStatus.BAD_REQUEST, "max_streak_freezes_reached", ex.getMessage());
    }

    @ExceptionHandler(BadgeAlreadyOwnedException.class)
    public ResponseEntity<Map<String, Object>> handleBadgeAlreadyExists(BadgeAlreadyOwnedException ex) {
        return body(HttpStatus.CONFLICT, "badge_already_owned", ex.getMessage()); // 409 es ideal para duplicados
    }

    // Fallback intencionalmente NO declarado para no enmascarar bugs en dev.

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(baseBody(status, code, message));
    }

    private Map<String, Object> baseBody(HttpStatus status, String code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", Instant.now().toString());
        map.put("status", status.value());
        map.put("error", code);
        map.put("message", message);
        return map;
    }
}
