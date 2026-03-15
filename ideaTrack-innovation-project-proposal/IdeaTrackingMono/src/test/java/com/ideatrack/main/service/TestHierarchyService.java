package com.ideatrack.main.service;

import com.ideatrack.main.data.*;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestHierarchyService {

    @Mock private IIdeaRepository ideaRepository;
    @Mock private IAssignedReviewerToIdeaRepository assignedReviewerToIdeaRepository;
    @Mock private IUserActivityRepository userActivityRepository;
    @Mock private IUserRepository userRepository; // <-- NEW: mock user repo for admin lookup

    @InjectMocks private HierarchyService hierarchyService;

    private static final Integer IDEA_ID = 101;

    // ---------- Helpers ----------
    private Idea mkIdea(Constants.IdeaStatus status, int stage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Idea idea = new Idea();
        idea.setIdeaId(IDEA_ID);
        idea.setTitle("Smart Hydration");
        idea.setDescription("Track water quality");
        idea.setIdeaStatus(status);
        idea.setStage(stage);
        idea.setCreatedAt(createdAt);
        idea.setUpdatedAt(updatedAt);
        return idea;
    }

    private User mkUser(Integer id, String name) {
        User u = new User();
        u.setUserId(id);
        u.setName(name);

        Department d = new Department();
        d.setDeptId(1);
        d.setDeptName("R&D");
        u.setDepartment(d);

        // Default role can be anything; specific tests will override to ADMIN when needed
        u.setRole(Constants.Role.ADMIN);

        u.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        return u;
    }

    private AssignedReviewerToIdea mkAssignment(Integer id, Idea idea, User reviewer, int stage, String feedback, LocalDateTime createdAt) {
        AssignedReviewerToIdea a = new AssignedReviewerToIdea();
        a.setId(id);
        a.setIdea(idea);
        a.setReviewer(reviewer);
        a.setStage(stage);
        a.setFeedback(feedback);
        a.setCreatedAt(createdAt);
        a.setDeleted(false);
        return a;
    }

    private UserActivity mkDecision(User reviewer, Idea idea, Integer stageId, Constants.IdeaStatus decision, LocalDateTime at) {
        UserActivity ua = new UserActivity();
        ua.setUser(reviewer);
        ua.setIdea(idea);
        ua.setStageId(stageId);
        ua.setDecision(decision);
        ua.setDeleted(false);
        ua.setCreatedAt(at);
        return ua;
    }

    @BeforeEach
    void init() {}

    // ----------------------------------------------------------------------

    @Test
    void getIdeaHierarchy_whenIdeaMissing_throwsIdeaNotFound() {
        when(ideaRepository.findById(IDEA_ID)).thenReturn(Optional.empty());

        assertThrows(IdeaNotFound.class, () -> hierarchyService.getIdeaHierarchy(IDEA_ID));

        verify(ideaRepository).findById(IDEA_ID);
        verifyNoMoreInteractions(assignedReviewerToIdeaRepository, userActivityRepository, userRepository);
    }

    // ----------------------------------------------------------------------

    @Test
    void getIdeaHierarchy_usesNonDeletedAssignmentsOnly_andBuildsNodes_andGroupsByStage() {
        LocalDateTime now = LocalDateTime.now();

        Idea idea = mkIdea(Constants.IdeaStatus.UNDERREVIEW, 2, now.minusDays(5), now.minusDays(1));

        User r1 = mkUser(201, "Reviewer One");
        User r2 = mkUser(202, "Reviewer Two");

        AssignedReviewerToIdea a1 = mkAssignment(1, idea, r1, 1, "Looks good", now.minusDays(4));
        AssignedReviewerToIdea a2 = mkAssignment(2, idea, r2, 2, "Needs metrics", now.minusDays(3));

        when(ideaRepository.findById(IDEA_ID)).thenReturn(Optional.of(idea));
        when(assignedReviewerToIdeaRepository.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID))
                .thenReturn(List.of(a1, a2));

        // No decisions present
        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(null);
        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(null);

        var dto = hierarchyService.getIdeaHierarchy(IDEA_ID);

        // Flat nodes present
        assertEquals(2, dto.getNodes().size());
        assertTrue(dto.getNodes().stream().anyMatch(n -> n.getReviewerName().equals("Reviewer One")));
        assertTrue(dto.getNodes().stream().anyMatch(n -> "PENDING".equalsIgnoreCase(n.getDecision())));

        // Grouped view checks
        assertNotNull(dto.getNodesByStage());
        assertTrue(dto.getNodesByStage().containsKey(1));
        assertTrue(dto.getNodesByStage().containsKey(2));
        assertEquals(1, dto.getNodesByStage().get(1).size());
        assertEquals(1, dto.getNodesByStage().get(2).size());

        // Stage keys ascending (TreeMap)
        List<Integer> stages = new ArrayList<>(dto.getNodesByStage().keySet());
        assertEquals(List.of(1, 2), stages);

        verify(assignedReviewerToIdeaRepository).findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID);
    }

    // ----------------------------------------------------------------------

    @Test
    void decisionResolution_prefersStageScopedOverIdeaScoped_andNodesByStageContainsStage() {
        LocalDateTime now = LocalDateTime.now();

        Idea idea = mkIdea(Constants.IdeaStatus.UNDERREVIEW, 2, now.minusDays(5), now.minusDays(1));
        User reviewer = mkUser(301, "Stage Boss");

        AssignedReviewerToIdea a = mkAssignment(11, idea, reviewer, 2, null, now.minusDays(4));

        LocalDateTime tStage = now.minusDays(2);

        UserActivity stageScoped = mkDecision(reviewer, idea, 2, Constants.IdeaStatus.REFINE, tStage);

        when(ideaRepository.findById(IDEA_ID)).thenReturn(Optional.of(idea));
        when(assignedReviewerToIdeaRepository.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID))
                .thenReturn(List.of(a));

        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(301, IDEA_ID, 2))
                .thenReturn(stageScoped);

        var dto = hierarchyService.getIdeaHierarchy(IDEA_ID);

        var node = dto.getNodes().get(0);
        assertEquals("REFINE", node.getDecision());
        assertEquals(tStage, node.getDecisionAt());

        // nodesByStage has exactly one stage (2) with one node
        assertNotNull(dto.getNodesByStage());
        assertTrue(dto.getNodesByStage().containsKey(2));
        assertEquals(1, dto.getNodesByStage().get(2).size());

        verify(userActivityRepository, never())
                .findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(301, IDEA_ID);
    }

    // ----------------------------------------------------------------------

    @Test
    void decisionResolution_fallsBackToIdeaScoped_whenNoStageScoped_andNodesByStageHasStage1() {
        LocalDateTime now = LocalDateTime.now();

        Idea idea = mkIdea(Constants.IdeaStatus.UNDERREVIEW, 1, now.minusDays(10), now.minusDays(2));
        User reviewer = mkUser(401, "Fallback Judge");

        AssignedReviewerToIdea a = mkAssignment(21, idea, reviewer, 1, null, now.minusDays(9));

        LocalDateTime tIdea = now.minusDays(3);

        UserActivity ideaScoped = mkDecision(reviewer, idea, null, Constants.IdeaStatus.REJECTED, tIdea);

        when(ideaRepository.findById(IDEA_ID)).thenReturn(Optional.of(idea));
        when(assignedReviewerToIdeaRepository.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID))
                .thenReturn(List.of(a));

        // Stage-scoped missing
        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(401, IDEA_ID, 1))
                .thenReturn(null);

        // Fallback used
        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(401, IDEA_ID))
                .thenReturn(ideaScoped);

        var dto = hierarchyService.getIdeaHierarchy(IDEA_ID);

        var node = dto.getNodes().get(0);
        assertEquals("REJECTED", node.getDecision());
        assertEquals(tIdea, node.getDecisionAt());

        // nodesByStage has exactly one stage (1) with one node
        assertNotNull(dto.getNodesByStage());
        assertTrue(dto.getNodesByStage().containsKey(1));
        assertEquals(1, dto.getNodesByStage().get(1).size());
    }

    // ----------------------------------------------------------------------

    @Test
    void timeline_includesAdminFinalDecision_andFinalStatus_nodesByStageEmptyWhenNoAssignments() {
        LocalDateTime now = LocalDateTime.now();

        Idea idea = mkIdea(Constants.IdeaStatus.APPROVED, 3,
                now.minusDays(20), now.minusDays(1));

        // --- Arrange category & admin for NEW logic (category -> department -> user ADMIN) ---
        Category cat = new Category();
        User admin = mkUser(777, "Admin Jane");

        // Ensure role = ADMIN
        admin.setRole(Constants.Role.ADMIN);

        // Ensure category has a department and it matches admin's department
        // (mkUser assigns deptId=1; reuse it)
        cat.setDepartment(admin.getDepartment());
        idea.setCategory(cat);

        // Mock userRepository to return this single ADMIN for the department
        when(userRepository.findFirstByDepartment_DeptIdAndRoleAndDeletedFalse(
                admin.getDepartment().getDeptId(), Constants.Role.ADMIN))
                .thenReturn(Optional.of(admin));

        when(ideaRepository.findById(IDEA_ID))
                .thenReturn(Optional.of(idea));

        when(assignedReviewerToIdeaRepository.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID))
                .thenReturn(Collections.emptyList());

        LocalDateTime tAdmin = now.minusDays(2);

        UserActivity adminDecision =
                mkDecision(admin, idea, null, Constants.IdeaStatus.APPROVED, tAdmin);

        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(777, IDEA_ID))
                .thenReturn(adminDecision);

        var dto = hierarchyService.getIdeaHierarchy(IDEA_ID);

        assertNotNull(dto.getAdmin(), "Admin block should be populated");
        assertEquals("Admin Jane", dto.getAdmin().getAdminName());
        assertEquals("APPROVED", dto.getAdmin().getDecision());
        assertEquals(tAdmin, dto.getAdmin().getDecisionAt());

        var titles = dto.getTimeline().stream()
                .map(m -> (String)m.get("title"))
                .toList();

        assertTrue(titles.contains("APPROVED by Admin Jane"));
        assertTrue(titles.stream().anyMatch(s -> s.startsWith("Final Status: APPROVED")));

        // nodesByStage is empty because there are no assignments
        assertNotNull(dto.getNodesByStage());
        assertTrue(dto.getNodesByStage().isEmpty());
    }

    // ----------------------------------------------------------------------

    @Test
    void timeline_includesReviewerDecisionsOnlyWhenActiveReview_andNodesByStageGrouped() {

        LocalDateTime now = LocalDateTime.now();

        Idea idea = mkIdea(Constants.IdeaStatus.UNDERREVIEW, 2,
                now.minusDays(10), now.minusDays(1));

        User r1 = mkUser(601, "Alice");
        User r2 = mkUser(602, "Bob");

        AssignedReviewerToIdea a1 = mkAssignment(31, idea, r1, 1, null, now.minusDays(9));
        AssignedReviewerToIdea a2 = mkAssignment(32, idea, r2, 2, null, now.minusDays(8));

        LocalDateTime t1 = now.minusDays(7);
        LocalDateTime t2 = now.minusDays(6);

        UserActivity d1 = mkDecision(r1, idea, 1, Constants.IdeaStatus.ACCEPTED, t1);
        UserActivity d2 = mkDecision(r2, idea, 2, Constants.IdeaStatus.REJECTED, t2);

        when(ideaRepository.findById(IDEA_ID)).thenReturn(Optional.of(idea));
        when(assignedReviewerToIdeaRepository
                .findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID))
                .thenReturn(List.of(a1, a2));

        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(601, IDEA_ID, 1))
                .thenReturn(d1);

        when(userActivityRepository
                .findFirstByUser_UserIdAndIdea_IdeaIdAndStageIdAndDecisionIsNotNullAndDeletedFalseOrderByCreatedAtDesc(602, IDEA_ID, 2))
                .thenReturn(d2);

        var dto = hierarchyService.getIdeaHierarchy(IDEA_ID);

        var titles = dto.getTimeline().stream()
                .map(m -> (String)m.get("title"))
                .toList();

        assertTrue(titles.contains("Accepted by Alice"));
        assertTrue(titles.contains("Rejected by Bob"));

        assertTrue(titles.contains("Idea Initialized"));
        assertTrue(titles.stream().anyMatch(s -> s.startsWith("Current Status: UNDERREVIEW")));

        // nodesByStage grouping present
        assertNotNull(dto.getNodesByStage());
        assertEquals(Set.of(1, 2), dto.getNodesByStage().keySet());
        assertEquals(1, dto.getNodesByStage().get(1).size());
        assertEquals(1, dto.getNodesByStage().get(2).size());
    }

    // ----------------------------------------------------------------------
    // NEW TEST: verify ordering — flat nodes sorted by (stage ASC, createdAt ASC, id ASC),
    // and within each stage list, sorted by (createdAt ASC, id ASC).
    // ----------------------------------------------------------------------

    @Test
    void nodes_areSortedByStageAndCreatedAt_andNodesByStageListsAreSorted() {
        LocalDateTime now = LocalDateTime.now();

        Idea idea = mkIdea(Constants.IdeaStatus.UNDERREVIEW, 2, now.minusDays(5), now.minusDays(1));

        User r1 = mkUser(701, "Charlie");
        User r2 = mkUser(702, "Diana");
        User r3 = mkUser(703, "Eve");

        // Intentionally create out-of-order inputs:
        // - Two assignments in stage 2 with different createdAt (t2a older than t2b)
        // - One assignment in stage 1 later in list
        LocalDateTime t2a = now.minusDays(4); // older
        LocalDateTime t2b = now.minusDays(3); // newer
        LocalDateTime t1  = now.minusDays(2);

        AssignedReviewerToIdea aStage2_old  = mkAssignment(41, idea, r1, 2, null, t2a);
        AssignedReviewerToIdea aStage2_new  = mkAssignment(42, idea, r2, 2, null, t2b);
        AssignedReviewerToIdea aStage1      = mkAssignment(40, idea, r3, 1, null, t1);

        when(ideaRepository.findById(IDEA_ID)).thenReturn(Optional.of(idea));
        // Provide out-of-order list to ensure service sorting kicks in
        when(assignedReviewerToIdeaRepository.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(IDEA_ID))
                .thenReturn(List.of(aStage2_old, aStage2_new, aStage1));

        // No decisions needed for ordering assertions (service handles nulls)
        var dto = hierarchyService.getIdeaHierarchy(IDEA_ID);

        // 1) Flat nodes should be ordered by stage ASC, then createdAt ASC, then id ASC
        var flat = dto.getNodes();
        assertEquals(3, flat.size());

        // stage 1 node first
        assertEquals(1, flat.get(0).getStage());
        assertEquals(Integer.valueOf(40), flat.get(0).getId());

        // then stage 2 nodes ordered by createdAt ASC (t2a then t2b)
        assertEquals(2, flat.get(1).getStage());
        assertEquals(Integer.valueOf(41), flat.get(1).getId()); // older createdAt first
        assertEquals(2, flat.get(2).getStage());
        assertEquals(Integer.valueOf(42), flat.get(2).getId());

        // 2) nodesByStage keys ascending and lists sorted by createdAt ASC inside stage
        var byStage = dto.getNodesByStage();
        assertNotNull(byStage);
        List<Integer> keys = new ArrayList<>(byStage.keySet());
        assertEquals(List.of(1, 2), keys);

        assertEquals(1, byStage.get(1).size());
        assertEquals(Integer.valueOf(40), byStage.get(1).get(0).getId());

        assertEquals(2, byStage.get(2).size());
        assertEquals(Integer.valueOf(41), byStage.get(2).get(0).getId()); // older first
        assertEquals(Integer.valueOf(42), byStage.get(2).get(1).getId());
    }
}