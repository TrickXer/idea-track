package com.ideatrack.main.service;

import com.ideatrack.main.data.Objectives;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.dto.objective.ObjectivesResponse;
import com.ideatrack.main.dto.proposal.ProposalListItemResponseDTO;
import com.ideatrack.main.dto.proposal.ProposalResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maps Proposal entity -> DTOs.
 * - Full     : ProposalResponseDTO
 * - List item: ProposalListItemResponseDTO
 *
 * IMPORTANT: Objectives are mapped to ObjectivesResponse DTOs to avoid cyclic
 * serialization (Proposal <-> Objectives) that causes StackOverflowError.
 */
@Component
public class ProposalMapper {

    /** Full DTO used by create/update/submit/getById */
    public ProposalResponseDTO toResponse(Proposal p) {
        if (p == null) return null;

        ProposalResponseDTO dto = new ProposalResponseDTO();

        // IDs
        dto.setProposalId(p.getProposalId());
        dto.setIdeaId(p.getIdea() != null ? p.getIdea().getIdeaId() : null);
        dto.setUserId(p.getUser() != null ? p.getUser().getUserId() : null);

        // Budget: entity Long -> DTO long (primitive) — guard null
        dto.setBudget(p.getBudget() != null ? p.getBudget() : 0L);

        // Dates (types already match your DTO)
        dto.setTimeLineStart(p.getTimeLineStart());
        dto.setTimeLineEnd(p.getTimeLineEnd());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());

        // Enum -> String
        dto.setIdeaStatus(p.getIdeaStatus() != null ? p.getIdeaStatus().name() : null);

        // Objectives -> DTOs (sorted by objectiveSeq)
        if (p.getObjectives() != null) {
            List<ObjectivesResponse> objDtos = p.getObjectives().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Objectives::getObjectiveSeq))
                    .map(this::toObjectivesResponse)
                    .collect(Collectors.toList());
            dto.setObjective(objDtos);
        }

        return dto;
    }

    /** Alias for callers still using toDto name */
    public ProposalResponseDTO toDto(Proposal p) {
        return toResponse(p);
    }

    /** Lightweight list row mapping for grids/tables */
    public ProposalListItemResponseDTO toListItem(Proposal p) {
        if (p == null) return null;

        ProposalListItemResponseDTO dto = new ProposalListItemResponseDTO();

        dto.setProposalId(p.getProposalId());
        dto.setIdeaId(p.getIdea() != null ? p.getIdea().getIdeaId() : null);
        dto.setIdeaTitle(p.getIdea() != null ? p.getIdea().getTitle() : null);
        dto.setUserId(p.getUser() != null ? p.getUser().getUserId() : null);

        dto.setBudget(p.getBudget() != null ? p.getBudget() : 0L);

        dto.setTimeLineStart(p.getTimeLineStart());
        dto.setTimeLineEnd(p.getTimeLineEnd());
        dto.setIdeaStatus(p.getIdeaStatus() != null ? p.getIdeaStatus().name() : null);
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());

        return dto;
    }

    /** Local mapper: Objectives entity -> ObjectivesResponse DTO */
    private ObjectivesResponse toObjectivesResponse(Objectives o) {
        if (o == null) return null;
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