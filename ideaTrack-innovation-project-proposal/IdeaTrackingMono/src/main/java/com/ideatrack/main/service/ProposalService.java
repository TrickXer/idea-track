package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.Objectives;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.objective.ObjectiveCreation;
import com.ideatrack.main.dto.proposal.*;
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
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.exception.SubmitTimelineMissingException;
import com.ideatrack.main.exception.TimelineInvalidException;
import com.ideatrack.main.exception.UserIdRequiredException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IObjectivesRepository;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IUserRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProposalService {

    private final IProposalRepository proposalRepo;
    private final IIdeaRepository ideaRepo;
    private final IUserRepository userRepo;
    private final IObjectivesRepository objectivesRepo;
    private final ProposalMapper mapper;
    private final NotificationHelper notificationHelper;

    private static final long MAX_PROOF_BYTES = 25L * 1024 * 1024; // 25 MB
    private static final String PDF = "application/pdf";
    private static final String JPG = "image/jpeg";

    @Transactional
    public ProposalResponseDTO convertIdeaToProposal(Integer ideaId, ProposalCreateRequestDTO req) {
        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> {
                    log.error("Idea with this id is not found: {}", ideaId);
                    return new IdeaNotFound("Idea not found: " + ideaId);
                });

        if (idea.getIdeaStatus() != Constants.IdeaStatus.ACCEPTED) {
            log.warn("Idea should be accepted to proceed for proposal: {}", ideaId);
            throw new IdeaStatusNotAcceptedException(ideaId);
        }

        if (proposalRepo.existsByIdea_IdeaIdAndDeletedFalse(ideaId)) {
            log.error("Proposal already exists for this idea: {}", ideaId);
            throw new ProposalAlreadyExistsException(ideaId);
        }

        Integer userId1 = Optional.ofNullable(req.getUserId())
                .orElseThrow(UserIdRequiredException::new);

        User user = userRepo.findById(userId1)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Proposal draft = Proposal.builder()
                .idea(idea)
                .user(user)
                .budget(Optional.ofNullable(req.getBudget()).orElse(0L))
                .timeLineStart(req.getTimeLineStart())
                .timeLineEnd(req.getTimeLineEnd())
                .ideaStatus(Constants.IdeaStatus.DRAFT) // begin in DRAFT
                .deleted(false)
                .build();

        Proposal saved = proposalRepo.save(draft);

        // Seed objectives if provided
        List<ObjectiveCreation> objectives = req.getObjectives();
        if (objectives == null || objectives.isEmpty()) {
            return mapper.toResponse(saved);
        }

        validateObjectiveBatchForNewProposal(objectives);

        List<Objectives> entities = new ArrayList<>();
        for (ObjectiveCreation dto : objectives) {
            Integer seq = dto.getObjectiveSeq();

            if (dto.getProof() == null) {
                log.warn("Objective {} missing proof", seq);
                throw new ObjectiveProofMissingException(seq);
            }

            var proof = dto.getProof();

            if (proof.getFilePath() == null || proof.getFilePath().isBlank()) {
                log.warn("Objective {} proof filePath is empty", seq);
                throw new ObjectiveProofPathEmptyException(seq);
            }

            if (!(PDF.equals(proof.getContentType()) || JPG.equals(proof.getContentType()))) {
                log.warn("Objective {} proof content type not supported: {}", seq, proof.getContentType());
                throw new ObjectiveProofContentTypeNotSupportedException(seq, proof.getContentType());
            }

            if (proof.getSizeBytes() == null || proof.getSizeBytes() <= 0 || proof.getSizeBytes() > MAX_PROOF_BYTES) {
                log.warn("Objective {} proof size invalid: {}", seq, proof.getSizeBytes());
                throw new ObjectiveProofSizeInvalidException(seq, proof.getSizeBytes(), MAX_PROOF_BYTES);
            }

            Objectives obj = Objectives.builder()
                    .proposal(saved)
                    .objectiveSeq(dto.getObjectiveSeq())
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .mandatory(Boolean.TRUE.equals(dto.getMandatory()))
                    .proofFileName(proof.getFileName())
                    .proofFilePath(proof.getFilePath())
                    .proofContentType(proof.getContentType())
                    .proofSizeBytes(proof.getSizeBytes())
                    .build();

            entities.add(obj);
        }

        List<Objectives> savedObjectives = objectivesRepo.saveAll(entities);

        if (saved.getObjectives() == null) {
            saved.setObjectives(new ArrayList<>(savedObjectives));
        } else {
            saved.getObjectives().addAll(savedObjectives);
        }
        saved.getObjectives().sort(Comparator.comparing(Objectives::getObjectiveSeq));

        return mapper.toResponse(saved);
    }

    /** Validates the incoming objectives payload for a brand-new proposal */
    private void validateObjectiveBatchForNewProposal(List<ObjectiveCreation> objectives) {
        Set<Integer> seenSeq = new HashSet<>();
        for (ObjectiveCreation o : objectives) {
            Integer seq = o.getObjectiveSeq();

            if (seq == null || seq < 1) {
                log.error("Invalid objective seq: {}", seq);
                throw new ObjectiveSeqInvalidException(seq);
            }
            if (!seenSeq.add(seq)) {
                log.error("Duplicate objective seq: {}", seq);
                throw new ObjectiveSeqDuplicateException(seq);
            }

            String title = o.getTitle();
            if (title == null || title.isBlank() || title.length() > 150) {
                log.error("Objective {} title invalid", seq);
                throw new ObjectiveTitleInvalidException(seq);
            }

            String description = o.getDescription();
            if (description == null || description.isBlank() || description.length() > 2000) {
                log.error("Objective {} description invalid", seq);
                throw new ObjectiveDescriptionInvalidException(seq);
            }
        }

        long mandatoryCount = objectives.stream()
                .filter(x -> Boolean.TRUE.equals(x.getMandatory()))
                .count();

        if (mandatoryCount > 1) {
            log.error("Only one objective can be mandatory");
            throw new OnlyOneMandatoryObjectiveAllowedException();
        }
    }

    @Transactional
    public ProposalResponseDTO updateDraft(Integer proposalId, ProposalUpdateRequestDTO req) {
        Proposal p = proposalRepo.findByIdWithObjectives(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        if (Boolean.TRUE.equals(p.isDeleted())) {
            log.error("Proposal already deleted: {}", proposalId);
            throw new ProposalDeletedException(proposalId);
        }

        if (req.getBudget() != null && req.getBudget() < 0) {
            log.error("Budget cannot be negative");
            throw new BudgetNegativeException(req.getBudget());
        }
        if (req.getTimeLineStart() != null && req.getTimeLineEnd() != null
                && req.getTimeLineStart().isAfter(req.getTimeLineEnd())) {
            log.error("Timeline invalid: {} > {}", req.getTimeLineStart(), req.getTimeLineEnd());
            throw new TimelineInvalidException(req.getTimeLineStart(), req.getTimeLineEnd());
        }

        if (req.getBudget() != null) p.setBudget(req.getBudget());
        if (req.getTimeLineStart() != null) p.setTimeLineStart(req.getTimeLineStart());
        if (req.getTimeLineEnd() != null) p.setTimeLineEnd(req.getTimeLineEnd());

        if (p.getIdeaStatus() != Constants.IdeaStatus.DRAFT) {
            p.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        }

        Proposal saved = proposalRepo.save(p);
        return mapper.toResponse(saved);
    }

    public void deleteDraft(Integer proposalId) {
        Proposal p = proposalRepo.findByProposalIdAndDeletedFalse(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        if (p.getIdeaStatus() != Constants.IdeaStatus.DRAFT) {
            log.error("Only draft proposals can be deleted");
            throw new ProposalDeleteOnlyWhenDraftException(proposalId, p.getIdeaStatus());
        }

        p.setDeleted(true); // soft delete
    }

    public Objectives addObjective(Integer proposalId,
                                   Integer objectiveSeq,
                                   String title,
                                   String description,
                                   boolean mandatory) {

        Proposal p = mustGetActiveDraftLocked(proposalId);

        if (objectiveSeq == null || objectiveSeq < 1) {
            throw new ObjectiveSeqInvalidException(objectiveSeq);
        }
        if (title == null || title.isBlank() || title.length() > 150) {
            throw new ObjectiveTitleInvalidException(objectiveSeq);
        }
        if (description == null || description.isBlank() || description.length() > 2000) {
            throw new ObjectiveDescriptionInvalidException(objectiveSeq);
        }
        boolean seqExists = objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(proposalId, objectiveSeq);
        if (seqExists) {
            throw new ObjectiveSeqDuplicateException(objectiveSeq);
        }
        if (mandatory) {
            long existingMandatory = objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(proposalId);
            if (existingMandatory > 0) {
                throw new OnlyOneMandatoryObjectiveAllowedException();
            }
        }

        Objectives o = Objectives.builder()
                .proposal(p)
                .objectiveSeq(objectiveSeq)
                .title(title)
                .description(description)
                .mandatory(mandatory)
                .build();

        Objectives saved = objectivesRepo.save(o);
        if (p.getObjectives() != null) {
            p.getObjectives().add(saved);
            p.getObjectives().sort(Comparator.comparing(Objectives::getObjectiveSeq));
        }
        return saved;
    }

    public Objectives updateObjective(Integer proposalId,
                                      Long objectiveId,
                                      Integer objectiveSeq,
                                      String title,
                                      String description,
                                      Boolean mandatory) {

        Proposal p = mustGetActiveDraftLocked(proposalId);

        Objectives o = objectivesRepo.findByIdAndProposal_ProposalId(objectiveId, proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Objective not found in this proposal: " + objectiveId));

        if (objectiveSeq != null) {
            if (objectiveSeq < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectiveSeq must be ≥ 1");
            }
            boolean seqTaken = objectivesRepo.existsByProposal_ProposalIdAndObjectiveSeq(proposalId, objectiveSeq)
                    && !Objects.equals(o.getObjectiveSeq(), objectiveSeq);
            if (seqTaken) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "objectiveSeq already exists for this proposal: " + objectiveSeq);
            }
            o.setObjectiveSeq(objectiveSeq);
        }

        if (title != null) {
            if (title.isBlank() || title.length() > 150) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required (≤ 150 chars)");
            }
            o.setTitle(title);
        }

        if (description != null) {
            if (description.isBlank() || description.length() > 2000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required (≤ 2000 chars)");
            }
            o.setDescription(description);
        }

        if (mandatory != null) {
            if (mandatory) {
                long othersMandatory = objectivesRepo.countByProposal_ProposalIdAndMandatoryTrue(proposalId);
                if (!o.isMandatory() && othersMandatory > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Only one objective can be mandatory. Unset others first.");
                }
            }
            o.setMandatory(mandatory);
        }

        Objectives saved = objectivesRepo.save(o);
        if (p.getObjectives() != null) {
            p.getObjectives().sort(Comparator.comparing(Objectives::getObjectiveSeq));
        }
        return saved;
    }

    @Transactional
    public ProposalResponseDTO submit(Integer proposalId, ProposalSubmitRequest req) {
        Proposal p = proposalRepo.findByIdWithObjectives(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        if (p.getIdeaStatus() != Constants.IdeaStatus.DRAFT) {
            throw new ProposalSubmitOnlyWhenDraftException(proposalId, p.getIdeaStatus());
        }
        if (req.getTimeLineStart() == null || req.getTimeLineEnd() == null) {
            throw new SubmitTimelineMissingException();
        }
        if (req.getTimeLineStart().isAfter(req.getTimeLineEnd())) {
            throw new TimelineInvalidException(req.getTimeLineStart(), req.getTimeLineEnd());
        }
        if (req.getBudget() < 0) {
            throw new BudgetNegativeException(req.getBudget());
        }

        // Apply basics
        p.setTimeLineStart(req.getTimeLineStart());
        p.setTimeLineEnd(req.getTimeLineEnd());
        p.setBudget(req.getBudget());

        // Promote status
        p.setIdeaStatus(Constants.IdeaStatus.PROJECTPROPOSAL);

        Proposal saved = proposalRepo.save(p);
        notificationHelper.notifyProposalSubmitted(saved, saved.getUser());
        return mapper.toResponse(saved);
    }

    private Specification<Proposal> notDeleted() {
        return (root, q, cb) -> cb.isFalse(root.get("deleted"));
    }

    private Specification<Proposal> hasStatus(String status) {
        return (root, q, cb) -> cb.equal(cb.lower(root.get("ideaStatus").as(String.class)), status.toLowerCase());
    }

    private Specification<Proposal> hasIdeaId(Integer ideaId) {
        return (root, q, cb) -> cb.equal(root.get("idea").get("ideaId"), ideaId);
    }

    private Specification<Proposal> createdAtBetween(LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            Path<LocalDateTime> created = root.get("createdAt");
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (from != null) ps.add(cb.greaterThanOrEqualTo(created, from.atStartOfDay()));
            if (to != null) ps.add(cb.lessThan(created, to.plusDays(1).atStartOfDay()));
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Specification<Proposal> searchInObjectivesOrIdeaTitle(String search) {
        return (root, query, cb) -> {
            String like = "%" + search.toLowerCase() + "%";

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }

            Join<Proposal, Idea> ideaJoin = root.join("idea", JoinType.LEFT);
            Join<Proposal, Objectives> objJoin = root.join("objectives", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(ideaJoin.get("title")), like),
                    cb.like(cb.lower(objJoin.get("title")), like),
                    cb.like(cb.lower(objJoin.get("description")), like)
            );
        };
    }

    private Proposal mustGetActiveDraftLocked(Integer proposalId) {
        Proposal p = proposalRepo.findForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposal not found: " + proposalId));
        if (Boolean.TRUE.equals(p.isDeleted())) {
            throw new ResponseStatusException(BAD_REQUEST, "Proposal is deleted");
        }
        if (p.getIdeaStatus() != Constants.IdeaStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Only Draft proposals can be updated");
        }
        return p;
    }

    public Page<ProposalListItemResponseDTO> list(ProposalListFilterDTO filter, PageRequest pageable) {
        Specification<Proposal> spec = (root, query, cb) -> cb.conjunction();

        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            spec = spec.and(ProposalSpecifications.hasStatus(filter.getStatus()));
        }
        if (filter.getIdeaId() != null) {
            spec = spec.and(ProposalSpecifications.hasIdeaId(filter.getIdeaId()));
        }
        if (filter.getFrom() != null && filter.getTo() != null) {
            spec = spec.and(ProposalSpecifications.createdBetween(filter.getFrom(), filter.getTo()));
        }
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            spec = spec.and(ProposalSpecifications.searchText(filter.getSearch()));
        }

        Page<Proposal> result = proposalRepo.findAll(spec, pageable);
        return result.map(mapper::toListItem);
    }

    @Transactional(readOnly = true)
    public Page<ProposalListItemResponseDTO> list(ProposalListFilterDTO f, Pageable pageable) {
        Specification<Proposal> spec = Specification.where(notDeleted());

        if (f.getStatus() != null && !f.getStatus().isBlank()) {
            spec = spec.and(hasStatus(f.getStatus()));
        }
        if (f.getIdeaId() != null) {
            spec = spec.and(hasIdeaId(f.getIdeaId()));
        }
        if (f.getFrom() != null || f.getTo() != null) {
            spec = spec.and(createdAtBetween(f.getFrom(), f.getTo()));
        }
        if (f.getSearch() != null && !f.getSearch().isBlank()) {
            spec = spec.and(searchInObjectivesOrIdeaTitle(f.getSearch()));
        }

        return proposalRepo.findAll(spec, pageable)
                .map(mapper::toListItem);
    }

    public Page<ProposalListItemResponseDTO> listByEmployee(Integer userId, Pageable pageable) {
        return proposalRepo.findByUser_UserId(userId, pageable)
                .map(mapper::toListItem);
    }

    public List<AcceptedIdeaDashboardDTO> getAcceptedIdeasWithProposal(Integer userId) {
        List<Constants.IdeaStatus> acceptedLikeStatuses = new ArrayList<>();
        acceptedLikeStatuses.add(Constants.IdeaStatus.APPROVED);
        try {
            acceptedLikeStatuses.add(Constants.IdeaStatus.valueOf("ACCEPTED"));
        } catch (IllegalArgumentException ignored) {}

        List<Idea> ideas = ideaRepo.findByUser_UserIdAndIdeaStatusInAndDeletedFalse(userId, acceptedLikeStatuses);
        if (ideas.isEmpty()) return List.of();

        List<Integer> ideaIds = ideas.stream().map(Idea::getIdeaId).toList();
        List<Proposal> proposals = proposalRepo.findByIdea_IdeaIdInAndUser_UserIdAndDeletedFalse(ideaIds, userId);

        Map<Integer, Proposal> proposalByIdeaId = proposals.stream()
                .collect(Collectors.toMap(p -> p.getIdea().getIdeaId(), p -> p, (a, b) -> a));

        List<AcceptedIdeaDashboardDTO> out = new ArrayList<>(ideas.size());
        for (Idea i : ideas) {
            Proposal p = proposalByIdeaId.get(i.getIdeaId());

            AcceptedIdeaDashboardDTO dto = new AcceptedIdeaDashboardDTO();
            dto.setIdeaId(i.getIdeaId());
            dto.setIdeaTitle(i.getTitle());
            dto.setIdeaDescription(i.getDescription());
            dto.setIdeaStatus(i.getIdeaStatus() != null ? i.getIdeaStatus().name() : null);
            dto.setIdeaCreatedAt(i.getCreatedAt());

            if (p != null) {
                dto.setProposalId(p.getProposalId());
                dto.setBudget(p.getBudget());
                dto.setTimeLineStart(p.getTimeLineStart());
                dto.setTimeLineEnd(p.getTimeLineEnd());
                dto.setProposalStatus(p.getIdeaStatus() != null ? p.getIdeaStatus().name() : null);
                dto.setProposalCreatedAt(p.getCreatedAt());
            }

            out.add(dto);
        }

        return out;
    }

    @Transactional(readOnly = true)
    public ProposalResponseDTO getById(Integer proposalId) {
        Proposal p = proposalRepo.findByIdWithObjectives(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalId));
        return mapper.toDto(p); // ✅ use bean mapper, not static
    }
}
