package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Objectives;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.dto.objective.ObjectiveCreation;
import com.ideatrack.main.dto.objective.ObjectivesResponse;
import com.ideatrack.main.dto.objective.ProofResponse;
import com.ideatrack.main.exception.ObjectiveDescriptionInvalidException;
import com.ideatrack.main.exception.ObjectiveFileStorageFailedException;
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
import com.ideatrack.main.repository.IObjectivesRepository;
import com.ideatrack.main.repository.IProposalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// Done by Vibhuti

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ObjectiveService {

    private final IProposalRepository proposalRepo;
    private final IObjectivesRepository objectivesRepo;

    private static final long MAX_PROOF_SIZE_BYTES = 25L * 1024L * 1024L; // 25 MB
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String IMAGE_JPEG = "image/jpeg";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            APPLICATION_PDF,
            IMAGE_JPEG
    );

    // A) GET — List all objectives for a proposal
    @Transactional(readOnly = true)
    public List<ObjectivesResponse> getAll(Integer proposalId) {
        List<Objectives> list =
                objectivesRepo.findAllByProposal_ProposalIdAndProposal_DeletedFalseOrderByObjectiveSeqAsc(proposalId);

        return list.stream()
                   .map(this::toResponse)
                   .toList();
    }

    // B) CREATE — Add one objective
    public ObjectivesResponse create(Integer proposalId, ObjectiveCreation req) {
        Proposal p = getDraftOrBad(proposalId);

        if (req.getObjectiveSeq() == null || req.getObjectiveSeq() < 1) {
            throw new ObjectiveSeqInvalidException(proposalId);
        }
        if (req.getTitle() == null || req.getTitle().isBlank() || req.getTitle().length() > 150) {
            throw new ObjectiveTitleInvalidException(proposalId);
        }
        if (req.getDescription() == null || req.getDescription().isBlank() || req.getDescription().length() > 2000) {
            throw new ObjectiveDescriptionInvalidException(proposalId);
        }

        boolean seqExists = objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(proposalId, req.getObjectiveSeq());
        if (seqExists) {
            throw new ObjectiveSeqDuplicateException(proposalId);
        }

        if (Boolean.TRUE.equals(req.getMandatory())) {
            long existingMandatory = objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(proposalId);
            if (existingMandatory > 0) {
                throw new OnlyOneMandatoryObjectiveAllowedException();
            }
        }

        Objectives o = Objectives.builder()
                .proposal(p)
                .objectiveSeq(req.getObjectiveSeq())
                .title(req.getTitle())
                .description(req.getDescription())
                .mandatory(Boolean.TRUE.equals(req.getMandatory()))
                .build();

        Objectives saved = objectivesRepo.save(o);
        return toResponse(saved);
    }

    // C) POST — Upload / Replace proof for an objective (multipart/form-data)
    public ProofResponse uploadProof(Integer proposalId, Integer objectiveId, MultipartFile file) {
        getDraftOrBad(proposalId);

        Objectives obj = objectivesRepo.findByIdAndProposal_ProposalId(objectiveId.longValue(), proposalId)
                .orElseThrow(() -> new ObjectiveNotFoundInProposalException(objectiveId, proposalId));

        if (file == null || file.isEmpty())
            throw new ProofFileRequiredException();

        if (file.getSize() > MAX_PROOF_SIZE_BYTES)
            throw new ObjectiveProofSizeInvalidException(objectiveId, file.getSize(), MAX_PROOF_SIZE_BYTES);

        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CONTENT_TYPES.contains(ct))
            throw new ObjectiveProofContentTypeNotSupportedException(objectiveId, ct);

        String ext;
        if (APPLICATION_PDF.equals(ct)) {
            ext = "pdf";
        } else if (IMAGE_JPEG.equals(ct)) {
            ext = "jpg";
        } else {
            ext = "png";
        }


        Path dir = Paths.get("uploads", "proposals", proposalId.toString(), "objectives", objectiveId.toString());
        Path target = dir.resolve("proof." + ext);

        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(target)) {
                StreamUtils.copy(in, out);
            }
        } catch (Exception e) {
            throw new ObjectiveFileStorageFailedException(target);
        }

        if (obj.getProofFilePath() != null)
            safeDelete(obj.getProofFilePath());

        obj.setProofFileName("proof." + ext);
        obj.setProofFilePath(target.toString());
        obj.setProofContentType(ct);
        obj.setProofSizeBytes(file.getSize());

        objectivesRepo.save(obj);

        return ProofResponse.builder()
                .fileName(obj.getProofFileName())
                .filePath(obj.getProofFilePath())
                .contentType(obj.getProofContentType())
                .sizeBytes(obj.getProofSizeBytes())
                .build();
    }

    // D) GET — Proof Metadata
    @Transactional(readOnly = true)
    public ProofResponse getProofMetadata(Integer proposalId, Integer objId) {
        Objectives obj = objectivesRepo.findByIdAndProposal_ProposalId(objId.longValue(), proposalId)
                .orElseThrow(() -> new ObjectiveNotFoundInProposalException(objId, proposalId));

        if (obj.getProofFilePath() == null)
            throw new ObjectiveProofMissingException(objId);

        return ProofResponse.builder()
                .fileName(obj.getProofFileName())
                .filePath(obj.getProofFilePath())
                .contentType(obj.getProofContentType())
                .sizeBytes(obj.getProofSizeBytes())
                .build();
    }
    
    @Transactional(readOnly = true)
    public Page<ObjectivesResponse> getForReview(
            Integer proposalId,
            Boolean hasProof,            // optional
            String proofType,            // pdf|jpg|png (optional)
            Boolean mandatory,           // optional
            String search,               // optional text in title/description
            Pageable pageable) {

        // Ensure proposal exists & not deleted (no DRAFT restriction for reviewer read)
        proposalRepo.findByProposalIdAndDeletedFalse(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));

        String contentType = normalizeProofType(proofType); // pdf/jpg/png -> MIME or null

        Specification<Objectives> spec = (root, query, cb) -> cb.conjunction();
        spec = spec
                .and(ObjectiveReviewSpecifications.belongsToProposal(proposalId))
                .and(ObjectiveReviewSpecifications.hasProof(hasProof))
                .and(ObjectiveReviewSpecifications.mandatoryEquals(mandatory))
                .and(ObjectiveReviewSpecifications.proofContentTypeEquals(contentType))
                .and(ObjectiveReviewSpecifications.searchText(search));

        Page<Objectives> page = objectivesRepo.findAll(spec, pageable); // <-- use spec here
        return page.map(this::toResponse);
    }
        
    private String normalizeProofType(String proofType) {
        if (proofType == null) return null;

        return switch (proofType.trim().toLowerCase()) {
            case "pdf" -> APPLICATION_PDF;
            case "jpg", "jpeg" -> IMAGE_JPEG;   // merged
            case "png" -> "image/png";
            default -> null;
        };
    }
    
    // Helpers
    private Proposal getDraftOrBad(Integer proposalId) {
        Proposal p = proposalRepo.findByProposalIdAndDeletedFalse(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException(proposalId));
        if (p.getIdeaStatus() != Constants.IdeaStatus.DRAFT)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only allowed in DRAFT.");
        return p;
    }

    private void safeDelete(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (Exception ignored) {
        	log.error("Exception ignored");
        }
    }

    // Local mapper: Objectives -> ObjectivesResponse
    private ObjectivesResponse toResponse(Objectives o) {
        return ObjectivesResponse.builder()
                .id(o.getId())
                .objectiveSeq(o.getObjectiveSeq())
                .title(o.getTitle())
                .description(o.getDescription())
                .mandatory(o.isMandatory())
                .proofFileName(o.getProofFileName())
                .proofContentType(o.getProofContentType())
                .proofFilePath(o.getProofFilePath())
                .proofSizeBytes(o.getProofSizeBytes())
                .build();
    }
}
