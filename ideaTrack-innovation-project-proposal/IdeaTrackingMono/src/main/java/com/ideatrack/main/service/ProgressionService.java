package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.dto.reviewer.ProgressionDTO;
import com.ideatrack.main.dto.reviewer.ProgressionDTO.Bar;
import com.ideatrack.main.dto.reviewer.ProgressionDTO.Step;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.repository.IIdeaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Builds a progression-bar timeline **only** from the idea table:
 *
 * Flow (with REFINE between review and final):
 *   SUBMITTED -> UNDERREVIEW -> REFINE -> (ACCEPTED | REJECTED)
 *                    |
 *                    └-> (direct ACCEPTED possible without REFINE)
 *   If ACCEPTED -> PROJECTPROPOSAL -> (APPROVED | REJECTED)
 *
 * Rules:
 * - 'PENDING' is treated as UNDERREVIEW for display.
 * - Bars are filled up to the current status.
 * - Current node is marked 'active=true' with timestamp = idea.updatedAt (or createdAt for SUBMITTED).
 * - We **guess** whether REJECTED is final (after PROPOSAL) or review-level using stage/stageCount if present.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProgressionService {

    private final IIdeaRepository ideaRepo;

    // Canonical node keys
    private static final String SUBMITTED        = "SUBMITTED";
    private static final String UNDERREVIEW      = "UNDERREVIEW";
    private static final String REFINE           = "REFINE";
    private static final String ACCEPTED         = "ACCEPTED";
    private static final String PROJECTPROPOSAL  = "PROJECTPROPOSAL";
    private static final String APPROVED         = "APPROVED";
    private static final String REJECTED         = "REJECTED";

    /**
     * Build the progression using only idea row.
     */
    public ProgressionDTO build(Integer ideaId) {
        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new IdeaNotFound("Idea not found: " + ideaId));
        if (idea.isDeleted()) throw new IdeaNotFound("Idea deleted: " + ideaId);

        String current = normalize(idea.getIdeaStatus());
        LocalDateTime createdAt = idea.getCreatedAt();
        LocalDateTime updatedAt = (idea.getUpdatedAt() != null ? idea.getUpdatedAt() : createdAt);

        // Prepare nodes
        Map<String, Step> nodes = new LinkedHashMap<>();
        for (String k : orderedNodes()) {
            nodes.put(k, Step.builder()
                    .key(k)
                    .label(k)
                    .reached(false)
                    .active(false)
                    .at(null)
                    .build());
        }

        // Prepare bars (edges) template
        List<Bar> bars = initBars();

        // SUBMITTED is always reached at createdAt
        markReached(nodes, SUBMITTED, createdAt, false);

        // Fill up to current
        switch (current) {
            case SUBMITTED -> {
                markActive(nodes, SUBMITTED, createdAt);
            }

            case UNDERREVIEW -> {
                // SUBMITTED -> UNDERREVIEW
                fillBar(bars, SUBMITTED, UNDERREVIEW);
                markReached(nodes, UNDERREVIEW, updatedAt, true);
            }

            case REFINE -> {
                // SUBMITTED -> UNDERREVIEW -> REFINE
                fillBar(bars, SUBMITTED, UNDERREVIEW);
                markReached(nodes, UNDERREVIEW, null, false);
                fillBar(bars, UNDERREVIEW, REFINE);
                markReached(nodes, REFINE, updatedAt, true);
            }

            case ACCEPTED -> {
                // Two variants are possible in real life:
                // (a) direct: SUBMITTED -> UNDERREVIEW -> ACCEPTED
                // (b) with refine: SUBMITTED -> UNDERREVIEW -> REFINE -> ACCEPTED
                // With only idea row we can't know if refine happened before; show direct path (cleanest).
                fillBar(bars, SUBMITTED, UNDERREVIEW);
                markReached(nodes, UNDERREVIEW, null, false);
                fillBar(bars, UNDERREVIEW, ACCEPTED);
                markReached(nodes, ACCEPTED, updatedAt, true);
            }

            case PROJECTPROPOSAL -> {
                // SUBMITTED -> UNDERREVIEW -> ACCEPTED -> PROJECTPROPOSAL
                fillBar(bars, SUBMITTED, UNDERREVIEW);
                markReached(nodes, UNDERREVIEW, null, false);
                fillBar(bars, UNDERREVIEW, ACCEPTED);
                markReached(nodes, ACCEPTED, null, false);
                fillBar(bars, ACCEPTED, PROJECTPROPOSAL);
                markReached(nodes, PROJECTPROPOSAL, updatedAt, true);
            }

            case APPROVED -> {
                // SUBMITTED -> UNDERREVIEW -> ACCEPTED -> PROJECTPROPOSAL -> APPROVED
                fillBar(bars, SUBMITTED, UNDERREVIEW);
                markReached(nodes, UNDERREVIEW, null, false);
                fillBar(bars, UNDERREVIEW, ACCEPTED);
                markReached(nodes, ACCEPTED, null, false);
                fillBar(bars, ACCEPTED, PROJECTPROPOSAL);
                markReached(nodes, PROJECTPROPOSAL, null, false);
                fillBar(bars, PROJECTPROPOSAL, APPROVED);
                markReached(nodes, APPROVED, updatedAt, true);
            }

            case REJECTED -> {
                // Decide: review rejection or final rejection?
                boolean finalReject = isFinalReject(idea);
                if (finalReject) {
                    // SUBMITTED -> UNDERREVIEW -> ACCEPTED -> PROJECTPROPOSAL -> REJECTED
                    fillBar(bars, SUBMITTED, UNDERREVIEW);
                    markReached(nodes, UNDERREVIEW, null, false);
                    fillBar(bars, UNDERREVIEW, ACCEPTED);
                    markReached(nodes, ACCEPTED, null, false);
                    fillBar(bars, ACCEPTED, PROJECTPROPOSAL);
                    markReached(nodes, PROJECTPROPOSAL, null, false);
                    fillBar(bars, PROJECTPROPOSAL, REJECTED);
                    markReached(nodes, REJECTED, updatedAt, true);
                } else {
                    // SUBMITTED -> UNDERREVIEW -> REJECTED
                    fillBar(bars, SUBMITTED, UNDERREVIEW);
                    markReached(nodes, UNDERREVIEW, null, false);
                    fillBar(bars, UNDERREVIEW, REJECTED);
                    markReached(nodes, REJECTED, updatedAt, true);
                }
            }

            default -> {
                // Unknown/null -> treat as SUBMITTED
                markActive(nodes, SUBMITTED, createdAt);
            }
        }

        return ProgressionDTO.builder()
                .ideaId(idea.getIdeaId())
                .currentStatus(current)
                .steps(new ArrayList<>(nodes.values()))
                .bars(bars)
                .build();
    }

    // ---------------- helpers ----------------

    private List<String> orderedNodes() {
        // The canonical order for UI rendering
        return List.of(
                SUBMITTED,
                UNDERREVIEW,
                REFINE,            // in-between node
                ACCEPTED,
                PROJECTPROPOSAL,
                APPROVED,
                REJECTED           // terminal node on either branch
        );
    }

    private List<Bar> initBars() {
        // We include both possible branches; UI will just render them:
        // direct + via REFINE; and both reject branches (review & final)
        return new ArrayList<>(List.of(
                Bar.builder().fromKey(SUBMITTED).toKey(UNDERREVIEW).filled(false).build(),

                // Between review and outcomes
                Bar.builder().fromKey(UNDERREVIEW).toKey(REFINE).filled(false).build(),
                Bar.builder().fromKey(REFINE).toKey(ACCEPTED).filled(false).build(),
                Bar.builder().fromKey(REFINE).toKey(REJECTED).filled(false).build(),
                Bar.builder().fromKey(UNDERREVIEW).toKey(ACCEPTED).filled(false).build(), // direct accept without refine
                Bar.builder().fromKey(UNDERREVIEW).toKey(REJECTED).filled(false).build(), // review reject

                // Post-accept path
                Bar.builder().fromKey(ACCEPTED).toKey(PROJECTPROPOSAL).filled(false).build(),
                Bar.builder().fromKey(PROJECTPROPOSAL).toKey(APPROVED).filled(false).build(),
                Bar.builder().fromKey(PROJECTPROPOSAL).toKey(REJECTED).filled(false).build() // final reject
        ));
    }

    private void fillBar(List<Bar> bars, String from, String to) {
        bars.stream()
                .filter(b -> b.getFromKey().equals(from) && b.getToKey().equals(to))
                .findFirst()
                .ifPresent(b -> b.setFilled(true));
    }

    private void markReached(Map<String, Step> nodes, String key, LocalDateTime at, boolean active) {
        Step s = nodes.get(key);
        if (s == null) return;
        nodes.put(key, Step.builder()
                .key(s.getKey())
                .label(s.getLabel())
                .reached(true)
                .active(active)
                .at(at != null ? at : s.getAt())
                .build());
    }

    private void markActive(Map<String, Step> nodes, String key, LocalDateTime at) {
        Step s = nodes.get(key);
        if (s == null) return;
        nodes.put(key, Step.builder()
                .key(s.getKey())
                .label(s.getLabel())
                .reached(true)
                .active(true)
                .at(at != null ? at : s.getAt())
                .build());
    }

    private String normalize(Constants.IdeaStatus status) {
        if (status == null) return SUBMITTED;
        // Hide PENDING as UNDERREVIEW for display continuity
        if (status == Constants.IdeaStatus.PENDING) return UNDERREVIEW;
        return status.name(); // keep enum names as-is
    }

    /**
     * Heuristic: if we are REJECTED and stage appears at/after proposal,
     * treat as final rejection. If category or stageCount is missing, default to review-level reject.
     */
    private boolean isFinalReject(Idea idea) {
        if (idea.getIdeaStatus() != Constants.IdeaStatus.REJECTED) return false;
        try {
            Integer stage = idea.getStage();
            Integer stageCount = (idea.getCategory() != null) ? idea.getCategory().getStageCount() : null;
            if (stage != null && stageCount != null) {
                // If we have already reached or passed proposal stage (post-review),
                // consider this a final rejection.
                return stage >= stageCount;
            }
        } catch (Exception ignored) {}
        return false; // default: review rejection (UNDERREVIEW -> REJECTED)
    }
}