package com.ideatrack.main.controller;

import com.ideatrack.main.dto.idea.bulk.BulkIdeaExportRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationResult;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.service.BulkIdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/ideas/bulk")
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN')")
public class BulkIdeaController {

    private static final String CSV_MEDIA_TYPE = "text/csv";
    private static final String ATTACHMENT_FILENAME_PREFIX = "attachment; filename=\"";

    private final BulkIdeaService bulkIdeaService;

    /**
     * Apply bulk operations to selected ideas:
     * - ideaStatus
     * - categoryId (move)
     * - delete (soft delete)
     * - tag / clearTag
     * - reviewerFeedback / clearReviewerFeedback
     * - thumbnailURL
     */
    @PostMapping("/actions")
    public ResponseEntity<BulkIdeaOperationResult> applyBulkActions(
            @Valid @RequestBody BulkIdeaOperationRequest request) throws CategoryNotFound {

        log.info("Received bulk actions request");
        BulkIdeaOperationResult result = bulkIdeaService.bulkUpdate(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Export selected idea IDs to CSV (explicit selection).
     * Includes deleted and non-deleted (because caller gave IDs explicitly).
     */
    @PostMapping(value = "/export", produces = CSV_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportSelected(@Valid @RequestBody BulkIdeaExportRequest request) {

        byte[] csv = bulkIdeaService.exportCsv(request);
        String fileName = BulkIdeaService.buildCsvFileName();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + fileName + "\"")
                .body(csv);
    }

    /**
     * Export all ideas of a given category to CSV.
     * Default: includeDeleted=false (export only active ideas).
     * Pass includeDeleted=true to include soft-deleted rows as well.
     */
    @GetMapping(value = "/export/category/{categoryId}", produces = CSV_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportByCategory(@PathVariable Integer categoryId,
                                                   @RequestParam(defaultValue = "false") boolean includeDeleted)
            throws CategoryNotFound {

        byte[] csv = bulkIdeaService.exportCsvByCategory(categoryId, includeDeleted);
        String fileName = BulkIdeaService.buildCsvFileNameForCategory(categoryId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + fileName + "\"")
                .body(csv);
    }

    /**
     * Export all ideas across categories to CSV.
     * Default: includeDeleted=false (export only active ideas).
     * Pass includeDeleted=true to include soft-deleted rows as well.
     */
    @GetMapping(value = "/export/all", produces = CSV_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportAll(@RequestParam(defaultValue = "false") boolean includeDeleted) {

        byte[] csv = bulkIdeaService.exportCsvAll(includeDeleted);
        String fileName = BulkIdeaService.buildCsvFileNameAll();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + fileName + "\"")
                .body(csv);
    }
}
