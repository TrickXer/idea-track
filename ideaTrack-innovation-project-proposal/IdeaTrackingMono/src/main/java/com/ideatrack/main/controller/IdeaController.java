package com.ideatrack.main.controller;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.service.IdeaService;
import com.ideatrack.main.service.UserActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ideas")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'REVIEWER', 'EMPLOYEE')")
public class IdeaController {

    private final IdeaService ideaService;
    private final UserActivityService activityService;

    /**
     * CREATE NEW IDEA
     */
    @PostMapping("/insertIdea")
    public ResponseEntity<String> createIdea(@Valid @RequestBody IdeaCreateRequest req, Principal principal) {
        IdeaResponse createdIdea = ideaService.createIdea(req, principal.getName());
        if (createdIdea != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Idea is inserted successfully");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to insert");
    }

    /**
     * GET SINGLE IDEA DETAILS
     */
    @GetMapping("/{id}")
    public ResponseEntity<IdeaResponse> getIdea(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer viewerUserId) {
        return ResponseEntity.ok(ideaService.getIdea(id, viewerUserId));
    }

    /**
     * UPDATE EXISTING IDEA (DRAFT OR PUBLISHED)
     */
    @PutMapping("/editDraft/{id}")
    public ResponseEntity<String> updateIdea(
            @PathVariable Integer id,
            @Valid @RequestBody IdeaUpdateRequest req) {
        IdeaResponse obj = ideaService.updateIdea(id, req);
        if (obj != null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Your changes have been saved in draft");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to update draft");
    }

    /**
     * SUBMIT IDEA
     */
    @PostMapping("/submit/{id}")
    public ResponseEntity<String> submitIdea(@PathVariable Integer id) {
        IdeaResponse obj = ideaService.submitIdea(id);
        if (obj != null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("The idea is submitted");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to submit");
    }

    /**
     * GET ALL IDEAS BY USER
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<IdeaResponse>> getIdeasByUserId(@PathVariable Integer userId) {
        List<IdeaResponse> userIdeas = ideaService.getIdeasByUser(userId);
        return userIdeas.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(userIdeas);
    }

    /**
     * SOFT DELETE IDEA
     */
    @DeleteMapping("/deleteIdea/{id}")
    public ResponseEntity<String> deleteIdea(@PathVariable Integer id) {
        ideaService.softDeleteIdea(id);
        return ResponseEntity.ok("The idea has been successfully deleted");
    }

    /**
     * SEARCH, FILTER, AND DASHBOARD
     */
    @GetMapping
    public ResponseEntity<PagedResponse<IdeaResponse>> searchIdeas(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Constants.IdeaStatus status,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) Integer viewerUserId
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return ResponseEntity.ok(
                ideaService.searchIdeas(q, categoryId, userId, status, stage, includeDeleted, pageable, viewerUserId)
        );
    }

    /**
     * ADD COMMENT TO IDEA
     */
    @PostMapping("/postComments/{id}")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable("id") Integer ideaId,
            @RequestBody CommentRequestDTO req,
            Principal principal) {
        return ResponseEntity.ok(activityService.addComment(ideaId, req.getText(), principal.getName()));
    }

    /**
     * GET ALL COMMENTS FOR AN IDEA
     */
    @GetMapping("/getComments/{id}")
    public ResponseEntity<List<AllCommentsDTO>> getComments(@PathVariable Integer id) {
        return ResponseEntity.ok(activityService.getAllCommentsForIdea(id));
    }

    /**
     * UPVOTE OR DOWNVOTE
     */
    @PostMapping("/vote/{id}")
    public ResponseEntity<ActivityResultResponse> castVote(
            @PathVariable Integer id,
            @RequestBody VoteRequest req,
            Principal principal) { // Added Principal

        ActivityResultResponse result = 
                activityService.castVote(id, req.getVoteType(), principal.getName());

        return ResponseEntity.ok(result);
    }

    /**
     * REMOVE VOTE
     */
    @DeleteMapping("/removeVote/{id}")
    public ResponseEntity<String> removeVote(
            @PathVariable Integer id, 
            Principal principal) { // Use Principal instead of @RequestParam userId
        
        activityService.removeVote(id, principal.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Vote removed successfully");
    }
    /**
     * SAVE OR UNSAVE IDEA
     */
    @PostMapping("/save/{id}")
    public ResponseEntity<String> toggleSave(
            @PathVariable Integer id,
            @RequestParam boolean shouldSave) {
        //activityService.toggleSave(id, shouldSave, null);
        return ResponseEntity.ok(shouldSave ? "The idea is saved" : "Idea removed from saved");
    }

    private Pageable toPageable(int page, int size, String sortParam) {
        String[] parts = sortParam.split(",");
        String field = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }
}