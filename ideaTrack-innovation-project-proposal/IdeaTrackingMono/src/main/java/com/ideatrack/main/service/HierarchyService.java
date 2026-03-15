package com.ideatrack.main.service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.dto.profilegamification.AdminInfoDTO;
import com.ideatrack.main.dto.profilegamification.HierarchyNodeDTO;
import com.ideatrack.main.dto.profilegamification.IdeaHierarchyDTO;
import com.ideatrack.main.dto.profilegamification.OwnerInfoDTO;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HierarchyService {

    private final IIdeaRepository ideaRepository;
    private final IAssignedReviewerToIdeaRepository assignedReviewerToIdeaRepository;
    private final IUserActivityRepository userActivityRepository;
    private final IUserRepository userRepository;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    /**
     * Builds the idea hierarchy DTO for a given ideaId.
     * Fetches idea, maps assigned reviewers into nodes (including decisions), enriches admin/owner,
     * and produces a timeline that includes reviewer/admin decisions and current/final status.
     *
     * NEW: Also groups nodes by stage into nodesByStage (stage ASC),
     * while preserving the original flat nodes list for backward compatibility.
     */
    public IdeaHierarchyDTO getIdeaHierarchy(Integer ideaId) {
        log.info("Building idea hierarchy for ideaId={}", ideaId);

        Idea idea = fetchIdea(ideaId);

        // 1) Build flat nodes (from reviewer assignments)
        List<HierarchyNodeDTO> nodes = buildNodes(ideaId, idea);

        // 2) Sort flat nodes deterministically: stage ASC, createdAt ASC, id ASC
        nodes = sortNodesAsc(nodes);

        // 3) Build grouped view by stage (TreeMap -> stage keys sorted ASC).
        Map<Integer, List<HierarchyNodeDTO>> nodesByStage = groupNodesByStage(nodes);

        // 4) Compose DTO (keep nodes; add nodesByStage)
        IdeaHierarchyDTO dto = new IdeaHierarchyDTO(
                idea.getIdeaId(),
                idea.getTitle(),
                idea.getDescription(),
                idea.getIdeaStatus() != null ? idea.getIdeaStatus().name() : null,
                nodes // backward compatible
        );
        dto.setNodesByStage(nodesByStage);

        // 5) Enrich admin/owner blocks
        AdminMeta adminMeta = applyAdminInfo(idea, dto);
        applyOwnerInfo(idea, dto);

        // 6) Build timeline
        dto.setTimeline(buildTimeline(idea, nodes, adminMeta.adminName, adminMeta.decision, adminMeta.decisionAt));

        log.info("Idea hierarchy built for ideaId={} with {} nodes ({} stages) and {} timeline entries",
                ideaId,
                nodes.size(),
                nodesByStage.size(),
                dto.getTimeline() != null ? dto.getTimeline().size() : 0
        );

        return dto;
    }

    // --------- HELPERS ----------

    private Idea fetchIdea(Integer ideaId) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> {
                    log.warn("Idea not found for ideaId={}", ideaId);
                    return new IdeaNotFound("Idea not found");
                });

        log.debug("Fetched idea: id={}, status={}, stage={}, title='{}'",
                idea.getIdeaId(),
                idea.getIdeaStatus() != null ? idea.getIdeaStatus().name() : null,
                idea.getStage(),
                idea.getTitle());
        return idea;
    }

    /**
     * Build node DTOs from reviewer assignments (already ordered by stage ASC at the DB level).
     */
    private List<HierarchyNodeDTO> buildNodes(Integer ideaId, Idea idea) {
        var nodeRows = assignedReviewerToIdeaRepository.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(ideaId);
        log.debug("Found {} assigned reviewer rows for ideaId={}", nodeRows.size(), ideaId);

        return nodeRows.stream()
                .map(row -> toEnhancedNode(row, idea))
                .toList();
    }

    /**
     * Sort nodes by (stage ASC, createdAt ASC, id ASC) for deterministic ordering.
     */
    private List<HierarchyNodeDTO> sortNodesAsc(List<HierarchyNodeDTO> nodes) {
        Comparator<HierarchyNodeDTO> cmp = Comparator
                .comparing(HierarchyNodeDTO::getStage, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HierarchyNodeDTO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HierarchyNodeDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        List<HierarchyNodeDTO> sorted = new ArrayList<>(nodes);
        sorted.sort(cmp);
        return sorted;
    }

    /**
     * Group nodes by stage in an ascending stage map (TreeMap).
     * Within each stage list, ensure deterministic ordering (createdAt ASC, id ASC).
     */
    private Map<Integer, List<HierarchyNodeDTO>> groupNodesByStage(List<HierarchyNodeDTO> nodes) {
        // TreeMap ensures stage keys are serialized in ascending order
        Map<Integer, List<HierarchyNodeDTO>> grouped = new TreeMap<>();
        for (HierarchyNodeDTO n : nodes) {
            Integer stage = n.getStage();
            grouped.computeIfAbsent(stage, k -> new ArrayList<>()).add(n);
        }

        // Optionally sort within each stage by createdAt ASC, id ASC (stable display)
        Comparator<HierarchyNodeDTO> withinStageCmp = Comparator
                .comparing(HierarchyNodeDTO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HierarchyNodeDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        grouped.values().forEach(list -> list.sort(withinStageCmp));

        log.debug("nodesByStage built with {} stages: {}",
                grouped.size(),
                grouped.keySet().stream().map(String::valueOf).collect(Collectors.joining(","))
        );
        return grouped;
    }

    // --- Admin enrichment ---

    private static final class AdminMeta {
        final String adminName;
        final String decision;
        final LocalDateTime decisionAt;
        AdminMeta(String adminName, String decision, LocalDateTime decisionAt) {
            this.adminName = adminName;
            this.decision = decision;
            this.decisionAt = decisionAt;
        }
        static AdminMeta empty() { return new AdminMeta(null, null, null); }
    }
    private AdminMeta applyAdminInfo(Idea idea, IdeaHierarchyDTO dto) {
        String adminName = null;
        String adminDecisionText = null;
        LocalDateTime adminDecisionAt = null;

        // Resolve deptId from the idea's category
        Integer deptId = resolveDeptIdFromIdeaCategory(idea);
        if (deptId == null) {
            log.debug("No deptId resolvable from category for ideaId={}; skipping admin lookup", idea.getIdeaId());
            return AdminMeta.empty();
        }

        // Find the (single) ADMIN user for this department
        var adminOpt = userRepository.findFirstByDepartment_DeptIdAndRoleAndDeletedFalse(
                deptId, Constants.Role.ADMIN
        );

        if (adminOpt.isEmpty()) {
            log.debug("No ADMIN user found for deptId={} (ideaId={})", deptId, idea.getIdeaId());
            return AdminMeta.empty();
        }

        var admin = adminOpt.get();
        adminName = admin.getName();
        log.debug("Dept admin identified for ideaId={}: adminUserId={}, adminName={}, deptId={}",
                idea.getIdeaId(), admin.getUserId(), adminName, deptId);

        // Resolve the latest decision by this admin (idea-scoped, same as your original approach)
        UserActivity adminDecisionRow =
                userActivityRepository.findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(
                        admin.getUserId(), idea.getIdeaId()
                );

        if (adminDecisionRow != null && adminDecisionRow.getDecision() != null) {
            adminDecisionText = mapDecisionForHeader(adminDecisionRow.getDecision());
            adminDecisionAt = adminDecisionRow.getCreatedAt();
            log.debug("Admin decision found for ideaId={}: decision={}, at={}",
                    idea.getIdeaId(), adminDecisionText, adminDecisionAt);
        } else {
            log.debug("No admin decision found for ideaId={} (adminUserId={})", idea.getIdeaId(), admin.getUserId());
        }

        AdminInfoDTO adminInfo = AdminInfoDTO.builder()
                .adminUserId(admin.getUserId())
                .adminName(admin.getName())
                .adminRole(admin.getRole() != null ? admin.getRole().name() : null)
                .adminDept(admin.getDepartment() != null ? admin.getDepartment().getDeptName() : null)
                .adminPhoneNo(admin.getPhoneNo())
                .adminEmail(admin.getEmail())
                .adminProfileUrl(admin.getProfileUrl())
                .adminBio(admin.getBio())
                .decision(adminDecisionText)
                .decisionAt(adminDecisionAt)
                .build();

        dto.setAdmin(adminInfo);
        return new AdminMeta(adminName, adminDecisionText, adminDecisionAt);
    }

    // --- Owner enrichment ---

    private void applyOwnerInfo(Idea idea, IdeaHierarchyDTO dto) {
        var owner = idea.getUser();
        if (owner != null) {
            log.debug("Owner identified for ideaId={}: ownerUserId={}, ownerName={}",
                    idea.getIdeaId(), owner.getUserId(), owner.getName());

            OwnerInfoDTO ownerInfo = OwnerInfoDTO.builder()
                    .ownerUserId(owner.getUserId())
                    .ownerName(owner.getName())
                    .ownerRole(owner.getRole() != null ? owner.getRole().name() : null)
                    .ownerDept(owner.getDepartment() != null ? owner.getDepartment().getDeptName() : null)
                    .ownerPhoneNo(owner.getPhoneNo())
                    .ownerEmail(owner.getEmail())
                    .ownerProfileUrl(owner.getProfileUrl())
                    .ownerBio(owner.getBio())
                    .build();
            dto.setOwner(ownerInfo);
        } else {
            log.debug("No owner found for ideaId={}", idea.getIdeaId());
        }
    }

    // --- Node composition ---

    /**
     * Converts an assigned reviewer row into a hierarchy node enriched with a structured decision.
     * Resolves the latest decision from UserActivity (stage-scoped first, then idea-scoped fallback),
     * and attaches public profile info plus assignment meta (stage, feedback, timestamps).
     */
    private HierarchyNodeDTO toEnhancedNode(AssignedReviewerToIdea row, Idea idea) {
        Integer rowId = row.getId();
        log.debug("Building node for rowId={}, ideaId={}, stage={}", rowId, idea.getIdeaId(), row.getStage());

        var reviewer = row.getReviewer();
        Integer reviewerId = (reviewer != null ? reviewer.getUserId() : null);

        DecisionMeta decisionMeta = resolveDecisionMeta(reviewerId, idea.getIdeaId(), row.getStage(), rowId);

        String dept = (reviewer != null && reviewer.getDepartment() != null)
                ? reviewer.getDepartment().getDeptName()
                : null;

        HierarchyNodeDTO node = HierarchyNodeDTO.builder()
                .id(row.getId())
                .reviewerId(reviewerId)
                .reviewerName(reviewer != null ? reviewer.getName() : null)
                .role(reviewer != null && reviewer.getRole() != null ? reviewer.getRole().name() : null)
                .department(dept)
                // Public info
                .phoneNo(reviewer != null ? reviewer.getPhoneNo() : null)
                .email(reviewer != null ? reviewer.getEmail() : null)
                .profileUrl(reviewer != null ? reviewer.getProfileUrl() : null)
                .bio(reviewer != null ? reviewer.getBio() : null)
                // Assignment & decision
                .stage(row.getStage())
                .feedback(row.getFeedback())
                .decision(decisionMeta.decision)
                .decisionAt(decisionMeta.decisionAt)
                .createdAt(row.getCreatedAt())
                .build();

        log.debug("Node built for rowId={} -> reviewerId={}, decision={}, stage={}",
                rowId, reviewerId, decisionMeta.decision, row.getStage());

        return node;
    }

    private static final class DecisionMeta {
        final String decision;
        final LocalDateTime decisionAt;
        DecisionMeta(String decision, LocalDateTime decisionAt) {
            this.decision = decision;
            this.decisionAt = decisionAt;
        }
    }

    private DecisionMeta resolveDecisionMeta(Integer reviewerId, Integer ideaId, Integer stage, Integer rowId) {
        if (reviewerId == null) {
            log.debug("No reviewer associated for rowId={}, defaulting to {}", rowId, STATUS_PENDING);
            return new DecisionMeta(STATUS_PENDING, null);
        }

        UserActivity ua = userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(
                        reviewerId, ideaId, stage
                );

        // Fallback: any latest decision for this idea by this reviewer (not deleted)
        if (ua == null) {
            ua = userActivityRepository
                    .findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(
                            reviewerId, ideaId
                    );
        }

        if (ua != null && ua.getDecision() != null) {
            String mappedDecision = mapDecision(ua.getDecision()); // map enum → string
            LocalDateTime at = ua.getCreatedAt();
            log.debug("Decision resolved for rowId={}, reviewerId={}, decision={}, at={}",
                    rowId, reviewerId, mappedDecision, at);
            return new DecisionMeta(mappedDecision, at);
        } else {
            log.debug("No decision found for rowId={}, reviewerId={}, defaulting to {}", rowId, reviewerId, STATUS_PENDING);
            return new DecisionMeta(STATUS_PENDING, null);
        }
    }

    // --- Active review check ---

    /**
     * Returns true if the idea is in an active review state where reviewer decisions are highlighted.
     * Final states return false to avoid duplicating final status messages.
     */
    private boolean isInActiveReview(Constants.IdeaStatus status) {
        if (status == null) {
            log.debug("isInActiveReview: status is null -> false");
            return false;
        }
        boolean active = switch (status) {
            case SUBMITTED, UNDERREVIEW, PROJECTPROPOSAL -> true;
            default -> false;   // DRAFT, ACCEPTED, APPROVED, REJECTED, REFINE, PENDING
        };
        log.debug("isInActiveReview: status={} -> {}", status.name(), active);
        return active;
    }

    // --- Decision mappers ---

    private String mapDecision(Constants.IdeaStatus d) {
        String mapped;
        if (d == null) {
            mapped = STATUS_PENDING;
        } else {
            mapped = switch (d) {
                case ACCEPTED -> STATUS_ACCEPTED;
                case REJECTED -> STATUS_REJECTED;
                case REFINE   -> "REFINE";
                default       -> STATUS_PENDING;
            };
        }
        log.trace("mapDecision: input={} -> {}", d, mapped);
        return mapped;
    }

    private String mapDecisionForHeader(Constants.IdeaStatus d) {
        String mapped;
        if (d == null) {
            mapped = null;
        } else {
            mapped = switch (d) {
                case APPROVED -> "APPROVED";
                case REJECTED -> STATUS_REJECTED;
                default       -> null;
            };
        }
        log.trace("mapDecisionForHeader: input={} -> {}", d, mapped);
        return mapped;
    }

    // --- Timeline ---

    /**
     * Builds a chronological timeline for the idea showing initialization,
     * reviewer accept/reject/refine decisions (during active review),
     * optional admin decision, and a final/current status line for clarity.
     */
    private List<Map<String, Object>> buildTimeline(
            Idea idea,
            List<HierarchyNodeDTO> nodes,
            String adminName,
            String adminDecision,          // "APPROVED" | "REJECTED" | null
            LocalDateTime adminDecisionAt  // null if none
    ) {
        log.debug("Building timeline for ideaId={} (nodes={}, adminName={}, adminDecision={})",
                idea.getIdeaId(), nodes != null ? nodes.size() : 0, adminName, adminDecision);

        List<Map<String, Object>> list = new ArrayList<>();

        // Idea initialized
        if (idea.getCreatedAt() != null) {
            list.add(line("Idea Initialized", idea.getCreatedAt()));
        }

        // Reviewer decisions during active review
        if (isInActiveReview(idea.getIdeaStatus())) {
            addReviewerTimelineEntries(list, nodes);
        } else {
            log.debug("IdeaId={} is not in active review state; skipping reviewer decisions on timeline", idea.getIdeaId());
        }

        if (adminDecision != null && adminName != null) {
            list.add(line(adminDecision + " by " + adminName, adminDecisionAt));
        }

        // Final / Current Status
        list.add(line(
                buildStatusLabel(idea),
                idea.getUpdatedAt() != null ? idea.getUpdatedAt() : idea.getCreatedAt()
        ));

        list.sort(Comparator.comparing(m -> (LocalDateTime) m.get("date"),
                Comparator.nullsLast(Comparator.naturalOrder())));

        log.debug("Timeline built for ideaId={} with {} entries", idea.getIdeaId(), list.size());
        return list;
    }

    private void addReviewerTimelineEntries(List<Map<String, Object>> list, List<HierarchyNodeDTO> nodes) {
        for (var n : nodes) {
            if (STATUS_ACCEPTED.equalsIgnoreCase(n.getDecision())) {
                list.add(line("Accepted by " + safe(n.getReviewerName()), n.getDecisionAt()));
            } else if (STATUS_REJECTED.equalsIgnoreCase(n.getDecision())) {
                list.add(line("Rejected by " + safe(n.getReviewerName()), n.getDecisionAt()));
            } else if ("REFINE".equalsIgnoreCase(n.getDecision())) {
                list.add(line("Refine requested by " + safe(n.getReviewerName()), n.getDecisionAt()));
            }
        }
    }

    private String buildStatusLabel(Idea idea) {
        String status = idea.getIdeaStatus() != null ? idea.getIdeaStatus().name() : "UNKNOWN";
        boolean isFinal = STATUS_ACCEPTED.equals(status) || "APPROVED".equals(status) || STATUS_REJECTED.equals(status);

        return isFinal
                ? "Final Status: " + status
                : "Current Status: " + status + " (Stage " + idea.getStage() + ")";
    }


    private Integer resolveDeptIdFromIdeaCategory(Idea idea) {
        if (idea == null) return null;
        if (idea.getCategory() == null) {
            log.debug("Idea {} has no category; cannot resolve deptId from category", idea.getIdeaId());
            return null;
        }
        if (idea.getCategory().getDepartment() == null) {
            log.debug("Category {} on idea {} has no department; cannot resolve deptId",
                    idea.getCategory().getCategoryId(), idea.getIdeaId());
            return null;
        }
        return idea.getCategory().getDepartment().getDeptId();
    }

    /**
     * Helper to produce a timeline entry with a title and timestamp.
     */
    private Map<String, Object> line(String title, LocalDateTime dt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("date", dt);
        log.trace("Timeline line added: title='{}', date={}", title, dt);
        return m;
    }

    private String safe(String s) {
        String val = s == null ? "-" : s;
        if (s == null) {
            log.trace("safe(): input is null -> returning '-'");
        }
        return val;
    }
}