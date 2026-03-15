package com.ideatrack.main.service;
 
import com.ideatrack.main.data.Constants;
 
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.dto.ActivityResultResponse;
import com.ideatrack.main.dto.AllCommentsDTO;
import com.ideatrack.main.dto.CommentDTO;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;
import jakarta.transaction.Transactional;
 
import java.time.LocalDateTime;
import java.util.List;
 
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
 
 
@Service
@Transactional
public class UserActivityService {
 
    private final IIdeaRepository ideaRepository;
    private final IUserRepository userRepository;
    private final IUserActivityRepository userActivityRepository;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;

    public UserActivityService(IIdeaRepository ideaRepository,
                               IUserRepository userRepository,
                               IUserActivityRepository userActivityRepository,
                               GamificationService gamificationService,
                               NotificationHelper notificationHelper){
        this.ideaRepository = ideaRepository;
        this.userRepository = userRepository;
        this.userActivityRepository = userActivityRepository;
        this.gamificationService = gamificationService;
        this.notificationHelper = notificationHelper;
    }    // -------------------------
    // 1. Comments
    // -------------------------
    public CommentDTO addComment(Integer ideaId, String text,String authEmail) {
        // 1. Fetch the Idea and User entities to ensure they exist
    	Idea idea = requireIdea(ideaId);
    	User user = userRepository.findByEmail(authEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + authEmail));
    	
    	
 
        int delta = gamificationService.getDeltaForActivity(Constants.ActivityType.COMMENT);
 
        // 2. Map to the Entity (UserActivity)
        UserActivity activity = UserActivity.builder()
                .idea(idea)     
                .user(user)// Mapped from Path                      
                .commentText(text)                // Mapped from JSON Body
                .activityType(Constants.ActivityType.COMMENT)
                .delta(delta)
                .event("COMMENT_ADDED")
                .createdAt(LocalDateTime.now())
                .build();
        
        Integer authorId = idea.getUser().getUserId();
 
        UserActivity saved =userActivityRepository.save(activity);
        gamificationService.applyDelta(authorId, delta);
        notificationHelper.notifyCommentAdded(idea, user);

        // 3. Map the Saved Entity back to CommentDTO for the response
        return toCommentDTO(saved);
    }
 
 
// -------------------------
// 2. Voting (Upvote/Downvote)
// ------------------------
public ActivityResultResponse castVote(Integer ideaId,  Constants.VoteType voteType,String authEmail) {
     Idea idea = requireIdea(ideaId);
     
     User voter = userRepository.findByEmail(authEmail)
             .orElseThrow(() -> new UserNotFoundException("User not found: " + authEmail));
 
     // Check for existing vote
     UserActivity vote = userActivityRepository
    		 .findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(
                     ideaId, voter.getUserId(), Constants.ActivityType.VOTE)
             .orElseGet(() -> {
            	 UserActivity v = new UserActivity();
                 v.setIdea(idea);
                 v.setUser(voter);
                 v.setActivityType(Constants.ActivityType.VOTE);
                 v.setCreatedAt(LocalDateTime.now());
                 // Award XP only when first vote record is created
                 int delta = gamificationService.getDeltaForActivity(Constants.ActivityType.VOTE);
                 v.setDelta(delta);
                 
                 gamificationService.applyDelta(voter.getUserId(), delta);
                 return v;
             });
 
     vote.setVoteType(voteType);
     vote.setEvent("VOTE_" + voteType.name());
     userActivityRepository.save(vote);
     notificationHelper.notifyVoteCast(idea, voter, voteType.name());

     return ActivityResultResponse.builder()
             .ideaId(ideaId)
             .userId(voter.getUserId())
             .voteType(voteType)
             .build();
}
 
 
 
public void removeVote(Integer ideaId, String authEmail) {
	    // 1. Fetch the Idea and the Logged-in User (the Voter)
	    requireIdea(ideaId); // Ensure idea exists
	    User voter = userRepository.findByEmail(authEmail)
	            .orElseThrow(() -> new UserNotFoundException("User not found: " + authEmail));
 
	    // 2. Find the specific vote cast by THIS user for THIS idea
	    userActivityRepository
	            .findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(
	                    ideaId, voter.getUserId(), Constants.ActivityType.VOTE)
	            .ifPresent(v -> {
	                // 3. Undo the XP for the VOTER (not the idea author)
	                gamificationService.undoXPChange(voter.getUserId(), v.getUserActivityId());
 
	                // 4. Soft-delete the activity record
	                v.setDeleted(true);
	                v.setEvent("VOTE_REMOVED");
	                userActivityRepository.save(v);
	            });
	}
 
    // -------------------------
    // 3. Save Idea
    // -------------------------
    @PreAuthorize("@ideaService.isOwner(#ideaId,authentication.name)")
    public void toggleSave(Integer ideaId, boolean shouldSave)  {
        Idea idea = requireIdea(ideaId);
 
        // 1. Find existing or create new
        UserActivity saveRecord = userActivityRepository
                .findFirstByIdea_IdeaIdAndActivityTypeAndDeletedFalse(
                         ideaId, Constants.ActivityType.SAVE)
                .orElseGet(() -> {
                    UserActivity s = new UserActivity();
                    s.setIdea(idea);
                    s.setActivityType(Constants.ActivityType.SAVE);
                    return s;
                });
 
        // 2. ALWAYS update the saved status based on the parameter
        saveRecord.setSavedIdea(shouldSave);
        
        // 3. Logic for 'Deleted' column (so 'getSaved' queries stay clean)
        saveRecord.setDeleted(!shouldSave);
 
        userActivityRepository.save(saveRecord);
    }
 
    // -------------------------
    // Helpers & Mappers
    // ------------------------
 
    private CommentDTO toCommentDTO(UserActivity ua) {
        return CommentDTO.builder()
                .userActivityId(ua.getUserActivityId())
                .ideaId(ua.getIdea().getIdeaId())
                .userId(ua.getUser().getUserId())
                .displayName(ua.getUser().getName())
                .commentText(ua.getCommentText())
                .createdAt(ua.getCreatedAt())
                .build();
    }
    
    
    public List<AllCommentsDTO> getAllCommentsForIdea(Integer ideaId) {
        // 1. Fetch the list from the database
        List<UserActivity> comments = userActivityRepository
                .findAllByIdea_IdeaIdAndActivityTypeAndDeletedFalseOrderByCreatedAtAsc(
                    ideaId,
                    Constants.ActivityType.COMMENT
                );
 
        // 2. Map and Return
        return comments.stream()
                .<AllCommentsDTO>map(activity -> AllCommentsDTO.builder() // Added <AllCommentsDTO>
                        .displayName(activity.getUser().getName())
                        .commentText(activity.getCommentText())
                        .build())
                .toList();
    }
 
 
 
    private Idea requireIdea(Integer id)  {
        return ideaRepository.findById(id).orElseThrow(() -> new IdeaNotFound("Idea not found"));
    }
 
    /**
  * Writes a timeline entry for idea status / reviewer-stage decision using CURRENTSTATUS.
  * - activityType = CURRENTSTATUS
  * - decision = any Constants.IdeaStatus (SUBMITTED..PENDING..ACCEPTED..REJECTED..REFINE..etc)
  * - delta = gamificationService.getDeltaForIdeaStatus(decision)
  * - applies XP to actor and/or owner based on flags
  */
public UserActivity logCurrentStatus(
         Idea idea,
         User actor,
         Integer stageId,
         Constants.IdeaStatus statusDecision,
         String event,
         boolean applyXpToActor,
         boolean applyXpToOwner
) {
     if (idea == null) throw new IllegalArgumentException("idea cannot be null");
     if (actor == null) throw new IllegalArgumentException("actor cannot be null");
     if (statusDecision == null) throw new IllegalArgumentException("statusDecision cannot be null");
 
     int delta = gamificationService.getDeltaForIdeaStatus(statusDecision);
 
     UserActivity ua = UserActivity.builder()
             .idea(idea)
             .user(actor)
             .activityType(Constants.ActivityType.CURRENTSTATUS)
             .decision(statusDecision)
             .event(event)
             .stageId(stageId)
             .delta(delta)
             .commentText(event) // keep simple; can be enriched later
             .createdAt(LocalDateTime.now())
             .deleted(false)
             .build();
 
     UserActivity saved = userActivityRepository.save(ua);
 
     if (applyXpToActor && delta != 0) {
         gamificationService.applyDelta(actor.getUserId(), delta);
     }
     if (applyXpToOwner && delta != 0 && idea.getUser() != null) {
         gamificationService.applyDelta(idea.getUser().getUserId(), delta);
     }
     return saved;
}
 
/**
  * Writes final whole-idea decision marker using FINALDECISION.
  * - activityType = FINALDECISION
  * - decision = APPROVED or REJECTED only
  * - delta = gamificationService.getDeltaForActivity(FINALDECISION) (usually 0 unless rule exists)
  */
public UserActivity logFinalDecision(
         Idea idea,
         User actor,
         Integer stageId,
         Constants.IdeaStatus finalDecision, // must be APPROVED or REJECTED
         String event,
         boolean applyXpToActor,
         boolean applyXpToOwner
) {
     if (finalDecision != Constants.IdeaStatus.APPROVED && finalDecision != Constants.IdeaStatus.REJECTED) {
         throw new IllegalArgumentException("FINALDECISION must be APPROVED or REJECTED only");
     }
 
     int delta = gamificationService.getDeltaForActivity(Constants.ActivityType.FINALDECISION);
 
     UserActivity ua = UserActivity.builder()
             .idea(idea)
             .user(actor)
             .activityType(Constants.ActivityType.FINALDECISION)
             .decision(finalDecision)
             .event(event)
             .stageId(stageId)
             .delta(delta)
             .commentText(event)
             .createdAt(LocalDateTime.now())
             .deleted(false)
             .build();
 
     UserActivity saved = userActivityRepository.save(ua);
 
     if (applyXpToActor && delta != 0) {
         gamificationService.applyDelta(actor.getUserId(), delta);
     }
     if (applyXpToOwner && delta != 0 && idea.getUser() != null) {
         gamificationService.applyDelta(idea.getUser().getUserId(), delta);
     }
     return saved;
}
}
 