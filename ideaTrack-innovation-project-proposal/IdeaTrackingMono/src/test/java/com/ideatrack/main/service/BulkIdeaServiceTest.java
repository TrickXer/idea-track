package com.ideatrack.main.service;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaExportRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationResult;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkIdeaServiceTest {

    @Mock private IIdeaRepository ideaRepo;
    @Mock private ICategoryRepository categoryRepo;

    @InjectMocks
    private BulkIdeaService bulkIdeaService;

    // --------------------------------------------
    // bulkUpdate(): validations
    // --------------------------------------------

    @Test
    @DisplayName("bulkUpdate(): throws when no operation provided")
    void bulkUpdate_noOps_throws() {
        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(1, 2))
                // no operations set
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bulkIdeaService.bulkUpdate(req));
        assertTrue(ex.getMessage().contains("No bulk operation provided"));
        verify(ideaRepo, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("bulkUpdate(): throws when tag and clearTag both provided")
    void bulkUpdate_contradict_tag_throws() {
        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(1))
                .tag("t1")
                .clearTag(true)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bulkIdeaService.bulkUpdate(req));
        assertTrue(ex.getMessage().contains("either 'tag' or 'clearTag=true'"));
        verify(ideaRepo, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("bulkUpdate(): throws when reviewerFeedback and clearReviewerFeedback both provided")
    void bulkUpdate_contradict_feedback_throws() {
        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(1))
                .reviewerFeedback("ok")
                .clearReviewerFeedback(true)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bulkIdeaService.bulkUpdate(req));
        assertTrue(ex.getMessage().contains("either 'reviewerFeedback' or 'clearReviewerFeedback=true'"));
        verify(ideaRepo, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("bulkUpdate(): throws CategoryNotFound when categoryId provided but missing")
    void bulkUpdate_category_not_found() {
        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(1, 2))
                .categoryId(99)
                .build();

        when(categoryRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFound.class, () -> bulkIdeaService.bulkUpdate(req));
        verify(ideaRepo, never()).saveAll(anyList());
    }

    // --------------------------------------------
    // bulkUpdate(): success path
    // --------------------------------------------

    @Test
    @DisplayName("bulkUpdate(): success with multiple operations + notFound computation")
    void bulkUpdate_success_multi_ops() throws Exception {
        // Prepare ideas
        Idea i1 = new Idea();
        i1.setIdeaId(10);
        i1.setDeleted(false);

        Idea i2 = new Idea();
        i2.setIdeaId(20);
        i2.setDeleted(false);

        // Requested {10,20,30}; mock repo returns 10 & 20; 30 -> not found
        when(ideaRepo.findAllByIdeaIdInAndDeletedFalse(eq(List.of(10, 20, 30))))
                .thenReturn(List.of(i1, i2));

        // Category exists
        Category cat = new Category();
        cat.setCategoryId(5);
        cat.setName("TargetCat");
        when(categoryRepo.findById(5)).thenReturn(Optional.of(cat));

        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(10, 20, 30))
                .ideaStatus(Constants.IdeaStatus.ACCEPTED)
                .categoryId(5)
                .delete(true)
                .tag("t1")
                .reviewerFeedback("good")
                .thumbnailURL("http://img")
                .build();

        BulkIdeaOperationResult result = bulkIdeaService.bulkUpdate(req);

        // Verify save & result
        verify(ideaRepo).saveAll(argThat(list -> ((List) list).size() == 2 && ((List) list).containsAll(List.of(i1, i2))));

        assertEquals(3, result.getRequestedCount());
        assertEquals(2, result.getFoundCount());
        assertEquals(2, result.getUpdatedCount());
        assertThat(result.getUpdatedIds()).containsExactlyInAnyOrder(10, 20);
        assertThat(result.getNotFoundIds()).containsExactly(30);
        assertThat(result.getWarnings()).isEmpty();

        // Validate fields were set on entities
        assertEquals(Constants.IdeaStatus.ACCEPTED, i1.getIdeaStatus());
        assertEquals(cat, i1.getCategory());
        assertEquals("t1", i1.getTag());
        assertEquals("good", i1.getReviewerFeedback());
        assertEquals("http://img", i1.getThumbnailURL());
        assertTrue(i1.isDeleted());

        assertEquals(Constants.IdeaStatus.ACCEPTED, i2.getIdeaStatus());
        assertEquals(cat, i2.getCategory());
        assertEquals("t1", i2.getTag());
        assertEquals("good", i2.getReviewerFeedback());
        assertEquals("http://img", i2.getThumbnailURL());
        assertTrue(i2.isDeleted());
    }

    @Test
    @DisplayName("bulkUpdate(): delete=true but idea already deleted -> warning, no update")
    void bulkUpdate_delete_already_deleted_warning() throws Exception {
        // Even though repo method suggests 'DeletedFalse', we mock a deleted idea to exercise warning logic
        Idea deletedIdea = new Idea();
        deletedIdea.setIdeaId(77);
        deletedIdea.setDeleted(true);

        when(ideaRepo.findAllByIdeaIdInAndDeletedFalse(eq(List.of(77))))
                .thenReturn(List.of(deletedIdea));

        BulkIdeaOperationRequest req = BulkIdeaOperationRequest.builder()
                .ideaIds(List.of(77))
                .delete(true)
                .build();

        BulkIdeaOperationResult result = bulkIdeaService.bulkUpdate(req);

        // No save since updated=0
        verify(ideaRepo, never()).saveAll(anyList());

        assertEquals(1, result.getRequestedCount());
        assertEquals(1, result.getFoundCount());
        assertEquals(0, result.getUpdatedCount());
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("already deleted"));
    }

    // --------------------------------------------
    // exportCsv(): selected IDs
    // --------------------------------------------

    @Test
    @DisplayName("exportCsv(): returns CSV bytes for selected ideas")
    void exportCsv_selected() {
        Idea a = new Idea();
        a.setIdeaId(1);
        a.setTitle("My Idea");
        a.setDeleted(false);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());

        when(ideaRepo.findAllByIdeaIdIn(eq(List.of(1, 2))))
                .thenReturn(List.of(a)); // 2 is not found, but service doesn't compute not-found for export

        byte[] csv = bulkIdeaService.exportCsv(BulkIdeaExportRequest.builder().ideaIds(List.of(1, 2)).build());
        String s = new String(csv, StandardCharsets.UTF_8);

        assertThat(s).startsWith("ideaId,title,");
        assertThat(s).contains("1,My Idea");
    }

    // --------------------------------------------
    // exportCsvByCategory()
    // --------------------------------------------

    @Test
    @DisplayName("exportCsvByCategory(): CategoryNotFound")
    void exportCsvByCategory_notFound() {
        when(categoryRepo.existsById(99)).thenReturn(false);
        assertThrows(CategoryNotFound.class, () -> bulkIdeaService.exportCsvByCategory(99, false));
    }

    @Test
    @DisplayName("exportCsvByCategory(): returns CSV bytes")
    void exportCsvByCategory_success() throws Exception {
        Idea a = new Idea();
        a.setIdeaId(10);
        a.setTitle("Cat Idea");
        a.setDeleted(false);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());

        when(categoryRepo.existsById(5)).thenReturn(true);
        when(ideaRepo.findAllByCategoryForExport(5, false)).thenReturn(List.of(a));

        byte[] csv = bulkIdeaService.exportCsvByCategory(5, false);
        String s = new String(csv, StandardCharsets.UTF_8);

        assertThat(s).contains("ideaId,title,");
        assertThat(s).contains("10,Cat Idea");
    }

    // --------------------------------------------
    // exportCsvAll()
    // --------------------------------------------

    @Test
    @DisplayName("exportCsvAll(): returns CSV bytes for all (includeDeleted=false)")
    void exportCsvAll_success() {
        Idea a = new Idea();
        a.setIdeaId(100);
        a.setTitle("All Idea");
        a.setDeleted(false);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());

        when(ideaRepo.findAllForExport(false)).thenReturn(List.of(a));

        byte[] csv = bulkIdeaService.exportCsvAll(false);
        String s = new String(csv, StandardCharsets.UTF_8);

        assertThat(s).contains("ideaId,title,");
        assertThat(s).contains("100,All Idea");
    }
}