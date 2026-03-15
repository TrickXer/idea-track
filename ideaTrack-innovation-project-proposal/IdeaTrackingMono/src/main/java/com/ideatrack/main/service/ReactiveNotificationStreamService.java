package com.ideatrack.main.service;

import com.ideatrack.main.data.Notification;
import com.ideatrack.main.dto.notification.NotificationSseDTO;
import com.ideatrack.main.repository.INotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveNotificationStreamService {

    private final INotificationRepository notificationRepo;
    private final TransactionTemplate transactionTemplate;

    // Per-user live sinks
    private final Map<Integer, Sinks.Many<NotificationSseDTO>> userSinks = new ConcurrentHashMap<>();

    private Sinks.Many<NotificationSseDTO> sinkFor(Integer userId) {
        // multicast sink supports multiple subscribers (tabs) for a user
        return userSinks.computeIfAbsent(
                userId,
                k -> Sinks.many().multicast().onBackpressureBuffer()
        );
    }

    /** Emit a live notification into the user's stream (non-blocking best-effort). */
    public void emit(Integer userId, NotificationSseDTO dto) {
        try {
            sinkFor(userId).tryEmitNext(dto);
        } catch (Exception ex) {
            log.debug("Sink emit ignored for userId={} (no subscribers yet?): {}", userId, ex.getMessage());
        }
    }

    /**
     * Reactive SSE stream:
     * - backlog (recent UNREAD)
     * - unpushed (mark pushed after sending)
     * - live stream via sink
     * - heartbeat every 15s
     */
    public Flux<ServerSentEvent<NotificationSseDTO>> subscribeFlux(Integer userId) {
        // 1) Backlog: recent unread (blocking → boundedElastic)
        Flux<ServerSentEvent<NotificationSseDTO>> backlog =
                Mono.fromCallable(() ->
                        notificationRepo.findTop50ByUser_UserIdAndNotificationStatusAndDeletedFalseOrderByCreatedAtDesc(userId, "UNREAD")
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(this::toSseDto)
                .map(this::toSseEvent);

        // 2) Unpushed: send oldest-first and mark pushed (blocking → boundedElastic)
        Flux<ServerSentEvent<NotificationSseDTO>> unpushed =
                Mono.fromCallable(() ->
                        notificationRepo.findTop200ByUser_UserIdAndIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc(userId)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(n -> new AbstractMap.SimpleEntry<>(n, toSseDto(n)))
                .flatMap(entry ->
                        Mono.fromRunnable(() -> markSinglePushed(entry.getKey().getNotificationId()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(entry.getValue())
                )
                .map(this::toSseEvent);

        // 3) Live stream from sink (non-blocking)
        Flux<ServerSentEvent<NotificationSseDTO>> live =
                sinkFor(userId)
                    .asFlux()
                    .map(this::toSseEvent);

        // 4) Heartbeat
        Flux<ServerSentEvent<NotificationSseDTO>> heartbeat =
                Flux.interval(Duration.ofSeconds(15))
                    .map(t -> ServerSentEvent.<NotificationSseDTO>builder()
                            .event("heartbeat")
                            .data(null)
                            .build());

        // Emit backlog → unpushed → then keep live, while heartbeats in parallel
        return Flux.concat(backlog, unpushed)
                   .concatWith(live)
                   .mergeWith(heartbeat)
                   .doOnSubscribe(s -> log.info("WebFlux SSE connected for userId={}", userId))
                   .doFinally(signal -> log.info("WebFlux SSE disconnected for userId={}, reason={}", userId, signal));
    }

    private void markSinglePushed(Integer notificationId) {
        transactionTemplate.executeWithoutResult(status -> {
            // markPushed expects a collection; call with one id
            notificationRepo.markPushed(Collections.singletonList(notificationId));
        });
    }

    private NotificationSseDTO toSseDto(Notification n) {
        return NotificationSseDTO.builder()
                .notificationId(n.getNotificationId())
                .type(n.getNotificationType())
                .title(n.getNotificationTitle())
                .message(n.getNotificationMessage())
                .priority(n.getPriority())
                .metadata(n.getMetadata())
                .createdAtIso(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .build();
    }

    private ServerSentEvent<NotificationSseDTO> toSseEvent(NotificationSseDTO dto) {
        return ServerSentEvent.<NotificationSseDTO>builder(dto)
                .id(dto.getNotificationId() != null ? String.valueOf(dto.getNotificationId()) : null)
                .event("notification")
                .data(dto)
                .build();
    }
}