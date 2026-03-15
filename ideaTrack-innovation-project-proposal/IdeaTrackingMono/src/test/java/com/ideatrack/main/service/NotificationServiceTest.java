package com.ideatrack.main.service;

import com.ideatrack.main.data.Notification;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.notification.*;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.INotificationRepository;
import com.ideatrack.main.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private INotificationRepository notificationRepo;
    @Mock private IUserRepository userRepo;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private ReactiveNotificationStreamService reactiveStreamService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void before() {
        // Make this stubbing lenient so tests that don't exercise TransactionTemplate won't fail with UnnecessaryStubbingException.
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Consumer<TransactionStatus> consumer = (Consumer<TransactionStatus>) inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    // --------------------------------------------------
    // create()
    // --------------------------------------------------

    @Test
    @DisplayName("create(): success -> saves, emits reactive, returns mapped")
    void create_success() throws Exception {
        User user = User.builder().userId(101).name("U").build();
        when(userRepo.findById(101)).thenReturn(Optional.of(user));

        Notification saved = Notification.builder()
                .notificationId(1)
                .user(user)
                .notificationType("SYSTEM")
                .notificationTitle("Title")
                .notificationMessage("Msg")
                .priority("LOW")
                .notificationStatus("UNREAD")
                .isPushed(false)
                .metadata("{}")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(notificationRepo.save(any(Notification.class))).thenReturn(saved);

        NotificationCreateRequest req = NotificationCreateRequest.builder()
                .userId(101).notificationType("SYSTEM").notificationTitle("Title")
                .notificationMessage("Msg").priority("LOW").metadata("{}").build();

        NotificationResponseDTO out = notificationService.create(req);

        assertNotNull(out);
        assertEquals(1, out.getNotificationId());
        assertEquals(101, out.getUserId());
        verify(notificationRepo).save(any(Notification.class));
        verify(reactiveStreamService).emit(eq(101), any(NotificationSseDTO.class));
    }

    @Test
    @DisplayName("create(): UserNotFound")
    void create_userNotFound() {
        when(userRepo.findById(404)).thenReturn(Optional.empty());
        NotificationCreateRequest req = NotificationCreateRequest.builder()
                .userId(404).notificationType("X").notificationTitle("Y").notificationMessage("Z").priority("HIGH").build();
        assertThrows(UserNotFoundException.class, () -> notificationService.create(req));
        verify(notificationRepo, never()).save(any());
    }

    // --------------------------------------------------
    // createBulk()
    // --------------------------------------------------

    @Test
    @DisplayName("createBulk(): success -> saves list, emits per item")
    void createBulk_success() throws Exception {
        User u1 = User.builder().userId(1).name("U1").build();
        User u2 = User.builder().userId(2).name("U2").build();

        when(userRepo.findById(1)).thenReturn(Optional.of(u1));
        when(userRepo.findById(2)).thenReturn(Optional.of(u2));

        Notification n1 = Notification.builder()
                .notificationId(11).user(u1).notificationType("COMMENT")
                .notificationTitle("T1").notificationMessage("M1").priority("MEDIUM")
                .notificationStatus("UNREAD").isPushed(false).deleted(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        Notification n2 = Notification.builder()
                .notificationId(22).user(u2).notificationType("SYSTEM")
                .notificationTitle("T2").notificationMessage("M2").priority("LOW")
                .notificationStatus("UNREAD").isPushed(false).deleted(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(notificationRepo.saveAll(anyList())).thenReturn(List.of(n1, n2));

        NotificationBulkCreateRequest req = NotificationBulkCreateRequest.builder()
                .items(List.of(
                        NotificationCreateRequest.builder().userId(1).notificationType("COMMENT").notificationTitle("T1").notificationMessage("M1").priority("MEDIUM").build(),
                        NotificationCreateRequest.builder().userId(2).notificationType("SYSTEM").notificationTitle("T2").notificationMessage("M2").priority("LOW").build()
                ))
                .build();

        List<NotificationResponseDTO> out = notificationService.createBulk(req);

        assertThat(out).hasSize(2);
        verify(notificationRepo).saveAll(anyList());
        // Two emits (one per saved item)
        verify(reactiveStreamService, times(2)).emit(anyInt(), any(NotificationSseDTO.class));
    }

    // --------------------------------------------------
    // listByUser()
    // --------------------------------------------------

    @Test
    @DisplayName("listByUser(): maps page & normalizes status 'ALL' -> null in repo call")
    void listByUser_maps_normalizes() {
        Notification a = Notification.builder().notificationId(1).notificationTitle("A").notificationStatus("UNREAD").deleted(false).build();
        Notification b = Notification.builder().notificationId(2).notificationTitle("B").notificationStatus("READ").deleted(false).build();
        Page<Notification> page = new PageImpl<>(List.of(a, b), PageRequest.of(0, 10), 2);

        when(notificationRepo.searchForUser(anyInt(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<NotificationResponseDTO> resp =
                notificationService.listByUser(101, "ALL", null, null, null, pageable);

        assertNotNull(resp);
        assertThat(resp.getContent()).hasSize(2);

        // Capture status argument to verify it was normalized to null
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationRepo).searchForUser(eq(101), statusCaptor.capture(), isNull(), isNull(), isNull(), eq(pageable));
        assertNull(statusCaptor.getValue(), "Expected status 'ALL' to be normalized to null");
    }

    // --------------------------------------------------
    // markRead / markAllRead
    // --------------------------------------------------

    @Test
    @DisplayName("markRead(): passes params to repo bulk update")
    void markRead_success() {
        when(notificationRepo.markRead(eq(101), eq(List.of(1, 2, 3)))).thenReturn(3);

        MarkReadRequest req = MarkReadRequest.builder()
                .userId(101)
                .notificationIds(List.of(1, 2, 3))
                .build();

        notificationService.markRead(req);

        verify(notificationRepo).markRead(101, List.of(1, 2, 3));
    }

    @Test
    @DisplayName("markRead(): throws if ids empty")
    void markRead_empty_throws() {
        MarkReadRequest req = MarkReadRequest.builder()
                .userId(101)
                .notificationIds(Collections.emptyList())
                .build();

        assertThrows(IllegalArgumentException.class, () -> notificationService.markRead(req));
        verify(notificationRepo, never()).markRead(anyInt(), anyCollection());
    }

    @Test
    @DisplayName("markAllRead(): delegates to repo")
    void markAllRead_success() {
        when(notificationRepo.markAllReadByUser(101)).thenReturn(10);
        notificationService.markAllRead(MarkAllReadRequest.builder().userId(101).build());
        verify(notificationRepo).markAllReadByUser(101);
    }

    // --------------------------------------------------
    // SSE subscribe + scheduled scan
    // --------------------------------------------------

    @Test
    @DisplayName("subscribe(): registers emitter, sends backlog + unpushed, marks pushed via transaction")
    void subscribe_backlog_markPushed() {
        int userId = 42;

        Notification unread = Notification.builder()
                .notificationId(1001)
                .user(User.builder().userId(userId).build())
                .notificationType("UNREAD")
                .notificationTitle("U")
                .notificationMessage("M")
                .priority("LOW")
                .notificationStatus("UNREAD")
                .isPushed(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Notification unpushed = Notification.builder()
                .notificationId(1002)
                .user(User.builder().userId(userId).build())
                .notificationType("UNPUSHED")
                .notificationTitle("U2")
                .notificationMessage("M2")
                .priority("HIGH")
                .notificationStatus("UNREAD")
                .isPushed(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(notificationRepo.findTop50ByUser_UserIdAndNotificationStatusAndDeletedFalseOrderByCreatedAtDesc(userId, "UNREAD"))
                .thenReturn(List.of(unread));
        when(notificationRepo.findTop200ByUser_UserIdAndIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(unpushed));

        SseEmitter emitter = notificationService.subscribe(userId);
        assertNotNull(emitter);

        // After subscribe, delivery should have attempted and markPushed invoked for delivered ids
        ArgumentCaptor<Collection<Integer>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(notificationRepo, atLeastOnce()).markPushed(idsCaptor.capture());

        // Delivered IDs should include both 1001 and 1002 (merged & delivered)
        Collection<Integer> delivered = idsCaptor.getValue();
        assertThat(delivered).contains(1001, 1002);
    }

    @Test
    @DisplayName("scanAndPushUnpushed(): when no connections, skips DB scan")
    void scan_skips_when_no_emitters() {
        // No subscribe() called, userEmitters is empty
        notificationService.scanAndPushUnpushed();
        verify(notificationRepo, never()).findTop200ByIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc();
    }

    @Test
    @DisplayName("scanAndPushUnpushed(): pushes to connected users and marks pushed")
    void scan_pushes_and_marks() {
        int userId = 7;

        // Register at least one emitter by calling subscribe
        when(notificationRepo.findTop50ByUser_UserIdAndNotificationStatusAndDeletedFalseOrderByCreatedAtDesc(anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(notificationRepo.findTop200ByUser_UserIdAndIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc(anyInt()))
                .thenReturn(Collections.emptyList());

        notificationService.subscribe(userId);

        Notification p1 = Notification.builder()
                .notificationId(501)
                .user(User.builder().userId(userId).build())
                .notificationTitle("A").notificationMessage("M").notificationStatus("UNREAD")
                .isPushed(false).deleted(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        Notification p2 = Notification.builder()
                .notificationId(502)
                .user(User.builder().userId(userId).build())
                .notificationTitle("B").notificationMessage("N").notificationStatus("UNREAD")
                .isPushed(false).deleted(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(notificationRepo.findTop200ByIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(p1, p2));

        notificationService.scanAndPushUnpushed();

        ArgumentCaptor<Collection<Integer>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(notificationRepo).markPushed(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(501, 502);
    }
}