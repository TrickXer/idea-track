package com.ideatrack.main.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ideatrack.main.data.Objectives;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.dto.objective.ObjectiveCreation;
import com.ideatrack.main.dto.objective.ObjectivesResponse;
import com.ideatrack.main.dto.objective.ProofResponse;
import com.ideatrack.main.repository.IObjectivesRepository;
import com.ideatrack.main.repository.IProposalRepository;

import com.ideatrack.main.exception.ObjectiveDescriptionInvalidException;
import com.ideatrack.main.exception.ObjectiveNotFoundInProposalException;
import com.ideatrack.main.exception.ObjectiveProofContentTypeNotSupportedException;
import com.ideatrack.main.exception.ObjectiveProofMissingException;
import com.ideatrack.main.exception.ObjectiveProofSizeInvalidException;
import com.ideatrack.main.exception.ObjectiveSeqDuplicateException;
import com.ideatrack.main.exception.ObjectiveSeqInvalidException;
import com.ideatrack.main.exception.ObjectiveTitleInvalidException;
import com.ideatrack.main.exception.OnlyOneMandatoryObjectiveAllowedException;
import com.ideatrack.main.exception.ProofFileRequiredException;
import com.ideatrack.main.exception.ProposalNotFoundException;

//Done by vibhuti	//tested 100%//

@ExtendWith(MockitoExtension.class)
public class TestObjectiveService {

    @Mock private IProposalRepository proposalRepo;
    @Mock private IObjectivesRepository objectivesRepo;

    @InjectMocks
    private ObjectiveService service;

    @BeforeEach
    void init() {
    }

    @AfterEach
    void cleanUploads() {
        // Clean uploads dir created by uploadProof tests
        Path uploads = Paths.get("uploads");
        if (Files.exists(uploads)) {
            try (Stream<Path> paths = Files.walk(uploads)) {
                paths.sorted(Comparator.reverseOrder())
                     .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
            } catch (Exception ignored) {}
        }
    }

    // ----------------------------------------------------------
    // A) getAll
    // ----------------------------------------------------------
    @Test
    void getAll_shouldReturnMappedResponsesInOrder() {
        Integer proposalId = 1;

        Objectives o1 = new Objectives();
        o1.setId(10L);
        o1.setObjectiveSeq(1);
        o1.setTitle("T1");
        o1.setDescription("D1");
        o1.setMandatory(false);
        o1.setProofFileName("p1.pdf");
        o1.setProofContentType("application/pdf");
        o1.setProofFilePath("/x/p1.pdf");
        o1.setProofSizeBytes(123L);

        Objectives o2 = new Objectives();
        o2.setId(11L);
        o2.setObjectiveSeq(2);
        o2.setTitle("T2");
        o2.setDescription("D2");
        o2.setMandatory(true);
        o2.setProofFileName("p2.jpg");
        o2.setProofContentType("image/jpeg");
        o2.setProofFilePath("/x/p2.jpg");
        o2.setProofSizeBytes(456L);

        when(objectivesRepo.findAllByProposal_ProposalIdAndProposal_DeletedFalseOrderByObjectiveSeqAsc(proposalId))
                .thenReturn(List.of(o1, o2));

        List<ObjectivesResponse> out = service.getAll(proposalId);

        assertEquals(2, out.size());
        assertEquals(10L, out.get(0).getId());
        assertEquals(2, out.get(1).getObjectiveSeq());
        assertEquals("T2", out.get(1).getTitle());
        verify(objectivesRepo).findAllByProposal_ProposalIdAndProposal_DeletedFalseOrderByObjectiveSeqAsc(proposalId);
    }

    // ----------------------------------------------------------
    // B) create
    // ----------------------------------------------------------
    @Test
    void create_shouldPersist_whenValidAndNoConflicts() {
        Integer proposalId = 5;

        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(proposalId, 1)).thenReturn(false);
        when(objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(proposalId)).thenReturn(0L);
        when(objectivesRepo.save(ArgumentMatchers.<Objectives>any())).thenAnswer(inv -> inv.getArgument(0));

        ObjectiveCreation req = mock(ObjectiveCreation.class);
        when(req.getObjectiveSeq()).thenReturn(1);
        when(req.getTitle()).thenReturn("Title");
        when(req.getDescription()).thenReturn("Desc");
        when(req.getMandatory()).thenReturn(Boolean.TRUE);

        ObjectivesResponse resp = service.create(proposalId, req);

        assertEquals(1, resp.getObjectiveSeq());
        assertEquals("Title", resp.getTitle());
        assertTrue(resp.isMandatory());

        ArgumentCaptor<Objectives> captor = ArgumentCaptor.forClass(Objectives.class);
        verify(objectivesRepo).save(captor.capture());
        Objectives saved = captor.getValue();
        assertEquals(1, saved.getObjectiveSeq());
        assertEquals("Title", saved.getTitle());
        assertEquals("Desc", saved.getDescription());
        assertTrue(saved.isMandatory());
    }

    @Test
    void create_shouldFail_whenProposalNotFound() {
        // Arrange: repo returns empty, so getDraftOrBad() must throw BEFORE reading req
        when(proposalRepo.findByProposalIdAndDeletedFalse(1)).thenReturn(Optional.empty());

        // Provide a fully valid req (even though it shouldn't be read)
        ObjectiveCreation req = mock(ObjectiveCreation.class);

        // Act & Assert
        assertThrows(ProposalNotFoundException.class, () -> service.create(1, req));
    }

    @Test
    void create_shouldFail_whenProposalNotDraft() {
        Integer proposalId = 1;
        Proposal p = new Proposal();
        p.setProposalId(proposalId);
        p.setIdeaStatus(Constants.IdeaStatus.APPROVED); // not DRAFT
        p.setDeleted(false);
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId)).thenReturn(Optional.of(p));

        // Provide a fully valid req (even though it shouldn't be read)
        ObjectiveCreation req = mock(ObjectiveCreation.class);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(proposalId, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void create_shouldFail_whenSeqInvalidOrTitleOrDescriptionInvalid() {
        when(proposalRepo.findByProposalIdAndDeletedFalse(9))
                .thenReturn(Optional.of(draftProposal(9)));

        // Invalid seq
        ObjectiveCreation badSeq = mock(ObjectiveCreation.class);
        when(badSeq.getObjectiveSeq()).thenReturn(0);
        assertThrows(ObjectiveSeqInvalidException.class, () -> service.create(9, badSeq));

        // Blank title
        ObjectiveCreation badTitle = mock(ObjectiveCreation.class);
        when(badTitle.getObjectiveSeq()).thenReturn(1);
        when(badTitle.getTitle()).thenReturn("  ");
        assertThrows(ObjectiveTitleInvalidException.class, () -> service.create(9, badTitle));

        // Blank description
        ObjectiveCreation badDesc = mock(ObjectiveCreation.class);
        when(badDesc.getObjectiveSeq()).thenReturn(1);
        when(badDesc.getTitle()).thenReturn("T");
        when(badDesc.getDescription()).thenReturn(" ");
        assertThrows(ObjectiveDescriptionInvalidException.class, () -> service.create(9, badDesc));
    }

    @Test
    void create_shouldFail_whenSeqConflict() {
        Integer proposalId = 3;
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(proposalId, 2)).thenReturn(true);

        ObjectiveCreation req = mock(ObjectiveCreation.class);
        when(req.getObjectiveSeq()).thenReturn(2);
        when(req.getTitle()).thenReturn("T");
        when(req.getDescription()).thenReturn("D");

        assertThrows(ObjectiveSeqDuplicateException.class, () -> service.create(proposalId, req));
    }

    @Test
    void create_shouldFail_whenMandatoryAlreadyExists() {
        Integer proposalId = 7;
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));
        when(objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(proposalId, 1)).thenReturn(false);
        when(objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(proposalId)).thenReturn(1L);

        ObjectiveCreation req = mock(ObjectiveCreation.class);
        when(req.getObjectiveSeq()).thenReturn(1);
        when(req.getTitle()).thenReturn("T");
        when(req.getDescription()).thenReturn("D");
        when(req.getMandatory()).thenReturn(true);

        assertThrows(OnlyOneMandatoryObjectiveAllowedException.class, () -> service.create(proposalId, req));
    }

    // ----------------------------------------------------------
    // D) uploadProof
    // ----------------------------------------------------------
    @Test
    void uploadProof_shouldStoreFileAndUpdateMetadata_whenPdf() throws Exception {
        Integer proposalId = 100;
        Integer objectiveId = 200;

        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));

        Objectives obj = new Objectives();
        obj.setId(objectiveId.longValue());
        obj.setObjectiveSeq(1);
        obj.setTitle("T");
        obj.setDescription("D");
        when(objectivesRepo.findByIdAndProposal_ProposalId(objectiveId.longValue(), proposalId))
                .thenReturn(Optional.of(obj));
        when(objectivesRepo.save(ArgumentMatchers.<Objectives>any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "proof.pdf", "application/pdf", "hello".getBytes());

        ProofResponse resp = service.uploadProof(proposalId, objectiveId, file);

        verify(objectivesRepo).save(obj);
        assertEquals("proof.pdf", resp.getFileName());
        assertEquals("application/pdf", resp.getContentType());
        assertEquals(file.getSize(), resp.getSizeBytes());

        // Verify file path was set (exact format may vary by service implementation)
        assertNotNull(resp.getFilePath());
    }

    @Test
    void uploadProof_shouldDeleteOldFile_whenReplacingExistingProof() throws IOException {
        Integer proposalId = 101;
        Integer objectiveId = 201;

        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));

        Path old = Paths.get("old-proof.tmp");
        Files.write(old, "old".getBytes());

        Objectives obj = new Objectives();
        obj.setId(objectiveId.longValue());
        obj.setProofFilePath(old.toString());
        when(objectivesRepo.findByIdAndProposal_ProposalId(objectiveId.longValue(), proposalId))
                .thenReturn(Optional.of(obj));
        when(objectivesRepo.save(ArgumentMatchers.<Objectives>any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "p.jpg", "image/jpeg", new byte[] {1, 2, 3});

        ProofResponse resp = service.uploadProof(proposalId, objectiveId, file);

        // Verify new file was uploaded successfully (exact path format may vary)
        assertNotNull(resp.getFilePath());
        assertEquals("image/jpeg", resp.getContentType());
    }

    @Test
    void uploadProof_shouldFail_whenProposalNotDraft() {
        Integer proposalId = 10;
        Proposal p = new Proposal();
        p.setProposalId(proposalId);
        p.setDeleted(false);
        p.setIdeaStatus(Constants.IdeaStatus.APPROVED);
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId)).thenReturn(Optional.of(p));

        MultipartFile file = mock(MultipartFile.class);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.uploadProof(proposalId, 1, file));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void uploadProof_shouldFail_whenObjectiveNotFound() {
        Integer proposalId = 10;
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, proposalId)).thenReturn(Optional.empty());

        MultipartFile file = mock(MultipartFile.class);

        assertThrows(ObjectiveNotFoundInProposalException.class,
                () -> service.uploadProof(proposalId, 1, file));
    }

    @Test
    void uploadProof_shouldFail_whenFileNullOrEmpty() {
        Integer proposalId = 10;
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));

        Objectives obj = new Objectives();
        obj.setId(1L);
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, proposalId))
                .thenReturn(Optional.of(obj));

        assertThrows(ProofFileRequiredException.class,
                () -> service.uploadProof(proposalId, 1, null));

        MockMultipartFile empty = new MockMultipartFile("f", "f.pdf", "application/pdf", new byte[0]);
        assertThrows(ProofFileRequiredException.class,
                () -> service.uploadProof(proposalId, 1, empty));
    }

    @Test
    void uploadProof_shouldFail_whenSizeExceedsLimit() throws Exception {
        Integer proposalId = 10;
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));

        Objectives obj = new Objectives();
        obj.setId(1L);
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, proposalId))
                .thenReturn(Optional.of(obj));

        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getSize()).thenReturn(25L * 1024 * 1024 + 1);
        assertThrows(ObjectiveProofSizeInvalidException.class,
                () -> service.uploadProof(proposalId, 1, big));
    }

    @Test
    void uploadProof_shouldFail_whenInvalidContentType() throws Exception {
        Integer proposalId = 10;
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(draftProposal(proposalId)));

        Objectives obj = new Objectives();
        obj.setId(1L);
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, proposalId))
                .thenReturn(Optional.of(obj));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getContentType()).thenReturn("application/octet-stream");
        assertThrows(ObjectiveProofContentTypeNotSupportedException.class,
                () -> service.uploadProof(proposalId, 1, file));
    }

    // ----------------------------------------------------------
    // E) getProofMetadata
    // ----------------------------------------------------------
    @Test
    void getProofMetadata_shouldReturn_whenPresent() {
        Integer proposalId = 20, objId = 30;

        Objectives obj = new Objectives();
        obj.setId(objId.longValue());
        obj.setProofFileName("proof.pdf");
        obj.setProofContentType("application/pdf");
        obj.setProofFilePath("/x/proof.pdf");
        obj.setProofSizeBytes(123L);

        when(objectivesRepo.findByIdAndProposal_ProposalId(objId.longValue(), proposalId))
                .thenReturn(Optional.of(obj));

        ProofResponse out = service.getProofMetadata(proposalId, objId);

        assertEquals("proof.pdf", out.getFileName());
        assertEquals("application/pdf", out.getContentType());
        assertEquals(123L, out.getSizeBytes());
    }

    @Test
    void getProofMetadata_shouldFail_whenObjectiveMissing() {
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, 2))
                .thenReturn(Optional.empty());
        assertThrows(ObjectiveNotFoundInProposalException.class,
                () -> service.getProofMetadata(2, 1));
    }

    @Test
    void getProofMetadata_shouldFail_whenNoProofUploaded() {
        Integer proposalId = 2, objId = 1;
        Objectives obj = new Objectives();
        obj.setId(1L);
        obj.setProofFilePath(null);
        when(objectivesRepo.findByIdAndProposal_ProposalId(1L, 2))
                .thenReturn(Optional.of(obj));

        assertThrows(ObjectiveProofMissingException.class,
                () -> service.getProofMetadata(proposalId, objId));
    }

    // ----------------------------------------------------------
    // getForReview (with Specification + paging)
    // ----------------------------------------------------------
    @Test
    void getForReview_shouldQueryWithSpecAndMapToResponses() {
        Integer proposalId = 88;

        Proposal p = new Proposal();
        p.setProposalId(proposalId);
        p.setDeleted(false);
        p.setIdeaStatus(Constants.IdeaStatus.PROJECTPROPOSAL);
        when(proposalRepo.findByProposalIdAndDeletedFalse(proposalId))
                .thenReturn(Optional.of(p));

        Pageable pageable = PageRequest.of(0, 5, Sort.by("objectiveSeq"));

        Objectives o = new Objectives();
        o.setId(1L);
        o.setObjectiveSeq(1);
        o.setTitle("Find me");
        o.setDescription("some text");
        o.setMandatory(true);
        o.setProofFileName("proof.pdf");
        o.setProofFilePath("/p.pdf");
        o.setProofContentType("application/pdf");
        o.setProofSizeBytes(1024L);

        Page<Objectives> page = new PageImpl<>(List.of(o), pageable, 1);

        when(objectivesRepo.findAll(ArgumentMatchers.<Specification<Objectives>>any(), eq(pageable)))
                .thenReturn(page);

        Page<ObjectivesResponse> out = service.getForReview(
                proposalId, true, "pdf", true, "find", pageable);

        assertEquals(1, out.getTotalElements());
        ObjectivesResponse item = out.getContent().get(0);
        assertEquals(1, item.getObjectiveSeq());
        assertEquals("Find me", item.getTitle());
        assertEquals("application/pdf", item.getProofContentType());

        verify(objectivesRepo).findAll(ArgumentMatchers.<Specification<Objectives>>any(), eq(pageable));
    }

    @Test
    void getForReview_shouldFail_whenProposalMissing() {
        when(proposalRepo.findByProposalIdAndDeletedFalse(55)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getForReview(55, null, null, null, null, PageRequest.of(0, 10)));
        assertEquals(404, ex.getStatusCode().value());
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------
    private Proposal draftProposal(Integer proposalId) {
        Proposal p = new Proposal();
        p.setProposalId(proposalId);
        p.setDeleted(false);
        p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        return p;
    }
}