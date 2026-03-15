package com.ideatrack.main.controller;

import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.ideatrack.main.dto.proposal.AcceptedIdeaDashboardDTO;
import com.ideatrack.main.dto.proposal.ProposalCreateRequestDTO;
import com.ideatrack.main.dto.proposal.ProposalResponseDTO;
import com.ideatrack.main.dto.proposal.ProposalSubmitRequest;
import com.ideatrack.main.dto.proposal.ProposalUpdateRequestDTO;
import com.ideatrack.main.service.ProposalService;

import jakarta.validation.Valid;

//Done by Vibhuti

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'REVIEWER', 'EMPLOYEE')")
@RequestMapping("/api/proposal")
@Validated
public class ProposalController {
	public final ProposalService proposalService;
	
	public ProposalController(ProposalService proposalService) {
		this.proposalService = proposalService;
	}
	
		/*
		 * 1) List proposals 
		 * http://localhost:8091/api/proposal/1/accepted-ideas
		 * on the dashboard of employee, 
		 * all the accepted ideas will be displayed to convert to detailed project proposal
		 */		
		@GetMapping("/{userId}/accepted-ideas")
		    public List<AcceptedIdeaDashboardDTO> getAcceptedIdeas(@PathVariable Integer userId) {
		        return proposalService.getAcceptedIdeasWithProposal(userId);
		    }
	
		/*
		 * 2) will convert only accepted ideas o proposal
		 * http://localhost:8091/api/proposal/ideas/3/convert-to-proposal
		 * converting accepted idea to detailed project proposal submission
		 * in which, budget, timeLineStart, timeLineEnd, Objectives and proof for objectives will be added
		 */	
		@PostMapping("/ideas/{ideaId}/convert-to-proposal")
		@ResponseStatus(HttpStatus.CREATED)
		public ProposalResponseDTO convertToProposal(
		        @PathVariable Integer ideaId,
		        @Valid @RequestBody ProposalCreateRequestDTO body) {
		    return proposalService.convertIdeaToProposal(ideaId, body);
		}

		/*
		 * 3) Full update Draft Proposal
		 * http://localhost:8091/api/proposal/updateProposal/18
		 * all fields will be available to be updated
		*/	    
		@PutMapping("/updateProposal/{proposalId}")
	    public ProposalResponseDTO updateDraft(
	            @PathVariable Integer proposalId,
	            @Valid @RequestBody ProposalUpdateRequestDTO body) {
	        return proposalService.updateDraft(proposalId, body);
		}

		/*
		 * 4) Delete Draft proposal
		 * http://localhost:8091/api/proposal/deleteProposal/18
		 * by passing proposalId to path variable, proposal in draft will be deleted
		 */	    
	    @DeleteMapping("/deleteProposal/{proposalId}")
	    @ResponseStatus(HttpStatus.NO_CONTENT)
	    public void deleteDraft(@PathVariable Integer proposalId) {
	        proposalService.deleteDraft(proposalId);
	    }
	    
		/*
		 * 5) Submit DRAFT → PROJECTPROPOSAL
		 * http://localhost:8091/api/proposal/18/submit
		 * upon clicking the submit button, proposal in DRAFT will go in PROJECTPROPOSAL
		 */	    
	    @PostMapping("/{proposalId}/submit")
	    public ProposalResponseDTO submit(@PathVariable Integer proposalId,
	                                      @Valid @RequestBody ProposalSubmitRequest body) {
	        return proposalService.submit(proposalId, body);
	    }
	    

	    // ADD THIS:
    @GetMapping("/{proposalId}")
    public ResponseEntity<?> getById(@PathVariable Integer proposalId) {
        var dto = proposalService.getById(proposalId);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Proposal not found"));
        }
        return ResponseEntity.ok(dto);
    }


}
