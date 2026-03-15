package com.ideatrack.main.controller;

import com.ideatrack.main.dto.idea.bulk.BulkIdeaExportRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationResult;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.service.BulkIdeaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestBulkIdeaController {

    @Autowired
    private BulkIdeaController bulkIdeaController;

    @MockitoBean
    private BulkIdeaService bulkIdeaService;

    // --------------------------------------------------
    // POST /api/ideas/bulk/actions
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("POST /api/ideas/bulk/actions - 200 OK")
    void applyBulkActions_200() throws Exception {
        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(1, 2, 3))
                .delete(true)
                .build();

        BulkIdeaOperationResult result = BulkIdeaOperationResult.builder()
                .requestedCount(3)
                .foundCount(3)
                .updatedCount(3)
                .updatedIds(List.of(1, 2, 3))
                .notFoundIds(List.of())
                .warnings(List.of())
                .build();

        Mockito.when(bulkIdeaService.bulkUpdate(any(BulkIdeaOperationRequest.class)))
                .thenReturn(result);

        ResponseEntity<BulkIdeaOperationResult> resp = bulkIdeaController.applyBulkActions(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getUpdatedCount()).isEqualTo(3);
        assertThat(resp.getBody().getUpdatedIds()).containsExactly(1, 2, 3);
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("POST /api/ideas/bulk/actions - CategoryNotFound bubbles")
    void applyBulkActions_404_category_bubbles() throws Exception {
        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(1))
                .categoryId(77)
                .build();

        Mockito.when(bulkIdeaService.bulkUpdate(any(BulkIdeaOperationRequest.class)))
                .thenThrow(new CategoryNotFound("Category not found: 77"));

        assertThrows(CategoryNotFound.class, () -> bulkIdeaController.applyBulkActions(req));
    }

    // --------------------------------------------------
    // POST /api/ideas/bulk/export
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("POST /api/ideas/bulk/export - 200 CSV + headers + body")
    void exportSelected_200() {
        byte[] csv = "ideaId,title\n1,My Idea".getBytes(StandardCharsets.UTF_8);

        Mockito.when(bulkIdeaService.exportCsv(any(BulkIdeaExportRequest.class)))
                .thenReturn(csv);

        BulkIdeaExportRequest req = BulkIdeaExportRequest.builder()
                .ideaIds(List.of(1, 2))
                .build();

        ResponseEntity<byte[]> resp = bulkIdeaController.exportSelected(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isNotNull();
        assertThat(resp.getHeaders().getContentType().toString()).isEqualTo("text/csv");
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"ideas_");
        assertThat(resp.getBody()).isEqualTo(csv);
    }

    // --------------------------------------------------
    // GET /api/ideas/bulk/export/category/{categoryId}
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /api/ideas/bulk/export/category/{id} - 200 CSV + headers")
    void exportByCategory_200() throws Exception {
        byte[] csv = "ideaId,title\n10,Cat Idea".getBytes(StandardCharsets.UTF_8);

        Mockito.when(bulkIdeaService.exportCsvByCategory(eq(7), eq(false)))
                .thenReturn(csv);

        ResponseEntity<byte[]> resp = bulkIdeaController.exportByCategory(7, false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isNotNull();
        assertThat(resp.getHeaders().getContentType().toString()).isEqualTo("text/csv");
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"ideas_category_7_");
        assertThat(resp.getBody()).isEqualTo(csv);
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /api/ideas/bulk/export/category/{id} - CategoryNotFound bubbles")
    void exportByCategory_404_bubbles() throws Exception {
        Mockito.when(bulkIdeaService.exportCsvByCategory(eq(99), eq(true)))
                .thenThrow(new CategoryNotFound("Category not found: 99"));

        assertThrows(CategoryNotFound.class, () -> bulkIdeaController.exportByCategory(99, true));
    }

    // --------------------------------------------------
    // GET /api/ideas/bulk/export/all
    // --------------------------------------------------

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /api/ideas/bulk/export/all - 200 CSV + headers")
    void exportAll_200() {
        byte[] csv = "ideaId,title\n100,All Idea".getBytes(StandardCharsets.UTF_8);

        Mockito.when(bulkIdeaService.exportCsvAll(eq(false))).thenReturn(csv);

        ResponseEntity<byte[]> resp = bulkIdeaController.exportAll(false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isNotNull();
        assertThat(resp.getHeaders().getContentType().toString()).isEqualTo("text/csv");
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"ideas_all_");
        assertThat(resp.getBody()).isEqualTo(csv);
    }
}