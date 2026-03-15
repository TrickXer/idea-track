package com.ideatrack.main.controller;

import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.notification.*;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.service.NotificationService;
import com.ideatrack.main.service.ReactiveNotificationStreamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','EMPLOYEE','REVIEWER')")
public class NotificationController {

    private final NotificationService notificationService;

    private final ReactiveNotificationStreamService streamService;


    // --------------------------------------------------
    // Create
    // --------------------------------------------------

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> create(
            @Valid @RequestBody NotificationCreateRequest req) throws UserNotFoundException {

        NotificationResponseDTO created = notificationService.create(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponseDTO>> createBulk(
            @Valid @RequestBody NotificationBulkCreateRequest req) throws UserNotFoundException {

        var created = notificationService.createBulk(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // --------------------------------------------------
    // List (paged, with filters)
    // --------------------------------------------------

    /**
     * Example:
     * GET /api/notifications?userId=1013&status=UNREAD&pushed=ALL&page=0&size=20&sort=createdAt,desc&from=2026-01-01T00:00:00&to=2026-01-31T23:59:59
     *
     * @param userId required
     * @param status UNREAD | READ | ALL (default ALL if null)
     * @param pushed true | false | null (ALL when null)
     */
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponseDTO>> list(
            @RequestParam Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean pushed,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Pageable pageable = toPageable(page, size, sort);
        LocalDateTime fromTs = parseDateTimeNullable(from);
        LocalDateTime toTs = parseDateTimeNullable(to);

        var resp = notificationService.listByUser(userId, status, pushed, fromTs, toTs, pageable);
        return ResponseEntity.ok(resp);
    }

    // --------------------------------------------------
    // Mark read
    // --------------------------------------------------

    @PatchMapping("/read")
    public ResponseEntity<Void> markRead(@Valid @RequestBody MarkReadRequest req) {
        notificationService.markRead(req);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@Valid @RequestBody MarkAllReadRequest req) {
        notificationService.markAllRead(req);
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------
    // SSE stream
    // --------------------------------------------------

    /**
     * SSE endpoint: Subscribe to live notifications.
     * Example: GET /api/notifications/stream?userId=1013
     * Response Content-Type: text/event-stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@RequestParam Integer userId) {
        log.info("SSE subscribe request for userId={}", userId);
        SseEmitter emitter = notificationService.subscribe(userId);
        // Optional: explicit no-cache headers for intermediaries
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(emitter);
    }


    /**
     * Reactive SSE stream using WebFlux:
     * GET /api/notifications/stream/reactive?userId=1013
     */
    @GetMapping(value = "/stream/reactive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationSseDTO>> reactiveStream(@RequestParam Integer userId) {
        log.info("WebFlux SSE subscribe request for userId={}", userId);
        return streamService.subscribeFlux(userId);
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------

    private Pageable toPageable(int page, int size, String sortParam) {
        String[] parts = sortParam.split(",");
        String field = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }

    private LocalDateTime parseDateTimeNullable(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid datetime format. Use ISO-8601, e.g., 2026-01-28T00:00:00");
        }
    }
}