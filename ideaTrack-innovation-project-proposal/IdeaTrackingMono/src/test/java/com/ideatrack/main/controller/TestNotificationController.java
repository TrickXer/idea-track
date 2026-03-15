package com.ideatrack.main.controller;

import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.notification.*;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.service.NotificationService;
import com.ideatrack.main.service.ReactiveNotificationStreamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestNotificationController {

    @Autowired
    private NotificationController notificationController;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private ReactiveNotificationStreamService streamService;

    // --------------------------------------------------
    // Create
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("POST /api/notifications - 201 Created + body")
    void create_201() throws Exception {
        NotificationCreateRequest req = NotificationCreateRequest.builder()
                .userId(101)
                .notificationType("IDEA_STATUS")
                .notificationTitle("Idea Approved")
                .notificationMessage("Your idea has been approved.")
                .priority("HIGH")
                .metadata("{\"redirectTo\":\"/ideas/55\"}")
                .build();

        NotificationResponseDTO created = NotificationResponseDTO.builder()
                .notificationId(999)
                .userId(101)
                .notificationType("IDEA_STATUS")
                .notificationTitle("Idea Approved")
                .notificationMessage("Your idea has been approved.")
                .priority("HIGH")
                .notificationStatus("UNREAD")
                .pushed(false)
                .metadata("{\"redirectTo\":\"/ideas/55\"}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(notificationService.create(any(NotificationCreateRequest.class)))
                .thenReturn(created);

        ResponseEntity<NotificationResponseDTO> resp = notificationController.create(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getNotificationId()).isEqualTo(999);
        assertThat(resp.getBody().getUserId()).isEqualTo(101);
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("POST /api/notifications - UserNotFound bubbles")
    void create_404_user_bubbles() throws Exception {
        NotificationCreateRequest req = NotificationCreateRequest.builder()
                .userId(404)
                .notificationType("SYSTEM")
                .notificationTitle("X")
                .notificationMessage("Y")
                .priority("LOW")
                .build();

        Mockito.when(notificationService.create(any(NotificationCreateRequest.class)))
                .thenThrow(new UserNotFoundException("User not found: 404"));

        assertThrows(UserNotFoundException.class, () -> notificationController.create(req));
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("POST /api/notifications/bulk - 201 Created + body")
    void createBulk_201() throws Exception {
        NotificationCreateRequest item = NotificationCreateRequest.builder()
                .userId(101)
                .notificationType("COMMENT")
                .notificationTitle("New Comment")
                .notificationMessage("Someone commented on your idea.")
                .priority("MEDIUM")
                .build();

        NotificationBulkCreateRequest req = NotificationBulkCreateRequest.builder()
                .items(List.of(item))
                .build();

        NotificationResponseDTO out = NotificationResponseDTO.builder()
                .notificationId(11).userId(101)
                .notificationType("COMMENT")
                .notificationTitle("New Comment")
                .notificationMessage("Someone commented on your idea.")
                .priority("MEDIUM")
                .notificationStatus("UNREAD")
                .pushed(false)
                .build();

        Mockito.when(notificationService.createBulk(any(NotificationBulkCreateRequest.class)))
                .thenReturn(List.of(out));

        ResponseEntity<?> resp = notificationController.createBulk(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<NotificationResponseDTO> list = (List<NotificationResponseDTO>) resp.getBody();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getNotificationId()).isEqualTo(11);
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("POST /api/notifications/bulk - UserNotFound bubbles")
    void createBulk_404_user_bubbles() throws Exception {
        NotificationBulkCreateRequest req = NotificationBulkCreateRequest.builder()
                .items(List.of(
                        NotificationCreateRequest.builder()
                                .userId(9999)
                                .notificationType("SYS")
                                .notificationTitle("Title")
                                .notificationMessage("Msg")
                                .priority("LOW")
                                .build()
                ))
                .build();

        Mockito.when(notificationService.createBulk(any(NotificationBulkCreateRequest.class)))
                .thenThrow(new UserNotFoundException("User not found: 9999"));

        assertThrows(UserNotFoundException.class, () -> notificationController.createBulk(req));
    }

    // --------------------------------------------------
    // List
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /api/notifications - 200 OK + pageable/sort mapping")
    void list_200_ok() {
        NotificationResponseDTO a = NotificationResponseDTO.builder()
                .notificationId(1).userId(101).notificationTitle("A").notificationStatus("UNREAD").build();
        NotificationResponseDTO b = NotificationResponseDTO.builder()
                .notificationId(2).userId(101).notificationTitle("B").notificationStatus("READ").build();

        PagedResponse<NotificationResponseDTO> pr = PagedResponse.<NotificationResponseDTO>builder()
                .content(List.of(a, b))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        Mockito.when(notificationService.listByUser(anyInt(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pr);

        ResponseEntity<PagedResponse<NotificationResponseDTO>> resp =
                notificationController.list(
                        101,
                        "ALL",
                        null,
                        "2026-01-01T00:00:00",
                        "2026-01-31T23:59:59",
                        0,
                        20,
                        "createdAt,desc"
                );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getContent()).hasSize(2);

        // Verify correct pageable mapping
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(notificationService).listByUser(eq(101), eq("ALL"), isNull(), any(), any(), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().iterator().next().getProperty()).isEqualTo("createdAt");
        assertThat(used.getSort().iterator().next().isDescending()).isTrue();
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /api/notifications - invalid datetime bubbles IllegalArgumentException")
    void list_400_invalid_datetime_bubbles() {
        assertThrows(IllegalArgumentException.class, () ->
                notificationController.list(101, "UNREAD", null, "not-a-date", null, 0, 20, "createdAt,desc"));
    }

    // --------------------------------------------------
    // Mark read
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("PATCH /api/notifications/read - 204 No Content")
    void markRead_204() {
        MarkReadRequest req = MarkReadRequest.builder()
                .userId(101)
                .notificationIds(List.of(1, 2, 3))
                .build();

        ResponseEntity<Void> resp = notificationController.markRead(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Mockito.verify(notificationService).markRead(eq(req));
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("PATCH /api/notifications/read-all - 204 No Content")
    void markAllRead_204() {
        MarkAllReadRequest req = MarkAllReadRequest.builder().userId(101).build();
        ResponseEntity<Void> resp = notificationController.markAllRead(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Mockito.verify(notificationService).markAllRead(eq(req));
    }

    // --------------------------------------------------
    // SSE
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /api/notifications/stream - 200 OK returns SseEmitter and calls service")
    void stream_200() {
        SseEmitter emitter = new SseEmitter();
        Mockito.when(notificationService.subscribe(101)).thenReturn(emitter);

        ResponseEntity<SseEmitter> resp = notificationController.stream(101);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(emitter);
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(resp.getHeaders().getFirst(HttpHeaders.PRAGMA)).isEqualTo("no-cache");

        Mockito.verify(notificationService).subscribe(101);
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /api/notifications/stream/reactive - delegates to ReactiveNotificationStreamService")
    void reactiveStream_200() {
        NotificationSseDTO dto = NotificationSseDTO.builder()
                .notificationId(7).title("T").message("M").build();
        Flux<ServerSentEvent<NotificationSseDTO>> flux =
                Flux.just(ServerSentEvent.builder(dto).event("notification").build());

        Mockito.when(streamService.subscribeFlux(101)).thenReturn(flux);

        Flux<ServerSentEvent<NotificationSseDTO>> out = notificationController.reactiveStream(101);
        assertThat(out).isNotNull();
        Mockito.verify(streamService).subscribeFlux(101);
    }
}