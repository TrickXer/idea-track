package com.ideatrack.main.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.Objectives;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.objective.ObjectiveCreation;
import com.ideatrack.main.dto.objective.ProofForObjectiveDTO;
import com.ideatrack.main.dto.proposal.ProposalCreateRequestDTO;
import com.ideatrack.main.dto.proposal.ProposalResponseDTO;
import com.ideatrack.main.dto.proposal.ProposalSubmitRequest;
import com.ideatrack.main.dto.proposal.ProposalUpdateRequestDTO;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IObjectivesRepository;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.web.server.ResponseStatusException;

// ==== Custom exceptions thrown by ProposalService ====
import com.ideatrack.main.exception.BudgetNegativeException;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.exception.IdeaStatusNotAcceptedException;
import com.ideatrack.main.exception.ObjectiveDescriptionInvalidException;
import com.ideatrack.main.exception.ObjectiveProofContentTypeNotSupportedException;
import com.ideatrack.main.exception.ObjectiveProofMissingException;
import com.ideatrack.main.exception.ObjectiveProofPathEmptyException;
import com.ideatrack.main.exception.ObjectiveProofSizeInvalidException;
import com.ideatrack.main.exception.ObjectiveSeqDuplicateException;
import com.ideatrack.main.exception.ObjectiveSeqInvalidException;
import com.ideatrack.main.exception.ObjectiveTitleInvalidException;
import com.ideatrack.main.exception.OnlyOneMandatoryObjectiveAllowedException;
import com.ideatrack.main.exception.ProposalAlreadyExistsException;
import com.ideatrack.main.exception.ProposalDeleteOnlyWhenDraftException;
import com.ideatrack.main.exception.ProposalDeletedException;
import com.ideatrack.main.exception.ProposalNotFoundException;
import com.ideatrack.main.exception.ProposalSubmitOnlyWhenDraftException;
import com.ideatrack.main.exception.SubmitTimelineMissingException;
import com.ideatrack.main.exception.TimelineInvalidException;
import com.ideatrack.main.exception.UserIdRequiredException;
import com.ideatrack.main.exception.UserNotFoundException;

//Done by Vibhuti //Tested 100%//

public class TestProposalService {

    private IProposalRepository proposalRepo;
    private IIdeaRepository ideaRepo;
    private IUserRepository userRepo;
    private IObjectivesRepository objectivesRepo;
    private ProposalMapper mapper;
    private NotificationHelper notificationHelper;

    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepo = mock(IProposalRepository.class);
        ideaRepo = mock(IIdeaRepository.class);
        userRepo = mock(IUserRepository.class);
        objectivesRepo = mock(IObjectivesRepository.class);
        mapper = mock(ProposalMapper.class);
        notificationHelper = mock(NotificationHelper.class);

        service = new ProposalService(proposalRepo, ideaRepo, userRepo, objectivesRepo, mapper, notificationHelper);
    }

    // 1) convertIdeaToProposal
    @Test
    void convertIdeaToProposal_shouldCreateProposal_whenIdeaAccepted_NoObjectives() {
        Integer ideaId = 1, userId = 10;

        Idea acceptedIdea = new Idea();
        acceptedIdea.setIdeaId(ideaId);
        acceptedIdea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);

        User user = new User();
        user.setUserId(userId);

        when(ideaRepo.findById(ideaId)).thenReturn(Optional.of(acceptedIdea));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(ideaId)).thenReturn(false);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        // Make save(...) assign an ID and return the same instance
        when(proposalRepo.save(ArgumentMatchers.any(Proposal.class)))
                .thenAnswer(inv -> {
                    Proposal arg = inv.getArgument(0);
                    arg.setProposalId(99);
                    return arg;
                });

        ProposalResponseDTO dto = mock(ProposalResponseDTO.class);
        when(mapper.toResponse(any(Proposal.class))).thenReturn(dto);

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(userId);
        when(req.getObjectives()).thenReturn(null);

        ProposalResponseDTO result = service.convertIdeaToProposal(ideaId, req);
        assertSame(dto, result);

        // Verify persisted Proposal
        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepo).save(captor.capture());
        Proposal persisted = captor.getValue();
        assertEquals(acceptedIdea, persisted.getIdea());
        assertEquals(user, persisted.getUser());
        assertEquals(0L, persisted.getBudget());
        assertNull(persisted.getTimeLineStart());
        assertNull(persisted.getTimeLineEnd());
        // ✅ Service creates proposals as DRAFT
        assertEquals(Constants.IdeaStatus.DRAFT, persisted.getIdeaStatus());
        assertFalse(Boolean.TRUE.equals(persisted.isDeleted()));
    }

    @Test
    void convertIdeaToProposal_shouldSeedObjectives_whenValidRequestObjectives() {
        Integer ideaId = 2, userId = 20;

        Idea acceptedIdea = new Idea();
        acceptedIdea.setIdeaId(ideaId);
        acceptedIdea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);

        User user = new User();
        user.setUserId(userId);

        when(ideaRepo.findById(ideaId)).thenReturn(Optional.of(acceptedIdea));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(ideaId)).thenReturn(false);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        // proposal save returns same instance
        when(proposalRepo.save(ArgumentMatchers.any(Proposal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Prepare request objective with VALID proof
        ObjectiveCreation o1 = mock(ObjectiveCreation.class);
        when(o1.getObjectiveSeq()).thenReturn(1);
        when(o1.getTitle()).thenReturn("Title 1");
        when(o1.getDescription()).thenReturn("Desc 1");
        when(o1.getMandatory()).thenReturn(Boolean.TRUE);

        ProofForObjectiveDTO proof = mock(ProofForObjectiveDTO.class); // <-- Use your actual class name
        when(proof.getFilePath()).thenReturn("/tmp/proof.pdf");
        when(proof.getContentType()).thenReturn("application/pdf");
        when(proof.getSizeBytes()).thenReturn(2048L);
        when(proof.getFileName()).thenReturn("proof.pdf");
        when(o1.getProof()).thenReturn(proof);

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(userId);
        when(req.getObjectives()).thenReturn(List.of(o1));

        when(objectivesRepo.saveAll(ArgumentMatchers.<List<Objectives>>any()))
                .thenAnswer(inv -> inv.getArgument(0));

        ProposalResponseDTO dto = mock(ProposalResponseDTO.class);
        when(mapper.toResponse(any(Proposal.class))).thenReturn(dto);

        ProposalResponseDTO out = service.convertIdeaToProposal(ideaId, req);
        assertSame(dto, out);

        // Verify objectives mapping persisted
        ArgumentCaptor<List<Objectives>> objCaptor = ArgumentCaptor.forClass(List.class);
        verify(objectivesRepo).saveAll(objCaptor.capture());
        List<Objectives> savedObjs = objCaptor.getValue();
        assertEquals(1, savedObjs.size());
        Objectives oSaved = savedObjs.get(0);
        assertEquals(1, oSaved.getObjectiveSeq());
        assertEquals("Title 1", oSaved.getTitle());
        assertEquals("Desc 1", oSaved.getDescription());
        assertTrue(oSaved.isMandatory());
        assertEquals("application/pdf", oSaved.getProofContentType());
        assertEquals("proof.pdf", oSaved.getProofFileName());
        assertEquals("/tmp/proof.pdf", oSaved.getProofFilePath());
        assertEquals(2048L, oSaved.getProofSizeBytes());
    }

    @Test
    void convertIdeaToProposal_shouldFail_whenIdeaNotFound() {
        when(ideaRepo.findById(111)).thenReturn(Optional.empty());

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(1);

        assertThrows(IdeaNotFound.class, () -> service.convertIdeaToProposal(111, req));
    }

    @Test
    void convertIdeaToProposal_shouldFail_whenIdeaNotAccepted() {
        Idea idea = new Idea();
        idea.setIdeaId(1);
        idea.setIdeaStatus(Constants.IdeaStatus.REJECTED);
        when(ideaRepo.findById(1)).thenReturn(Optional.of(idea));

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(2);

        assertThrows(IdeaStatusNotAcceptedException.class,
                () -> service.convertIdeaToProposal(1, req));
    }

    @Test
    void convertIdeaToProposal_shouldFail_whenProposalAlreadyExists() {
        Idea idea = new Idea();
        idea.setIdeaId(5);
        idea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);

        when(ideaRepo.findById(5)).thenReturn(Optional.of(idea));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(5)).thenReturn(true);

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(100);

        assertThrows(ProposalAlreadyExistsException.class,
                () -> service.convertIdeaToProposal(5, req));
    }

    @Test
    void convertIdeaToProposal_shouldFail_whenUserIdMissing() {
        Idea idea = new Idea();
        idea.setIdeaId(12);
        idea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);
        when(ideaRepo.findById(12)).thenReturn(Optional.of(idea));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(12)).thenReturn(false);

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(null);

        assertThrows(UserIdRequiredException.class,
                () -> service.convertIdeaToProposal(12, req));
    }

    @Test
    void convertIdeaToProposal_shouldFail_whenUserNotFound() {
        Integer ideaId = 15;
        Idea idea = new Idea();
        idea.setIdeaId(ideaId);
        idea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);
        when(ideaRepo.findById(ideaId)).thenReturn(Optional.of(idea));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(ideaId)).thenReturn(false);

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(999);
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.convertIdeaToProposal(ideaId, req));
    }

    @Test
    void convertIdeaToProposal_shouldFail_onObjectiveValidation_duplicateSeq_orMultipleMandatory() {
        Integer ideaId = 30, userId = 300;
        Idea accepted = new Idea();
        accepted.setIdeaId(ideaId);
        accepted.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);
        when(ideaRepo.findById(ideaId)).thenReturn(Optional.of(accepted));
        when(userRepo.findById(userId)).thenReturn(Optional.of(new User()));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(ideaId)).thenReturn(false);
        when(proposalRepo.save(ArgumentMatchers.any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        // Two objectives with same seq => should fail with ObjectiveSeqDuplicateException
        ObjectiveCreation o1 = mock(ObjectiveCreation.class);
        when(o1.getObjectiveSeq()).thenReturn(1);
        when(o1.getTitle()).thenReturn("A");
        when(o1.getDescription()).thenReturn("a");
        when(o1.getMandatory()).thenReturn(Boolean.TRUE);

        ObjectiveCreation o2 = mock(ObjectiveCreation.class);
        when(o2.getObjectiveSeq()).thenReturn(1); // duplicate seq
        when(o2.getTitle()).thenReturn("B");
        when(o2.getDescription()).thenReturn("b");
        when(o2.getMandatory()).thenReturn(Boolean.TRUE); // also multiple mandatory

        ProposalCreateRequestDTO req = mock(ProposalCreateRequestDTO.class);
        when(req.getUserId()).thenReturn(userId);
        when(req.getObjectives()).thenReturn(List.of(o1, o2));

        assertThrows(ObjectiveSeqDuplicateException.class,
                () -> service.convertIdeaToProposal(ideaId, req));
    }

    @Test
    void convertIdeaToProposal_shouldFail_onObjectiveProofMissing_orInvalid() {
        Integer ideaId = 40, userId = 400;
        Idea accepted = new Idea();
        accepted.setIdeaId(ideaId);
        accepted.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);
        when(ideaRepo.findById(ideaId)).thenReturn(Optional.of(accepted));
        when(userRepo.findById(userId)).thenReturn(Optional.of(new User()));
        when(proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(ideaId)).thenReturn(false);
        when(proposalRepo.save(ArgumentMatchers.any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        // Missing proof
        ObjectiveCreation o1 = mock(ObjectiveCreation.class);
        when(o1.getObjectiveSeq()).thenReturn(1);
        when(o1.getTitle()).thenReturn("A");
        when(o1.getDescription()).thenReturn("a");
        when(o1.getMandatory()).thenReturn(Boolean.FALSE);
        when(o1.getProof()).thenReturn(null);

        ProposalCreateRequestDTO req1 = mock(ProposalCreateRequestDTO.class);
        when(req1.getUserId()).thenReturn(userId);
        when(req1.getObjectives()).thenReturn(List.of(o1));

        assertThrows(ObjectiveProofMissingException.class,
                () -> service.convertIdeaToProposal(ideaId, req1));

        // Invalid content type
        ObjectiveCreation o2 = mock(ObjectiveCreation.class);
        when(o2.getObjectiveSeq()).thenReturn(2);
        when(o2.getTitle()).thenReturn("B");
        when(o2.getDescription()).thenReturn("b");
        when(o2.getMandatory()).thenReturn(Boolean.FALSE);
        ProofForObjectiveDTO badCt = mock(ProofForObjectiveDTO.class);
        when(badCt.getFilePath()).thenReturn("/tmp/p.png");
        when(badCt.getContentType()).thenReturn("image/png"); // not allowed
        when(badCt.getSizeBytes()).thenReturn(1L);
        when(badCt.getFileName()).thenReturn("p.png");
        when(o2.getProof()).thenReturn(badCt);

        ProposalCreateRequestDTO req2 = mock(ProposalCreateRequestDTO.class);
        when(req2.getUserId()).thenReturn(userId);
        when(req2.getObjectives()).thenReturn(List.of(o2));

        assertThrows(ObjectiveProofContentTypeNotSupportedException.class,
                () -> service.convertIdeaToProposal(ideaId, req2));

        // Size invalid
        ObjectiveCreation o3 = mock(ObjectiveCreation.class);
        when(o3.getObjectiveSeq()).thenReturn(3);
        when(o3.getTitle()).thenReturn("C");
        when(o3.getDescription()).thenReturn("c");
        when(o3.getMandatory()).thenReturn(Boolean.FALSE);
        ProofForObjectiveDTO badSize = mock(ProofForObjectiveDTO.class);
        when(badSize.getFilePath()).thenReturn("/tmp/p.pdf");
        when(badSize.getContentType()).thenReturn("application/pdf");
        when(badSize.getSizeBytes()).thenReturn(0L); // invalid
        when(badSize.getFileName()).thenReturn("p.pdf");
        when(o3.getProof()).thenReturn(badSize);

        ProposalCreateRequestDTO req3 = mock(ProposalCreateRequestDTO.class);
        when(req3.getUserId()).thenReturn(userId);
        when(req3.getObjectives()).thenReturn(List.of(o3));

        assertThrows(ObjectiveProofSizeInvalidException.class,
                () -> service.convertIdeaToProposal(ideaId, req3));

        // Proof path empty
        ObjectiveCreation o4 = mock(ObjectiveCreation.class);
        when(o4.getObjectiveSeq()).thenReturn(4);
        when(o4.getTitle()).thenReturn("D");
        when(o4.getDescription()).thenReturn("d");
        when(o4.getMandatory()).thenReturn(Boolean.FALSE);
        ProofForObjectiveDTO emptyPath = mock(ProofForObjectiveDTO.class);
        when(emptyPath.getFilePath()).thenReturn("  "); // blank
        when(emptyPath.getContentType()).thenReturn("application/pdf");
        when(emptyPath.getSizeBytes()).thenReturn(1L);
        when(emptyPath.getFileName()).thenReturn("p.pdf");
        when(o4.getProof()).thenReturn(emptyPath);

        ProposalCreateRequestDTO req4 = mock(ProposalCreateRequestDTO.class);
        when(req4.getUserId()).thenReturn(userId);
        when(req4.getObjectives()).thenReturn(List.of(o4));

        assertThrows(ObjectiveProofPathEmptyException.class,
                () -> service.convertIdeaToProposal(ideaId, req4));
    }

    // 2) updateDraft
    @Test
    void updateDraft_shouldApplyFields_whenValid_andForceStatusToDraft() {
        // NOTE: updateDraft forces status to DRAFT even if not DRAFT
        Proposal p = new Proposal();
        p.setProposalId(77);
        p.setIdeaStatus(Constants.IdeaStatus.APPROVED); // will be forced to DRAFT
        p.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(77)).thenReturn(Optional.of(p));
        when(proposalRepo.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        ProposalUpdateRequestDTO req = mock(ProposalUpdateRequestDTO.class);
        when(req.getBudget()).thenReturn(1000L);
        when(req.getTimeLineStart()).thenReturn(LocalDate.of(2026, 1, 1));
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 3, 1));

        ProposalResponseDTO resp = mock(ProposalResponseDTO.class);
        when(mapper.toResponse(p)).thenReturn(resp);

        ProposalResponseDTO out = service.updateDraft(77, req);

        assertSame(resp, out);
        assertEquals(1000L, p.getBudget());
        assertEquals(LocalDate.of(2026, 1, 1), p.getTimeLineStart());
        assertEquals(LocalDate.of(2026, 3, 1), p.getTimeLineEnd());
        assertEquals(Constants.IdeaStatus.DRAFT, p.getIdeaStatus()); // forced
    }

    @Test
    void updateDraft_shouldFail_whenBudgetNegative() {
        Proposal draft = new Proposal();
        draft.setProposalId(1);
        draft.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        draft.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(1)).thenReturn(Optional.of(draft));

        ProposalUpdateRequestDTO req = mock(ProposalUpdateRequestDTO.class);
        when(req.getBudget()).thenReturn(-1L);

        assertThrows(BudgetNegativeException.class, () -> service.updateDraft(1, req));
    }

    @Test
    void updateDraft_shouldFail_whenTimelineStartAfterEnd() {
        Proposal draft = new Proposal();
        draft.setProposalId(1);
        draft.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        draft.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(1)).thenReturn(Optional.of(draft));

        ProposalUpdateRequestDTO req = mock(ProposalUpdateRequestDTO.class);
        when(req.getTimeLineStart()).thenReturn(LocalDate.of(2026, 5, 1));
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 4, 1));

        assertThrows(TimelineInvalidException.class, () -> service.updateDraft(1, req));
    }

    @Test
    void updateDraft_shouldFail_whenProposalDeleted() {
        Proposal p = new Proposal();
        p.setProposalId(20);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        p.setDeleted(true); // deleted
        when(proposalRepo.findByIdWithObjectives(20)).thenReturn(Optional.of(p));

        ProposalUpdateRequestDTO req = mock(ProposalUpdateRequestDTO.class);
        assertThrows(ProposalDeletedException.class, () -> service.updateDraft(20, req));
    }

    @Test
    void updateDraft_shouldFail_whenProposalNotFound() {
        when(proposalRepo.findByIdWithObjectives(404)).thenReturn(Optional.empty());
        assertThrows(ProposalNotFoundException.class, () -> service.updateDraft(404, mock(ProposalUpdateRequestDTO.class)));
    }

    // 3) deleteDraft
    @Test
    void deleteDraft_shouldMarkDeleted_whenDraft() {
        Proposal draft = new Proposal();
        draft.setProposalId(9);
        draft.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        draft.setDeleted(false);

        when(proposalRepo.findByProposalIdAndDeletedFalse(9)).thenReturn(Optional.of(draft));

        service.deleteDraft(9);

        assertTrue(Boolean.TRUE.equals(draft.isDeleted()));
    }

    @Test
    void deleteDraft_shouldFail_whenNotDraft() {
        Proposal p = new Proposal();
        p.setProposalId(9);
        p.setIdeaStatus(Constants.IdeaStatus.APPROVED);
        p.setDeleted(false);

        when(proposalRepo.findByProposalIdAndDeletedFalse(9)).thenReturn(Optional.of(p));

        assertThrows(ProposalDeleteOnlyWhenDraftException.class, () -> service.deleteDraft(9));
    }

    @Test
    void deleteDraft_shouldFail_whenNotFound() {
        when(proposalRepo.findByProposalIdAndDeletedFalse(1)).thenReturn(Optional.empty());
        assertThrows(ProposalNotFoundException.class, () -> service.deleteDraft(1));
    }

    // 4) OBJECTIVES: add / update
    @Test
    void addObjective_shouldPersistAndAttachToProposal_whenValid() {
        Proposal p = draftForUpdateLock(100);
        p.setObjectives(new ArrayList<>());

        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(100, 1)).thenReturn(false);
        when(objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(100)).thenReturn(0L);

        when(objectivesRepo.save(ArgumentMatchers.any(Objectives.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Objectives out = service.addObjective(100, 1, "T", "D", true);

        assertNotNull(out);
        assertEquals(1, out.getObjectiveSeq());
        assertEquals("T", out.getTitle());
        assertEquals("D", out.getDescription());
        assertTrue(out.isMandatory());

        assertEquals(1, p.getObjectives().size());
        assertSame(out, p.getObjectives().get(0));
    }

    @Test
    void addObjective_shouldFail_whenSeqAlreadyExists() {
        draftForUpdateLock(100);
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(100, 1)).thenReturn(true);
        assertThrows(ObjectiveSeqDuplicateException.class,
                () -> service.addObjective(100, 1, "T", "D", false));
    }

    @Test
    void addObjective_shouldFail_whenInvalidParams() {
        draftForUpdateLock(100);
        // seq invalid
        assertThrows(ObjectiveSeqInvalidException.class,
                () -> service.addObjective(100, 0, "T", "D", false));
        // title invalid
        assertThrows(ObjectiveTitleInvalidException.class,
                () -> service.addObjective(100, 1, "   ", "D", false));
        // description invalid
        assertThrows(ObjectiveDescriptionInvalidException.class,
                () -> service.addObjective(100, 1, "T", "  ", false));
    }

    @Test
    void addObjective_shouldFail_whenAnotherMandatoryExists() {
        draftForUpdateLock(100);
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(100, 1)).thenReturn(false);
        when(objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(100)).thenReturn(1L);
        assertThrows(OnlyOneMandatoryObjectiveAllowedException.class,
                () -> service.addObjective(100, 1, "T", "D", true));
    }

    @Test
    void addObjective_shouldFail_whenProposalNotFoundOrNotDraft() {
        // mustGetActiveDraftLocked -> 404 if not found
        when(proposalRepo.findForUpdate(999)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addObjective(999, 1, "T", "D", false));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateObjective_shouldApplyChanges_whenValid() {
        Proposal p = draftForUpdateLock(200);

        Objectives existing = new Objectives();
        existing.setId(900L);
        existing.setObjectiveSeq(1);
        existing.setTitle("Old");
        existing.setDescription("OldD");
        existing.setMandatory(false);

        when(objectivesRepo.findByIdAndProposal_ProposalId(900L, 200))
                .thenReturn(Optional.of(existing));
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(200, 2)).thenReturn(false);
        when(objectivesRepo.save(ArgumentMatchers.any(Objectives.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Objectives out = service.updateObjective(200, 900L, 2, "New", "NewD", true);

        assertSame(existing, out);
        assertEquals(2, existing.getObjectiveSeq());
        assertEquals("New", existing.getTitle());
        assertEquals("NewD", existing.getDescription());
        assertTrue(existing.isMandatory());
    }

    @Test
    void updateObjective_shouldFail_whenObjectiveNotFound() {
        draftForUpdateLock(200);
        when(objectivesRepo.findByIdAndProposal_ProposalId(900L, 200))
                .thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateObjective(200, 900L, 2, null, null, null));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateObjective_shouldFail_whenSeqTakenByAnother() {
        draftForUpdateLock(200);

        Objectives existing = new Objectives();
        existing.setId(900L);
        existing.setObjectiveSeq(1);

        when(objectivesRepo.findByIdAndProposal_ProposalId(900L, 200))
                .thenReturn(Optional.of(existing));
        // simulate seqTaken (exists true and new seq differs from current)
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(200, 2)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateObjective(200, 900L, 2, null, null, null));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void updateObjective_shouldFail_whenInvalidFields() {
        draftForUpdateLock(200);
        Objectives existing = new Objectives();
        existing.setId(1L);
        existing.setObjectiveSeq(5);
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, 200))
                .thenReturn(Optional.of(existing));

        // seq < 1
        ResponseStatusException badSeq = assertThrows(ResponseStatusException.class,
                () -> service.updateObjective(200, 1L, 0, null, null, null));
        assertEquals(400, badSeq.getStatusCode().value());

        // title blank
        ResponseStatusException badTitle = assertThrows(ResponseStatusException.class,
                () -> service.updateObjective(200, 1L, null, "   ", null, null));
        assertEquals(400, badTitle.getStatusCode().value());

        // description blank
        ResponseStatusException badDesc = assertThrows(ResponseStatusException.class,
                () -> service.updateObjective(200, 1L, null, null, " ", null));
        assertEquals(400, badDesc.getStatusCode().value());
    }

    @Test
    void updateObjective_shouldFail_whenSettingMandatoryButAnotherExists() {
        draftForUpdateLock(200);

        Objectives existing = new Objectives();
        existing.setId(1L);
        existing.setObjectiveSeq(1);
        existing.setMandatory(false); // currently not mandatory

        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, 200))
                .thenReturn(Optional.of(existing));
        when(objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(200)).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateObjective(200, 1L, null, null, null, true));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().toLowerCase().contains("only one objective"));
    }

    // 5) submit (Draft -> PROJECTPROPOSAL)
    @Test
    void submit_shouldMoveToProjectProposal_whenValid() {
        Proposal p = new Proposal(); // used below via proposalRepo mock
        p.setProposalId(500);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        p.setDeleted(false);

        when(proposalRepo.findByIdWithObjectives(500)).thenReturn(Optional.of(p));
        when(proposalRepo.save(ArgumentMatchers.any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProposalSubmitRequest req = mock(ProposalSubmitRequest.class);
        when(req.getTimeLineStart()).thenReturn(LocalDate.of(2026, 2, 1));
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 3, 1));
        when(req.getBudget()).thenReturn(1000L);

        ProposalResponseDTO dto = mock(ProposalResponseDTO.class);
        when(mapper.toResponse(p)).thenReturn(dto);

        ProposalResponseDTO out = service.submit(500, req);

        assertSame(dto, out);
        assertEquals(Constants.IdeaStatus.PROJECTPROPOSAL, p.getIdeaStatus());
        assertEquals(1000L, p.getBudget());
        assertEquals(LocalDate.of(2026, 2, 1), p.getTimeLineStart());
        assertEquals(LocalDate.of(2026, 3, 1), p.getTimeLineEnd());
        verify(proposalRepo).save(p);
    }

    @Test
    void submit_shouldFail_whenNotDraft() {
        Proposal p = new Proposal();
        p.setProposalId(2);
        p.setIdeaStatus(Constants.IdeaStatus.APPROVED);
        p.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(2)).thenReturn(Optional.of(p));

        ProposalSubmitRequest req = mock(ProposalSubmitRequest.class);
        when(req.getTimeLineStart()).thenReturn(LocalDate.of(2026, 1, 1));
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 2, 1));
        when(req.getBudget()).thenReturn(100L);

        assertThrows(ProposalSubmitOnlyWhenDraftException.class,
                () -> service.submit(2, req));
    }

    @Test
    void submit_shouldFail_whenTimelineMissing() {
        Proposal p = new Proposal();
        p.setProposalId(3);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        p.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(3)).thenReturn(Optional.of(p));

        ProposalSubmitRequest req = mock(ProposalSubmitRequest.class);
        when(req.getTimeLineStart()).thenReturn(null);
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 2, 1));
        when(req.getBudget()).thenReturn(100L);

        assertThrows(SubmitTimelineMissingException.class,
                () -> service.submit(3, req));
    }

    @Test
    void submit_shouldFail_whenTimelineInvalid() {
        Proposal p = new Proposal();
        p.setProposalId(3);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        p.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(3)).thenReturn(Optional.of(p));

        ProposalSubmitRequest req = mock(ProposalSubmitRequest.class);
        when(req.getTimeLineStart()).thenReturn(LocalDate.of(2026, 5, 1));
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 4, 1));
        when(req.getBudget()).thenReturn(100L);

        assertThrows(TimelineInvalidException.class,
                () -> service.submit(3, req));
    }

    @Test
    void submit_shouldFail_whenBudgetNegative() {
        Proposal p = new Proposal();
        p.setProposalId(3);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        p.setDeleted(false);
        when(proposalRepo.findByIdWithObjectives(3)).thenReturn(Optional.of(p));

        ProposalSubmitRequest req = mock(ProposalSubmitRequest.class);
        when(req.getTimeLineStart()).thenReturn(LocalDate.of(2026, 1, 1));
        when(req.getTimeLineEnd()).thenReturn(LocalDate.of(2026, 2, 1));
        when(req.getBudget()).thenReturn(-1L);

        assertThrows(BudgetNegativeException.class,
                () -> service.submit(3, req));
    }

    @Test
    void submit_shouldFail_whenProposalNotFound() {
        when(proposalRepo.findByIdWithObjectives(404)).thenReturn(Optional.empty());
        assertThrows(ProposalNotFoundException.class,
                () -> service.submit(404, mock(ProposalSubmitRequest.class)));
    }

    // Helpers
    // Creates a DRAFT, not-deleted proposal returned by findForUpdate(...) to satisfy mustGetActiveDraftLocked(...) */
    private Proposal draftForUpdateLock(int proposalId) {
        Proposal p = new Proposal();
        p.setProposalId(proposalId);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        p.setDeleted(false);
        when(proposalRepo.findForUpdate(proposalId)).thenReturn(Optional.of(p));
        return p;
    }
}
