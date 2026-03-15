package com.ideatrack.main.service;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaExportRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationRequest;
import com.ideatrack.main.dto.idea.bulk.BulkIdeaOperationResult;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BulkIdeaService {

    private static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";

    private final IIdeaRepository ideaRepo;
    private final ICategoryRepository categoryRepo;

    /**
     * Apply bulk operations on selected ideas.
     * Supported ops:
     *  - ideaStatus
     *  - categoryId (move)
     *  - delete (soft delete)
     *  - tag / clearTag
     *  - reviewerFeedback / clearReviewerFeedback
     *  - thumbnailURL
     */
    public BulkIdeaOperationResult bulkUpdate(BulkIdeaOperationRequest req) throws CategoryNotFound {
        log.info("Bulk update requested: {} IDs", req.getIdeaIds() != null ? req.getIdeaIds().size() : 0);

        // Validate at least one operation present
        if (!hasAnyOperation(req)) {
            throw new IllegalArgumentException("No bulk operation provided. Provide at least one field to update.");
        }

        // Disallow contradictory flags
        if (Boolean.TRUE.equals(req.getClearTag()) && req.getTag() != null) {
            throw new IllegalArgumentException("Provide either 'tag' or 'clearTag=true', not both.");
        }
        if (Boolean.TRUE.equals(req.getClearReviewerFeedback()) && req.getReviewerFeedback() != null) {
            throw new IllegalArgumentException("Provide either 'reviewerFeedback' or 'clearReviewerFeedback=true', not both.");
        }

        // Pre-fetch target category if asked to move
        Category newCategory = null;
        if (req.getCategoryId() != null) {
            newCategory = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFound("Category not found: " + req.getCategoryId()));
        }

        // Load only ACTIVE (non-deleted) ideas for updates
        List<Idea> ideas = ideaRepo.findAllByIdeaIdInAndDeletedFalse(req.getIdeaIds());

        // Compute not-found subset from requestedIds - foundIds
        Set<Integer> foundIds = ideas.stream().map(Idea::getIdeaId).collect(Collectors.toSet());
        List<Integer> notFound = req.getIdeaIds().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        List<String> warnings = new ArrayList<>();
        int updated = 0;

        for (Idea i : ideas) {
            boolean changed = false;

            if (req.getIdeaStatus() != null) {
                i.setIdeaStatus(req.getIdeaStatus());
                changed = true;
            }

            if (newCategory != null) {
                i.setCategory(newCategory);
                changed = true;
            }

            if (Boolean.TRUE.equals(req.getClearTag())) {
                i.setTag(null);
                changed = true;
            } else if (req.getTag() != null) {
                i.setTag(req.getTag());
                changed = true;
            }

            if (Boolean.TRUE.equals(req.getClearReviewerFeedback())) {
                i.setReviewerFeedback(null);
                changed = true;
            } else if (req.getReviewerFeedback() != null) {
                i.setReviewerFeedback(req.getReviewerFeedback());
                changed = true;
            }

            if (req.getThumbnailURL() != null) {
                i.setThumbnailURL(req.getThumbnailURL());
                changed = true;
            }

            if (Boolean.TRUE.equals(req.getDelete())) {
                if (i.isDeleted()) {
                    warnings.add("Idea already deleted: " + i.getIdeaId());
                } else {
                    i.setDeleted(true);
                    changed = true;
                }
            }

            if (changed) updated++;
        }

        if (updated > 0) {
            ideaRepo.saveAll(ideas);
        }

        BulkIdeaOperationResult result = BulkIdeaOperationResult.builder()
                .requestedCount(req.getIdeaIds().size())
                .foundCount(ideas.size())
                .updatedCount(updated)
                .updatedIds(ideas.stream().map(Idea::getIdeaId).toList())
                .notFoundIds(notFound)
                .warnings(warnings)
                .build();

        log.info("Bulk update completed: requested={}, found={}, updated={}",
                result.getRequestedCount(), result.getFoundCount(), result.getUpdatedCount());
        if (!notFound.isEmpty()) {
            log.warn("Bulk update: not found IDs: {}", notFound);
        }
        if (!warnings.isEmpty()) {
            log.warn("Bulk update warnings: {}", warnings);
        }

        return result;
    }

    private boolean hasAnyOperation(BulkIdeaOperationRequest r) {
        return r.getIdeaStatus() != null
                || r.getCategoryId() != null
                || Boolean.TRUE.equals(r.getDelete())
                || r.getTag() != null
                || Boolean.TRUE.equals(r.getClearTag())
                || r.getReviewerFeedback() != null
                || Boolean.TRUE.equals(r.getClearReviewerFeedback())
                || r.getThumbnailURL() != null;
    }

    /**
     * Export a specific selection of idea IDs to CSV.
     * Includes both deleted and non-deleted ideas (because selection is explicit).
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(BulkIdeaExportRequest req) {
        log.info("Export CSV requested for {} ideas", req.getIdeaIds().size());
        List<Idea> ideas = ideaRepo.findAllByIdeaIdIn(req.getIdeaIds());
        byte[] bytes = buildCsv(ideas);
        log.info("Export CSV (selected) completed: rows={}, bytes={}", ideas.size(), bytes.length);
        return bytes;
    }

    /**
     * Export all ideas of a given category to CSV.
     * By default excludes deleted; pass includeDeleted=true to include them.
     */
    @Transactional(readOnly = true)
    public byte[] exportCsvByCategory(Integer categoryId, boolean includeDeleted) throws CategoryNotFound {
        log.info("Export CSV by category: categoryId={}, includeDeleted={}", categoryId, includeDeleted);

        boolean exists = categoryRepo.existsById(categoryId);
        if (!exists) {
            throw new CategoryNotFound("Category not found: " + categoryId);
        }

        List<Idea> ideas = ideaRepo.findAllByCategoryForExport(categoryId, includeDeleted);
        byte[] bytes = buildCsv(ideas);
        log.info("Export CSV by category completed: rows={}, bytes={}", ideas.size(), bytes.length);
        return bytes;
    }

    /**
     * Export all ideas across categories to CSV.
     * By default excludes deleted; pass includeDeleted=true to include them.
     */
    @Transactional(readOnly = true)
    public byte[] exportCsvAll(boolean includeDeleted) {
        log.info("Export CSV for ALL ideas, includeDeleted={}", includeDeleted);
        List<Idea> ideas = ideaRepo.findAllForExport(includeDeleted);
        byte[] bytes = buildCsv(ideas);
        log.info("Export CSV ALL completed: rows={}, bytes={}", ideas.size(), bytes.length);
        return bytes;
    }

    // ---------------------------
    // CSV Helpers
    // ---------------------------

    private byte[] buildCsv(List<Idea> ideas) {
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("ideaId,title,description,problemStatement,stage,ideaStatus,categoryId,categoryName,userId,userName,tag,thumbnailURL,reviewerFeedback,createdAt,updatedAt,deleted\n");

        for (Idea i : ideas) {
            sb.append(csv(i.getIdeaId()))
              .append(',').append(csv(i.getTitle()))
              .append(',').append(csv(i.getDescription()))
              .append(',').append(csv(i.getProblemStatement()))
              .append(',').append(csv(i.getStage()))
              .append(',').append(csv(i.getIdeaStatus() != null ? i.getIdeaStatus().name() : null))
              .append(',').append(csv(i.getCategory() != null ? i.getCategory().getCategoryId() : null))
              .append(',').append(csv(i.getCategory() != null ? i.getCategory().getName() : null))
              .append(',').append(csv(i.getUser() != null ? i.getUser().getUserId() : null))
              .append(',').append(csv(i.getUser() != null ? i.getUser().getName() : null))
              .append(',').append(csv(i.getTag()))
              .append(',').append(csv(i.getThumbnailURL()))
              .append(',').append(csv(i.getReviewerFeedback()))
              .append(',').append(csv(i.getCreatedAt()))
              .append(',').append(csv(i.getUpdatedAt()))
              .append(',').append(csv(i.isDeleted()))
              .append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value).replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    public static String buildCsvFileName() {
        String ts = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        return "ideas_" + ts + ".csv";
    }

    public static String buildCsvFileNameForCategory(Integer categoryId) {
        String ts = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        return "ideas_category_" + categoryId + "_" + ts + ".csv";
    }

    public static String buildCsvFileNameAll() {
        String ts = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        return "ideas_all_" + ts + ".csv";
    }
}
