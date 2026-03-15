package com.ideatrack.main.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================
    // 404 - Not Found (Your domain)
    // ============================
    @ExceptionHandler({
            IdeaNotFound.class,
            CategoryNotFound.class,
            DepartmentNotFound.class,
            UserNotFoundException.class,
            ResourceNotFoundException.class,
            ReportNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        log.warn("NOT_FOUND: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null);
    }

    // ============================
    // 409 - Conflicts / duplicates
    // ============================
    @ExceptionHandler({
            DuplicateCategoryException.class,
            DuplicateEmailException.class,
            AssignmentConflictException.class
    })
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex, HttpServletRequest req) {
        log.warn("CONFLICT: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI(), null);
    }

    // ============================
    // 400 - Validation / Bad input
    // ============================

    // @Valid on @RequestBody DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest req) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                		FieldError::getField,
                        err -> err.getDefaultMessage() == null ? "Invalid value" : err.getDefaultMessage(),
                        (a, b) -> a
                ));
        log.warn("VALIDATION_FAILED: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), errors);
    }

    // @Validated on @PathVariable / @RequestParam
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest req) {
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
        log.warn("CONSTRAINT_VIOLATION: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "Constraint violation", req.getRequestURI(), errors);
    }

    // Malformed JSON, invalid enum value, wrong types, etc.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest req) {
        String msg = rootMessage(ex);
        log.warn("MESSAGE_NOT_READABLE: {}", msg);
        return build(HttpStatus.BAD_REQUEST, "Malformed request: " + msg, req.getRequestURI(), null);
    }

    // ============================
    // 400 - Illegal state / arguments + Custom BadRequest
    // ============================
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            BadRequestException.class,
            WeakPasswordException.class,
            XPUpdateException.class,
            BadgeComputationException.class,
            ActivityProcessingException.class,
            DecisionResolutionException.class,
            ProfileOperationException.class,
            PasswordChangeException.class,
            FileStorageException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex, HttpServletRequest req) {
        log.warn("BAD_REQUEST: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI(), null);
    }

    // ============================
    // 401 - Authentication problems
    // ============================
    @ExceptionHandler({
            UnauthorizedException.class,
            AuthenticationException.class,
            BadCredentialsException.class,
            DisabledException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex, HttpServletRequest req) {
        log.warn("UNAUTHORIZED: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI(), null);
    }

    // ============================
    // 403 - Authorization problems
    // ============================
    @ExceptionHandler({
            ForbiddenOperationException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ApiError> handleForbidden(RuntimeException ex, HttpServletRequest req) {
        log.warn("FORBIDDEN: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req.getRequestURI(), null);
    }

    // ============================
    // 401 - JWT Exceptions
    // ============================
    @ExceptionHandler({
            InvalidJwtException.class,
            JwtException.class,
            SignatureException.class,
            MalformedJwtException.class,
            UnsupportedJwtException.class
    })
    public ResponseEntity<ApiError> handleInvalidJwt(RuntimeException ex, HttpServletRequest req) {
        log.warn("INVALID_JWT: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid token: " + ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler({TokenExpiredException.class, ExpiredJwtException.class})
    public ResponseEntity<ApiError> handleExpiredJwt(RuntimeException ex, HttpServletRequest req) {
        log.warn("EXPIRED_JWT: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Token expired", req.getRequestURI(), null);
    }

    // ============================
    // 400 - DB / Repository Issues
    // ============================
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex,
                                                            HttpServletRequest req) {
        String msg = mostSpecific(ex);
        log.warn("INVALID_DATA_ACCESS: {}", msg);
        return build(HttpStatus.BAD_REQUEST, msg, req.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                 HttpServletRequest req) {
        String msg = mostSpecific(ex);
        log.warn("DATA_INTEGRITY: {}", msg);
        return build(HttpStatus.BAD_REQUEST, "Data integrity violation: " + msg, req.getRequestURI(), null);
    }

    // ============================
    // 500 - Internal errors (Profile/Hierarchy)
    // ============================
    @ExceptionHandler({
            HierarchyBuildException.class,
            ProfileComputationException.class
    })
    public ResponseEntity<ApiError> handleInternal(RuntimeException ex, HttpServletRequest req) {
        log.error("INTERNAL_ERROR: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req.getRequestURI(), null);
    }

    // ============================
    // Async / client disconnect (SSE)
    // ============================
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("CLIENT_DISCONNECTED: {}", ex.getMessage());
        return ResponseEntity.noContent().build();
    }

    // ============================
    // 500 - Fallback
    // ============================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("UNHANDLED_EXCEPTION", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal error: " + ex.getMessage(),
                req.getRequestURI(),
                null);
    }

    // ============================
    // Helpers
    // ============================
    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String message,
                                           String path,
                                           Map<String, String> validationErrors) {

        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private static String mostSpecific(RuntimeException ex) {
        return ex.getCause() != null && ex.getCause().getMessage() != null
                ? ex.getCause().getMessage()
                : ex.getMessage();
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : ex.getMessage();
    }

/*
import com.ideatrack.main.dto.reviewer.ReviewerErrorResponse;


	 // ==========================================================
	 // Reviewer Module Exception Handler
	 // ==========================================================
	 @ExceptionHandler(com.ideatrack.main.exception.ReviewerException.class)
 public ResponseEntity<com.ideatrack.main.dto.reviewer.ReviewerErrorResponse> handleReviewerException(
         ReviewerException ex,
         jakarta.servlet.http.HttpServletRequest request) {

     ReviewerErrorResponse body =ReviewerErrorResponse.builder()
                     .timestamp(java.time.LocalDateTime.now())
                     .status(ex.getStatus().value())
                     .errorCode(ex.getErrorCode())
                     .message(ex.getMessage())
                     .path(request.getRequestURI())
                     .build();

     return ResponseEntity.status(ex.getStatus()).body(body);
 }*/
 }