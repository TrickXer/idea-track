package com.ideatrack.main.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ideatrack.main.data.Constants.IdeaStatus;
import com.ideatrack.main.data.Constants.Status;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.dto.admin.AdminHealthSummaryDto;
import com.ideatrack.main.dto.admin.AdminOverdueReviewerDto;
import com.ideatrack.main.dto.admin.AdminProposalSummaryDto;
import com.ideatrack.main.dto.admin.AdminUpdateUserStatusRequest;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.repository.ReviewerAggregateProjection;

import org.mockito.junit.jupiter.MockitoExtension;

//Done By Vibhuti //tested 100%

@ExtendWith(MockitoExtension.class)
public class TestAdminService {
	
    @Mock private IProposalRepository proposalRepository;
    @Mock private IReviewerCategoryRepository assignmentRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IAssignedReviewerToIdeaRepository assignedReviewerToIdeaRepository;
    @Mock private IIdeaRepository ideaRepository;

    @InjectMocks
    private AdminDashboardService service;

    // ----------------------------- listPendingOrStuckProposals -----------------------------

    @Test
    void listPendingOrStuckProposals_shouldMapEntitiesToDtos_andPassFilters() {
        String status = "underreview";
        Long stageId = 2L;
        Long reviewerId = 5L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // Mock proposals as simple Mockito stubs (avoid relying on entity setters)
        Proposal p1 = mock(Proposal.class);
        lenient().when(p1.getIdeaStatus()).thenReturn(IdeaStatus.UNDERREVIEW);
        lenient().when(p1.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        Proposal p2 = mock(Proposal.class);
        lenient().when(p2.getIdeaStatus()).thenReturn(IdeaStatus.ACCEPTED);
        lenient().when(p2.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 1, 2, 8, 30)); // use LDT here too

        Page<Proposal> page = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(proposalRepository.searchAdminList(any(IdeaStatus.class), any(Integer.class), any(Integer.class), any(Pageable.class))).thenReturn(page);

        // ACT
        Page<AdminProposalSummaryDto> out = service.listPendingOrStuckProposals(status, stageId, reviewerId, pageable);

        // ASSERT repo was called
        verify(proposalRepository).searchAdminList(eq(IdeaStatus.UNDERREVIEW), eq(2), eq(5), eq(pageable));

        // ASSERT mapping
        assertEquals(2, out.getTotalElements());
        AdminProposalSummaryDto d1 = out.getContent().get(0);
        assertEquals("underreview", d1.status()); // lowercased in service
        assertNotNull(d1.lastUpdatedAt());

        AdminProposalSummaryDto d2 = out.getContent().get(1);
        assertEquals("accepted", d2.status());
        //assertEquals("Stage 2", d2.stageLabel());
        assertNotNull(d2.lastUpdatedAt());
    }

    @Test
    void listPendingOrStuckProposals_shouldHandleNullStatusAndNullFilters() {
        Pageable pageable = PageRequest.of(0, 5);

        Proposal p = mock(Proposal.class);
        lenient().when(p.getIdeaStatus()).thenReturn(null); // service should map to null status string
        Instant inst = Instant.parse("2026-01-03T12:00:00Z");
        LocalDateTime ldt = LocalDateTime.ofInstant(inst, ZoneOffset.UTC);
        lenient().when(p.getUpdatedAt()).thenReturn(ldt);
        when(proposalRepository.searchAdminList(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));

        Page<AdminProposalSummaryDto> out = service.listPendingOrStuckProposals(null, null, null, pageable);

        assertEquals(1, out.getTotalElements());
        AdminProposalSummaryDto dto = out.getContent().get(0);
        assertNull(dto.status());     // because p.getIdeaStatus() was null
        assertNotNull(dto.lastUpdatedAt());
        assertNotNull(dto.lastUpdatedAt());
    }

    @Test
    void listPendingOrStuckProposals_shouldRejectInvalidStatusString() {
        Pageable pageable = PageRequest.of(0, 5);
        String invalid = "not_a_valid_status";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.listPendingOrStuckProposals(invalid, null, null, pageable));
        assertTrue(ex.getMessage().contains("Invalid status: " + invalid));
        verify(proposalRepository, never()).searchAdminList(any(), any(), any(), any());
    }

    // ----------------------------- getOverdueReviewers -----------------------------
    @Test
    void getOverdueReviewers_shouldMapProjectionToDto() {
        Pageable pageable = PageRequest.of(0, 10);

        ReviewerAggregateProjection agg1 = mock(ReviewerAggregateProjection.class);
        when(agg1.getReviewerId()).thenReturn((long) 11); // Integer is safer for mapping to int
        when(agg1.getPendingTasks()).thenReturn(3);
        when(agg1.getOverdueByDays()).thenReturn(2);

        ReviewerAggregateProjection agg2 = mock(ReviewerAggregateProjection.class);
        when(agg2.getReviewerId()).thenReturn((long) 12);
        when(agg2.getPendingTasks()).thenReturn(null); // safeInt -> 0
        when(agg2.getOverdueByDays()).thenReturn(5);

        Page<ReviewerAggregateProjection> page = new PageImpl<>(List.of(agg1, agg2), pageable, 2);

        when(assignmentRepository.findOverdueReviewerAggregates(pageable)).thenReturn(page);

        Page<AdminOverdueReviewerDto> out = service.getOverdueReviewers(pageable);

        assertEquals(2, out.getTotalElements());

        AdminOverdueReviewerDto d1 = out.getContent().get(0);
        assertEquals(11L, d1.reviewerId());
        assertEquals(3, d1.pendingTasks());
        assertEquals(2, d1.overdueByDays());

        AdminOverdueReviewerDto d2 = out.getContent().get(1);
        assertEquals(12L, d2.reviewerId());
        assertEquals(0, d2.pendingTasks()); // null -> 0
        assertEquals(5, d2.overdueByDays());
    }

    // ----------------------------- getHealthSummary -----------------------------

    @Test
    void getHealthSummary_shouldReturnStaticFieldsAndCounts() {
        when(proposalRepository.count()).thenReturn(257L);

        AdminHealthSummaryDto dto = service.getHealthSummary();

        assertNotNull(dto);
        assertEquals("healthy", dto.database());
        assertEquals(0, dto.queueBacklog());
        assertEquals("99.99%", dto.serviceUptime());
        assertEquals(257 % 100, dto.pendingJobs()); // proposals % 100
        assertEquals("1.0.0", dto.version());
        assertNotNull(dto.timestamp());
        
        verify(proposalRepository).count();
    }

    // ----------------------------- updateUserStatus -----------------------------
    @Test
    void updateUserStatus_shouldActivateUser() {
        Integer userId = 9001;

        com.ideatrack.main.data.User user = mock(com.ideatrack.main.data.User.class);
        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));

        AdminUpdateUserStatusRequest req = AdminUpdateUserStatusRequest.builder()
                .status(Status.ACTIVE)
                .build();

        service.updateUserStatus(userId, req);

        verify(user).setStatus(Status.ACTIVE);
        verify(user).setDeleted(false);
        verify(userRepository).save(user);
    }


    @Test
    void updateUserStatus_shouldActivateUser_withMockedRequest() {
        Integer userId = 9001;

        com.ideatrack.main.data.User user = mock(com.ideatrack.main.data.User.class);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(user));

        AdminUpdateUserStatusRequest req = mock(AdminUpdateUserStatusRequest.class);
        when(req.getStatus()).thenReturn(Status.ACTIVE); // ✅ domain enum now

        service.updateUserStatus(userId, req);

        verify(user).setStatus(Status.ACTIVE);
        verify(user).setDeleted(false);
        verify(userRepository).save(user);
    }

    @Nested
    class AdminService_UserNotFoundTest {

        @Mock IProposalRepository proposalRepository;
        @Mock IReviewerCategoryRepository assignmentRepository;
        @Mock IUserRepository userRepository;
        @Mock IAssignedReviewerToIdeaRepository assignedReviewerToIdeaRepository;
        @Mock IIdeaRepository ideaRepository;

        @InjectMocks
        private AdminDashboardService service;

        @Test
        void updateUserStatus_shouldThrow_whenUserNotFound() {
            // Arrange
            Integer userId = 404;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            AdminUpdateUserStatusRequest req = mock(AdminUpdateUserStatusRequest.class);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.updateUserStatus(userId, req) // ✅ real arrow
            );

            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("User not found: " + userId));

            // Verify the repository was queried, and save was not called
            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any(com.ideatrack.main.data.User.class));

            // No accidental calls to other repos
            verifyNoInteractions(proposalRepository, assignmentRepository, assignedReviewerToIdeaRepository, ideaRepository);
        }

    }

    // ----------------------------- removeReviewersFromStage -----------------------------

    @Test
    void removeReviewersFromStage_shouldReturnEarly_onNullOrEmptyList() {
        service.removeReviewersFromStage(1L, 1L, null);
        service.removeReviewersFromStage(1L, 1L, List.of());

        verifyNoInteractions(ideaRepository, assignedReviewerToIdeaRepository);
    }

    @Test
    void removeReviewersFromStage_shouldConvertIds_andCallSoftDelete() {
        Long ideaIdLong = 77L;
        Long stageIdLong = 3L;
        List<Long> reviewerIds = List.of(10L, 11L, 12L);

        // orElseThrow() — ensure present
        when(ideaRepository.findById(Math.toIntExact(ideaIdLong))).thenReturn(Optional.of(mock(com.ideatrack.main.data.Idea.class)));
        when(assignedReviewerToIdeaRepository.softDeleteAssignments(anyInt(), anyInt(), any(), any())).thenReturn(3);

        // We don't strictly assert the returned 'updated' count; just verify invocation args
        service.removeReviewersFromStage(ideaIdLong, stageIdLong, reviewerIds);

        ArgumentCaptor<Integer> ideaIdCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> stageCap = ArgumentCaptor.forClass(Integer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> reviewersCap = ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<OffsetDateTime> tsCap = ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(assignedReviewerToIdeaRepository).softDeleteAssignments(
                ideaIdCap.capture(), stageCap.capture(), reviewersCap.capture(), tsCap.capture()
        );

        assertEquals(77, ideaIdCap.getValue());
        assertEquals(3, stageCap.getValue());
        assertEquals(List.of(10, 11, 12), reviewersCap.getValue());
        assertNotNull(tsCap.getValue());
    }

    @Test
    void removeReviewersFromStage_shouldThrow_whenIdeaNotFound() {
        when(ideaRepository.findById(123)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> service.removeReviewersFromStage(123L, 1L, List.of(9L)));
        verify(assignedReviewerToIdeaRepository, never()).softDeleteAssignments(any(), any(), any(), any());
    }
}
