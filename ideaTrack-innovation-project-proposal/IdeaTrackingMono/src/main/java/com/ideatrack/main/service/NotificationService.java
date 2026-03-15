package com.ideatrack.main.service;

import com.ideatrack.main.data.Notification;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.notification.*;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.INotificationRepository;
import com.ideatrack.main.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String UNREAD_STATUS = "UNREAD";

    private final INotificationRepository notificationRepo;
    private final IUserRepository userRepo;
    private final TransactionTemplate transactionTemplate; // ✅ used to wrap DB writes from SSE/scheduler threads

    // ---------------------------
    // SSE Emitter Registry
    // ---------------------------
    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final ReactiveNotificationStreamService reactiveStreamService;
    // 30 minutes
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    // ---------------------------
    // Create (single & bulk)
    // ---------------------------

    @Transactional
    public NotificationResponseDTO create(NotificationCreateRequest req) throws UserNotFoundException {
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + req.getUserId()));

        Notification n = Notification.builder()
                .user(user)
                .notificationType(req.getNotificationType())
                .notificationTitle(req.getNotificationTitle())
                .notificationMessage(req.getNotificationMessage())
                .priority(req.getPriority())
                .notificationStatus(UNREAD_STATUS)
                .isPushed(false)
                .metadata(req.getMetadata())
                .deleted(false)
                .build();

        Notification saved = notificationRepo.save(n);

        // Try immediate push if user is connected (no transaction required here)
        tryPushToConnected(saved);
        reactiveStreamService.emit(saved.getUser().getUserId(), toSse(saved));

        return toResponse(saved);
    }

    @Transactional
    public List<NotificationResponseDTO> createBulk(NotificationBulkCreateRequest req) throws UserNotFoundException {
        List<Notification> toSave = new ArrayList<>(req.getItems().size());

        for (NotificationCreateRequest item : req.getItems()) {
            User user = userRepo.findById(item.getUserId())
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + item.getUserId()));

            Notification n = Notification.builder()
                    .user(user)
                    .notificationType(item.getNotificationType())
                    .notificationTitle(item.getNotificationTitle())
                    .notificationMessage(item.getNotificationMessage())
                    .priority(item.getPriority())
                    .notificationStatus(UNREAD_STATUS)
                    .isPushed(false)
                    .metadata(item.getMetadata())
                    .deleted(false)
                    .build();

            toSave.add(n);
        }

        List<Notification> saved = notificationRepo.saveAll(toSave);

        // Try immediate push to connected users (batch by user)
        Map<Integer, List<Notification>> byUser = saved.stream()
                .collect(Collectors.groupingBy(n -> n.getUser().getUserId()));

        for (Map.Entry<Integer, List<Notification>> e : byUser.entrySet()) {
            pushBatchToConnected(e.getKey(), e.getValue());
            

            	Integer userId = e.getKey();
            	for (Notification n : e.getValue()) {
            		reactiveStreamService.emit(userId, toSse(n));
            	}

        }

        return saved.stream().map(this::toResponse).toList();
    }

    // ---------------------------
    // List / Search (paged)
    // ---------------------------

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponseDTO> listByUser(
            Integer userId,
            String status,          // UNREAD | READ | null (ALL)
            Boolean pushed,         // true | false | null (ALL)
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Page<Notification> page = notificationRepo.searchForUser(userId,
                normalizeAll(status),
                pushed,
                from,
                to,
                pageable);

        Page<NotificationResponseDTO> mapped = page.map(this::toResponse);
        return buildPagedResponse(mapped);
    }

    private String normalizeAll(String status) {
        if (status == null) return null;
        String s = status.trim();
        if (s.equalsIgnoreCase("ALL")) return null;
        return s.toUpperCase();
    }

    private <T> PagedResponse<T> buildPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ---------------------------
    // Mark read (bulk & all)
    // ---------------------------

    @Transactional
    public void markRead(MarkReadRequest req) {
        if (req.getNotificationIds() == null || req.getNotificationIds().isEmpty()) {
            throw new IllegalArgumentException("notificationIds must not be empty");
        }
        int updated = notificationRepo.markRead(req.getUserId(), req.getNotificationIds());
        log.info("Marked READ {} notifications for userId={}", updated, req.getUserId());
    }

    @Transactional
    public void markAllRead(MarkAllReadRequest req) {
        int updated = notificationRepo.markAllReadByUser(req.getUserId());
        log.info("Marked READ (all) {} notifications for userId={}", updated, req.getUserId());
    }

    // ---------------------------
    // SSE Subscribe
    // ---------------------------

    /**
     * Register an SSE connection for the given user.
     * Sends recent unread backlog and any unpushed notifications immediately (best effort).
     */
    // No transaction here; we are streaming
    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> {
            log.warn("SSE error for userId={}: {}", userId, ex.getMessage());
            removeEmitter(userId, emitter);
        });

        // Send a connected event immediately
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected")
                    .id("init-" + System.currentTimeMillis()));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE connected event for userId={}", userId, e);
        }

        // Send recent unread backlog (up to 50)
        List<Notification> recentUnread = notificationRepo
                .findTop50ByUser_UserIdAndNotificationStatusAndDeletedFalseOrderByCreatedAtDesc(userId, UNREAD_STATUS);

        // Send unpushed for the user (oldest first)
        List<Notification> unpushed = notificationRepo
                .findTop200ByUser_UserIdAndIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc(userId);

        // Merge by id to avoid duplicates
        Map<Integer, Notification> toSend = new LinkedHashMap<>();
        for (Notification n : recentUnread) toSend.put(n.getNotificationId(), n);
        for (Notification n : unpushed) toSend.put(n.getNotificationId(), n);

        // Now send and mark pushed if delivery succeeded
        if (!toSend.isEmpty()) {
            deliverAndMarkPushed(userId, new ArrayList<>(toSend.values()));
        }

        return emitter;
    }

    private void removeEmitter(Integer userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    // ---------------------------
    // Scheduled Push Loop (unpushed scan)
    // ---------------------------

    /**
     * Scan globally for unpushed notifications and try to deliver them
     * to currently connected users. Runs every 3 seconds.
     */
    @Scheduled(fixedDelay = 3000)
    public void scanAndPushUnpushed() {
        // ✅ Skip DB scan when nobody is connected (saves IO & noise)
        if (userEmitters.isEmpty()) {
            return;
        }

        List<Notification> pending = notificationRepo
                .findTop200ByIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) return;

        // Group by userId for efficient emitter selection
        Map<Integer, List<Notification>> byUser = pending.stream()
                .collect(Collectors.groupingBy(n -> n.getUser().getUserId()));

        byUser.forEach(this::deliverAndMarkPushed);
    }

    // ---------------------------
    // Internal Delivery Helpers
    // ---------------------------

    private void tryPushToConnected(Notification n) {
        Integer userId = n.getUser().getUserId();
        if (!userEmitters.containsKey(userId)) return;
        deliverAndMarkPushed(userId, List.of(n));
    }

    private void pushBatchToConnected(Integer userId, List<Notification> list) {
        if (list == null || list.isEmpty()) return;
        if (!userEmitters.containsKey(userId)) return;
        deliverAndMarkPushed(userId, list);
    }

    /**
     * Deliver the given notifications to all active emitters of the user.
     * If at least one emitter receives an event for a notification, we mark it as pushed.
     * The markPushed DB write is wrapped in a short transaction via TransactionTemplate.
     */
    private void deliverAndMarkPushed(Integer userId, List<Notification> notifications) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        List<Integer> deliveredIds = new ArrayList<>();

        for (Notification n : notifications) {
            NotificationSseDTO dto = toSse(n);
            boolean delivered = false;

            // ✅ Iterate over a snapshot (avoid iterator.remove on CopyOnWriteArrayList)
            for (SseEmitter emitter : new ArrayList<>(emitters)) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(String.valueOf(n.getNotificationId()))
                            .data(dto));
                    delivered = true; // at least one emitter succeeded
                } catch (IOException | IllegalStateException ex) {
                    // connection closed / emitter completed — clean up quietly
                    log.warn("SSE send failed for userId={} notifId={}: {}", userId, n.getNotificationId(), ex.getMessage());
                    removeEmitter(userId, emitter);
                }
            }

            if (delivered && !n.isPushed()) {
                deliveredIds.add(n.getNotificationId());
            }
        }

        if (!deliveredIds.isEmpty()) {
            // ✅ ensure DB write happens inside a transaction even from SSE or @Scheduled thread
            transactionTemplate.executeWithoutResult(status -> {
                int count = notificationRepo.markPushed(deliveredIds);
                log.info("Marked pushed={} for userId={} (notifIds={})", count, userId, deliveredIds);
            });
        }
    }

    // Optional heartbeat to keep connections alive (every 15s)
    @Scheduled(fixedDelay = 15000)
    public void sendHeartbeats() {
        userEmitters.forEach((userId, list) -> {
            // ✅ snapshot copy; remove via helper (no iterator.remove)
            for (SseEmitter emitter : new ArrayList<>(list)) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (IOException | IllegalStateException e) {
                    log.debug("Heartbeat failed for userId={}: {}", userId, e.getMessage());
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    // ---------------------------
    // Mapping Helpers
    // ---------------------------

    private NotificationResponseDTO toResponse(Notification n) {
        return NotificationResponseDTO.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUser() != null ? n.getUser().getUserId() : null)
                .notificationType(n.getNotificationType())
                .notificationTitle(n.getNotificationTitle())
                .notificationMessage(n.getNotificationMessage())
                .priority(n.getPriority())
                .notificationStatus(n.getNotificationStatus())
                .pushed(n.isPushed())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }

    private NotificationSseDTO toSse(Notification n) {
        return NotificationSseDTO.builder()
                .notificationId(n.getNotificationId())
                .type(n.getNotificationType())
                .title(n.getNotificationTitle())
                .message(n.getNotificationMessage())
                .priority(n.getPriority())
                .metadata(n.getMetadata())
                .createdAtIso(formatIso(n.getCreatedAt()))
                .build();
    }

    private String formatIso(LocalDateTime t) {
        if (t == null) return null;
        return t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
