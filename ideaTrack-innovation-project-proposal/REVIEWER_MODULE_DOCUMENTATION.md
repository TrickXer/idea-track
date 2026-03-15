# REVIEWER MODULE - COMPLETE TECHNICAL DOCUMENTATION
## Detailed Function-by-Function Analysis

---

## Table of Contents
1. Module Overview
2. Architecture & Design
3. Core Services - Detailed Function Analysis
4. Data Models
5. Utility Classes
6. Repository Interfaces
7. Controllers & API Endpoints
8. Database Tables
9. Complete Workflow Examples
10. SLA & Timeout Handling

---

## 1. MODULE OVERVIEW

### Purpose
The Reviewer Module is the heart of the innovation evaluation system. It manages the assignment of ideas to reviewers, collection of feedback decisions, calculation of consensus through matrix voting, and tracking of review timelines. The module ensures fair evaluation, prevents reviewer overload, enforces time-based SLA deadlines, and tracks the entire review history.

### Key Responsibilities

1. **Reviewer Assignment** - Automatically assign reviewers to submitted ideas based on category expertise
2. **Decision Collection** - Gather approval/rejection/refinement decisions from reviewers
3. **Discussion Management** - Enable structured feedback discussions between reviewers
4. **Timeline Tracking** - Log all activities with timestamps for audit trail
5. **SLA Enforcement** - Ensure decisions are made within 3-day deadline
6. **Matrix Resolution** - Calculate consensus from multiple reviewer votes
7. **Gamification** - Award XP points for reviewer activities
8. **Notification** - Alert stakeholders of important events

### Key Services in the Reviewer Module

```
ReviewerAssignmentService
    ↓
ReviewerDecisionService
    ↓
ReviewerDiscussionService
    ↓
ReviewerTimelineUtil
    ↓
ReviewerService (Coordinator)
    ↓
ReviewerStageAssignmentService
    ↓
ReviewerDashboardService
```

---

## 2. ARCHITECTURE & DESIGN

### Module Architecture

```
┌─────────────────────────────────────────────────────────┐
│           ReviewerController (REST API)                 │
│  - Routes: /api/reviewer/...                            │
│  - Handles HTTP requests from UI                        │
└────────────────┬────────────────────────────────────────┘
                 │
      ┌──────────┴──────────┬──────────────┬──────────────┐
      ↓                     ↓              ↓              ↓
┌──────────────┐  ┌──────────────────┐  ┌────────────┐  ┌────────────────┐
│ReviewerDash  │  │ReviewerAssignment│  │ReviewerDec │  │ReviewerDiscuss │
│boardService  │  │      Service     │  │  isionServ │  │    ionService  │
│              │  │                  │  │  ice       │  │                │
│ • Get all    │  │ • Auto-assign    │  │ • Validate │  │ • Validate     │
│   ideas for  │  │   reviewers at   │  │   decision │  │   discussion   │
│   reviewer   │  │   EOD            │  │ • Process  │  │ • Save comment │
│ • Filter by  │  │ • Verify no     │  │   feedback │  │ • Thread       │
│   status     │  │   re-assignment  │  │ • Resolve  │  │   replies      │
│ • Pagination │  │ • Notify         │  │   stage by │  │ • Notify       │
│              │  │   reviewers      │  │   matrix   │  │   owner        │
└──────────────┘  │ • Send SLA       │  │ • Check    │  │ • Award XP     │
                  │   warnings       │  │   SLA      │  │ • Pagination   │
                  │ • Award XP       │  │ • Award XP │  └────────────────┘
                  └──────────────────┘  │ • Log      │
                                        │   activity │
                                        └────────────┘
                           ↓
                  ┌────────────────────────┐
                  │ ReviewerTimelineUtil   │
                  │ (Static Logging Utility)
                  │                        │
                  │ • logCurrentStatus()   │
                  │ • logWithDelta()       │
                  │ • logFinalDecision()   │
                  │ • logProposal...()     │
                  └────────────────────────┘
                           ↓
      ┌────────────────────┴──────────────────────┐
      ↓                                            ↓
┌──────────────────┐                    ┌──────────────────┐
│ Repositories     │                    │ GamificationServ │
│                  │                    │ ice              │
│ • IAssigned...   │                    │ • getDeltaFor    │
│ • IReviewerCat.. │                    │   IdeaStatus()   │
│ • IIdeaRepository│                    │ • getDeltaFor    │
│ • IUserActivity..│                    │   Activity()     │
│ • IUserRepository│                    │ • applyDelta()   │
└──────────────────┘                    └──────────────────┘
      ↓                                            ↓
   MySQL Database                          XP Points System
```

### Design Principles

1. **Separation of Concerns** - Each service handles specific responsibilities
2. **Transaction Management** - All database changes are atomic (@Transactional)
3. **Immutable Timeline** - UserActivity records cannot be changed (append-only audit trail)
4. **Soft Deletes** - Non-destructive deletions preserve data integrity
5. **Error Handling** - Custom exceptions for clear error messages
6. **Logging** - All critical operations logged with Slf4j
7. **Security** - JWT-based authentication, role-based authorization
8. **Idempotency** - Duplicate requests safely ignored

---

## 3. CORE SERVICES - DETAILED FUNCTION ANALYSIS

---

## 3.1 ReviewerAssignmentService

**Purpose:** Automatically assign reviewers to submitted ideas at the end of the day.

**Author:** Pavan

**Scope:** Handles the initial distribution of review work to capable reviewers based on category expertise.

### Class Structure

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewerAssignmentService {
    // Dependencies
    private final IIdeaRepository ideaRepo;
    private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    private final IUserActivityRepository activityRepo;
    private final IReviewerCategoryRepository reviewerCategoryRepo;
    private final IUserRepository userRepo;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;
}
```

### Key Functions

#### Function 1: assignSubmittedIdeasEndOfDay()

**Trigger:** Scheduled daily at 23:59 (11:59 PM)

**Purpose:** Automatically process all SUBMITTED ideas and assign to appropriate reviewers.

**Function Flow:**

```
1. Query all ideas with status = SUBMITTED and deleted = false

2. For each submitted idea:
   a. Validate idea is not null and not deleted
   b. Get idea's category
   c. Determine stage = 1 (first review stage)
   d. Query active reviewers for category + stage 1
   
   e. For each found reviewer:
      - Check if reviewer was ever assigned to this idea (prevent re-assign)
      - If new assignment:
        * Create AssignedReviewerToIdea record
        * Set: idea, reviewer, category, stage, decision=null, deleted=false
        * Save to database
        * Send notification to reviewer about assignment
   
   f. After all reviewers assigned:
      - Update idea status: SUBMITTED → UNDERREVIEW
      - Set idea.stage = 1
      - Save idea
      - Log STATUS_CHANGE activity (link to owner)
      - Log STAGE_START activity (for SLA tracking)
   
   g. Process count++

3. Log completion: "EOD assignment completed. processed=X/Y"
```

**Code Detail:**

```java
@Scheduled(cron = "${reviewer.assign.cron:0 59 23 * * *}")
public void assignSubmittedIdeasEndOfDay() {
    // Step 1: Fetch submitted ideas
    List<Idea> submitted = ideaRepo.findByIdeaStatusAndDeletedFalse(
        Constants.IdeaStatus.SUBMITTED
    );
    if (submitted == null || submitted.isEmpty()) {
        log.info("EOD reviewer assignment: no ideas found in SUBMITTED.");
        return;
    }

    int processed = 0;
    for (Idea idea : submitted) {
        // Step 2: Validate idea
        if (idea == null || idea.isDeleted() || idea.getCategory() == null) 
            continue;

        Integer stage = 1;
        Integer categoryId = idea.getCategory().getCategoryId();

        // Step 3: Find active reviewers for this category and stage
        List<Integer> reviewerIds = reviewerCategoryRepo
            .findActiveReviewerUserIdsByCategoryAndStage(categoryId, stage);

        if (reviewerIds == null || reviewerIds.isEmpty()) {
            log.info("No active reviewers for ideaId={} categoryId={} stage={}",
                idea.getIdeaId(), categoryId, stage);
            continue;
        }

        // Step 4: Get reviewer user objects
        List<User> reviewers = userRepo.findAllById(reviewerIds);
        
        for (User r : reviewers) {
            if (r == null) continue;

            // Step 5: Check if ever assigned before
            boolean everAssigned = reviewerAssignRepo
                .existsByIdea_IdeaIdAndReviewer_UserIdAndStage(
                    idea.getIdeaId(), 
                    r.getUserId(), 
                    stage
                );
            if (everAssigned) continue;

            // Step 6: Create new assignment
            reviewerAssignRepo.save(
                AssignedReviewerToIdea.builder()
                    .idea(idea)
                    .reviewer(r)
                    .category(idea.getCategory())
                    .stage(stage)
                    .decision(null)
                    .deleted(false)
                    .build()
            );

            // Step 7: Notify reviewer
            notificationHelper.notifyReviewerAssigned(idea, r, stage);
        }

        // Step 8: Update idea status and stage
        idea.setStage(stage);
        idea.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
        ideaRepo.save(idea);

        // Step 9: Log timeline activities
        User owner = idea.getUser();
        if (owner == null) {
            log.warn("Idea owner not found for ideaId={}. Skipping timeline log.", 
                idea.getIdeaId());
            continue;
        }

        // Log status change
        ReviewerTimelineUtil.logCurrentStatus(
            activityRepo, gamificationService, idea, owner, stage,
            Constants.IdeaStatus.UNDERREVIEW, 
            "STATUS_CHANGE", 
            true,   // award XP to actor (owner)
            true    // award XP to idea owner
        );

        // Log stage start (for SLA timer)
        ReviewerTimelineUtil.logCurrentStatus(
            activityRepo, gamificationService, idea, owner, stage,
            Constants.IdeaStatus.UNDERREVIEW, 
            "STAGE_START", 
            false,  // don't award XP
            false
        );

        processed++;
        log.info("EOD Auto-assigned ideaId={} stage={} reviewers={}", 
            idea.getIdeaId(), stage, reviewers.size());
    }

    log.info("EOD reviewer assignment completed. processed={}/{}", 
        processed, submitted.size());
}
```

**Key Points:**

- **Idempotency:** Checks `everAssigned` to prevent duplicate assignments
- **No Re-Assignment:** Once assigned for a stage, never reassigned (even if deleted)
- **Selective:** Only processes new assignments
- **Timeline:** Creates stage markers for SLA enforcement
- **Notification:** Alerts reviewers immediately
- **Atomic:** All database changes in single transaction

**Example Scenario:**

```
Time: 23:59 (EOD job triggers)

Database: 3 SUBMITTED ideas
  - Idea#101 (Category: Mobile, stage 1, no reviewers yet)
  - Idea#102 (Category: Cloud, stage 1, no reviewers yet)
  - Idea#103 (Category: Mobile, stage 1, no reviewers yet)

Category: Mobile has 2 reviewers assigned to Stage 1
  - Reviewer#5 (John)
  - Reviewer#8 (Alice)

Category: Cloud has 1 reviewer
  - Reviewer#12 (Bob)

Execution:
1. Find: submitted = [Idea#101, Idea#102, Idea#103]

2. Process Idea#101:
   - Category = Mobile, Stage = 1
   - Find reviewers: [5, 8]
   - Check if 5 ever assigned: NO → Create assignment
   - Check if 8 ever assigned: NO → Create assignment
   - Update Idea#101: SUBMITTED → UNDERREVIEW, stage=1
   - Log STATUS_CHANGE (actor=owner)
   - Log STAGE_START (for SLA timer)
   - Notify: John, Alice
   - processed++

3. Process Idea#102:
   - Category = Cloud, Stage = 1
   - Find reviewers: [12]
   - Check if 12 ever assigned: NO → Create assignment
   - Update Idea#102: SUBMITTED → UNDERREVIEW, stage=1
   - Log STATUS_CHANGE
   - Log STAGE_START
   - Notify: Bob
   - processed++

4. Process Idea#103:
   - Category = Mobile, Stage = 1
   - Find reviewers: [5, 8]
   - Check if 5 ever assigned: NO → Create assignment
   - Check if 8 ever assigned: NO → Create assignment
   - Update Idea#103: SUBMITTED → UNDERREVIEW, stage=1
   - Log STATUS_CHANGE
   - Log STAGE_START
   - Notify: John, Alice
   - processed++

Result: processed = 3/3
Database Changes:
  - 6 rows added to assigned_reviewer_to_idea table
  - 3 rows updated in idea table (status, stage)
  - 6 rows added to user_activity table (STATUS_CHANGE + STAGE_START)
  - 4 rows added to notification table
```

---

## 3.2 ReviewerDecisionService

**Purpose:** Process reviewer decisions, handle SLA expirations, and resolve stages through matrix voting.

**Author:** Pavan

**Scope:** Central service for decision lifecycle and stage resolution logic.

### Class Structure

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewerDecisionService {
    // Constants
    private static final String EVENT_REVIEWER_DECISION = "REVIEWER_DECISION";
    private static final String EVENT_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String EVENT_STAGE_START = "STAGE_START";
    private static final String EVENT_MATRIX_OUTCOME = "MATRIX_OUTCOME";
    private static final String EVENT_MATRIX_OUTCOME_SLA = "MATRIX_OUTCOME_SLA";
    private static final String EVENT_DECISION_PENDING = "REVIEWER_DECISION_PENDING";
    private static final String EVENT_FINAL_DECISION = "FINAL_DECISION";

    // Dependencies
    private final IIdeaRepository ideaRepo;
    private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    private final IUserActivityRepository activityRepo;
    private final IReviewerCategoryRepository reviewerCategoryRepo;
    private final IUserRepository userRepo;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;
}
```

### Key Functions

#### Function 1: processDecision(ideaId, request)

**Purpose:** Handle a reviewer's decision submission for an idea.

**Permission:** Only REVIEWER role can call this

**Input Parameters:**
- `ideaId` (Integer) - The idea being reviewed
- `request` (ReviewerDecisionRequest) - Contains: decision, feedback

**Function Flow:**

```
1. VALIDATION PHASE
   a. Check if ideaId is not null
   b. Check if request body is provided
   c. Check if decision field is provided
   d. Check if feedback is provided (required)
   e. Check if feedback length <= 2000 characters
   
2. AUTHORIZATION PHASE
   a. Find idea by ideaId (throw if not found)
   b. Check idea is not deleted
   c. Get idea's current stage (throw if null)
   d. Get authenticated reviewer from JWT token
   e. Verify reviewer role = REVIEWER
   f. Find AssignedReviewerToIdea record for:
      - Current idea
      - Current reviewer
      - Current stage
      - Not deleted
      (throw if not found)
   
3. DECISION NORMALIZATION
   a. Convert input decision string to enum
   b. Validate decision is one of: ACCEPTED, REJECTED, REFINE
   c. Check if REFINE was already used (by matrix outcome)
   d. If trying to use REFINE and already used → throw error
   
4. IDEMPOTENCY CHECK
   a. Check if assignment.decision is already set
   b. If already set to same decision → ignore (return)
   c. If already set to different decision → throw error
   
5. SAVE DECISION
   a. Set assignment.decision = decision.name()
   b. Set assignment.feedback = feedback
   c. Save assignment to database
   d. Send notification to idea owner about feedback
   
6. TIMELINE LOGGING
   a. Log REVIEWER_DECISION activity
      - Award XP to reviewer
      - Don't award owner (individual vote, not final)
   b. Ensure STAGE_START marker exists (for SLA)
   
7. AUTO-RESOLUTION
   a. Check if all reviewers in stage have decided
   b. If all decided → resolve stage by matrix
   
8. Save updated idea
```

**Code Detail:**

```java
public void processDecision(Integer ideaId, ReviewerDecisionRequest req) {
    // Phase 1: Validation
    if (ideaId == null) 
        throw new ReviewerBadRequestException(
            "REV_IDEAID_REQUIRED", "ideaId is required");
    if (req == null) 
        throw new ReviewerBadRequestException(
            "REV_BODY_REQUIRED", "request body is required");
    if (req.getDecision() == null || req.getDecision().isBlank())
        throw new ReviewerBadRequestException(
            "REV_DECISION_REQUIRED", "decision is required");
    if (req.getFeedback() == null || req.getFeedback().isBlank()) {
        throw new ReviewerBadRequestException(
            "REV_FEEDBACK_REQUIRED", "feedback is required");
    }
    if (req.getFeedback().length() > 2000) {
        throw new ReviewerBadRequestException(
            "REV_FEEDBACK_TOO_LONG", "feedback must be <= 2000 chars");
    }

    // Phase 2a: Find idea
    Idea idea = ideaRepo.findById(ideaId)
        .orElseThrow(() -> new ReviewerNotFoundException(
            "REV_IDEA_NOT_FOUND", "Idea not found: " + ideaId));

    if (idea.isDeleted()) 
        throw new ReviewerBadRequestException(
            "REV_IDEA_DELETED", "Idea is deleted: " + ideaId);

    Integer stage = idea.getStage();
    if (stage == null) 
        throw new ReviewerBadRequestException(
            "REV_STAGE_NULL", "Idea stage is null");

    // Phase 2b: Get authenticated reviewer from JWT
    Integer resolvedId = getAuthenticatedUserId();
    User reviewer = userRepo.findById(resolvedId)
        .orElseThrow(() -> new ReviewerNotFoundException(
            "REV_USER_NOT_FOUND", 
            "Authenticated user not found: " + resolvedId));

    if (reviewer.getRole() == null || 
        reviewer.getRole() != Constants.Role.REVIEWER) {
        throw new ReviewerForbiddenException(
            "REV_ONLY_REVIEWER", "Only REVIEWER can submit decisions");
    }

    // Phase 2c: Find assignment
    AssignedReviewerToIdea assignment = reviewerAssignRepo
        .findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
            ideaId, reviewer.getUserId(), stage)
        .orElseThrow(() -> new ReviewerForbiddenException(
            "REV_NOT_ASSIGNED", 
            "Reviewer not assigned to current stage"));

    // Phase 3: Normalize decision
    Constants.IdeaStatus decision = normalizeReviewerDecision(
        req.getDecision(), ideaId);

    // Phase 4: Idempotency check
    if (assignment.getDecision() != null && 
        !assignment.getDecision().isBlank()) {
        
        if (assignment.getDecision().equalsIgnoreCase(decision.name())) {
            return;  // Duplicate → ignore
        }
        
        throw new ReviewerForbiddenException(
            "REV_DECISION_LOCKED",
            "Decision already submitted for this stage. Cannot change it.");
    }

    // Phase 5: Save decision
    assignment.setDecision(decision.name());
    assignment.setFeedback(req.getFeedback());
    reviewerAssignRepo.save(assignment);

    notificationHelper.notifyFeedbackPosted(idea, reviewer);

    // Phase 6: Timeline logging
    ReviewerTimelineUtil.logCurrentStatus(
        activityRepo, gamificationService, idea, reviewer, stage,
        decision, EVENT_REVIEWER_DECISION, true, false);

    ensureStageStartMarker(idea, stage, reviewer);

    // Phase 7: Auto-resolution
    tryResolveStageIfReady(idea, stage, reviewer);
    ideaRepo.save(idea);
}
```

**Key Points:**

- **JWT-Based:** Reviewer identity from JWT, not from request body (security)
- **Validation:** Complete input validation before processing
- **Idempotent:** Same request can be submitted multiple times safely
- **No Updates:** Decision can't be changed after submission (lock)
- **Auto-Resolution:** Triggered when all reviewers decide
- **Feedback Capture:** Full feedback text logged for future reference

**Example Scenario:**

```
Idea#101 is in UNDERREVIEW status, stage=1
Assigned Reviewers: John(#5), Alice(#8)

Request 1:
Reviewer John submits decision
- Input: ideaId=101, decision="ACCEPTED", feedback="Great idea!"
- Validation: All checks pass
- Get authenticated user from JWT: John (#5)
- Find assignment: Idea#101 + Reviewer#5 + Stage 1 → Found
- Normalize: "ACCEPTED" → ACCEPTED
- Check REFINE used: NO
- Idempotency check: assignment.decision = null → OK
- Save: assignment.decision = "ACCEPTED", feedback = "Great idea!"
- Log: REVIEWER_DECISION activity, award +50 XP to John
- Ensure STAGE_START marker exists
- Check if all decided: John=ACCEPTED, Alice=null → NO
- Status: Waiting for Alice's decision

Request 2:
Reviewer Alice submits decision
- Input: ideaId=101, decision="ACCEPTED", feedback="Excellent innovation!"
- Same process...
- Save: assignment.decision = "ACCEPTED"
- Log: REVIEWER_DECISION activity, award +50 XP to Alice
- Ensure STAGE_START marker exists
- Check if all decided: John=ACCEPTED, Alice=ACCEPTED → YES!
- Trigger: resolveStageByMatrix()
  * Count: accepted=2, rejected=0, refine=0
  * Result: MAJORITY ACCEPTED
  * Action: Advance to next stage (or finalize if last stage)
  * Update idea: stage=2 (if 2 stages exist) or ACCEPTED (if last)
  * Log: MATRIX_OUTCOME activity
  * Notify: Employee about stage completion
```

---

#### Function 2: expireStageDecisionsBySla()

**Trigger:** Scheduled daily at 23:30

**Purpose:** Mark reviewers as PENDING if they miss 3-day SLA deadline.

**Function Flow:**

```
1. FETCH UNDERREVIEW IDEAS
   a. Find all ideas with status = UNDERREVIEW and deleted = false
   
2. FOR EACH IDEA:
   a. Validate idea not null/deleted and has stage
   b. Get stage number
   c. Find first STAGE_START activity for this idea/stage
   d. Get stage start timestamp
   
   e. IF no STAGE_START marker exists:
      - Create it now (ensure marker exists)
      - Continue to next idea
   
   f. IF stage start + 3 days > now:
      - Not yet expired → skip to next idea
   
   g. IF stage start + 3 days <= now:
      - SLA expired → Process expired reviewers
      
      For each assigned reviewer in this stage:
        - If reviewer.decision is null/blank:
          * Mark as PENDING (assignment.decision = "PENDING")
          * Soft delete assignment (assignment.deleted = true)
          * Remove from idea's reviewer list
          * Calculate penalty XP (usually -100)
          * Log DECISION_PENDING activity with penalty
          * Notify reviewer about SLA expiration
        - Else: (already decided)
          * Skip (already submitted decision)
      
      h. After processing expired reviewers:
         - If any reviewers were marked PENDING:
           * Trigger stage resolution by matrix
           * Update idea status if needed
           * Save idea
```

**Code Detail:**

```java
@Scheduled(
    cron = "${reviewer.sla.cron:0 30 23 * * *}",
    zone = "${app.tz:Asia/Kolkata}"
)
public void expireStageDecisionsBySla() {
    List<Idea> underReview = ideaRepo.findByIdeaStatusAndDeletedFalse(
        Constants.IdeaStatus.UNDERREVIEW);
    LocalDateTime now = LocalDateTime.now();

    for (Idea idea : underReview) {
        if (idea == null || idea.isDeleted() || idea.getStage() == null) 
            continue;

        Integer stage = idea.getStage();

        // Find STAGE_START activity
        LocalDateTime stageStart = activityRepo
            .findFirstByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(
                idea.getIdeaId(), 
                stage, 
                Constants.ActivityType.CURRENTSTATUS, 
                EVENT_STAGE_START)
            .map(UserActivity::getCreatedAt)
            .orElse(null);

        // Ensure stage start marker exists
        if (stageStart == null) {
            User owner = idea.getUser();
            if (owner != null) 
                ensureStageStartMarker(idea, stage, owner);
            continue;
        }

        // Check if SLA expired (3 days)
        if (stageStart.plusDays(3).isAfter(now)) 
            continue;  // Not yet expired

        // SLA expired → Process expired reviewers
        List<AssignedReviewerToIdea> stageAssignments = 
            reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(
                idea.getIdeaId(), stage);

        boolean anyUpdated = false;

        for (AssignedReviewerToIdea a : stageAssignments) {
            // Only process reviewers who haven't decided
            if (a.getDecision() == null || a.getDecision().isBlank()) {
                // Mark as PENDING
                a.setDecision(Constants.IdeaStatus.PENDING.name());
                
                // Remove from active reviewers (soft delete)
                a.setDeleted(true);
                reviewerAssignRepo.save(a);

                // Award penalty XP to reviewer
                User reviewer = a.getReviewer();
                if (reviewer != null) {
                    int pendingDelta = 
                        gamificationService.getDeltaForIdeaStatus(
                            Constants.IdeaStatus.PENDING);
                    int penalty = pendingDelta == 0 ? 0 : -pendingDelta;

                    // Log DECISION_PENDING with penalty
                    ReviewerTimelineUtil.logCurrentStatusWithDelta(
                        activityRepo, gamificationService, idea, 
                        reviewer, stage,
                        Constants.IdeaStatus.PENDING, 
                        EVENT_DECISION_PENDING, 
                        penalty,  // Negative XP
                        true,     // award to reviewer (negative)
                        false);

                    // Notify reviewer of SLA expiration
                    notificationHelper.notifyReviewerSlaExpired(
                        idea, reviewer);
                }
                anyUpdated = true;
            }
        }

        // Resolve stage after SLA expiration
        if (anyUpdated) {
            User actor = idea.getUser();
            if (actor == null && !stageAssignments.isEmpty()) 
                actor = stageAssignments.get(0).getReviewer();
            if (actor != null) {
                resolveStageByMatrix(idea, stage, actor, true);
                ideaRepo.save(idea);
            }
        }
    }
}
```

**Key Points:**

- **3-Day Window:** stageStart + 3 days
- **Penalty XP:** Reviewer loses XP for missing deadline
- **Auto-Soft-Delete:** Removes reviewer from active pool
- **Matrix Resolution:** Immediately resolves stage with remaining votes
- **Notification:** Alerts reviewer of expiration
- **Timezone Aware:** Uses configurable timezone

**Example Scenario:**

```
Current Time: 23:30 on Day 4

Idea#101: stage=1, UNDERREVIEW
Stage Start: Day 1 at 10:00 AM
Duration: 4 days > 3 days → EXPIRED!

Assigned Reviewers:
  - John: decision = "ACCEPTED" (decided)
  - Alice: decision = null (not decided)
  - Bob: decision = null (not decided)

Execution:
1. Find stage start: Day 1 @ 10:00 AM
2. Check expiry: (Day 1 10:00) + 3 days = Day 4 10:00 AM
3. Is now (Day 4 23:30) after Day 4 10:00 AM? YES → Expired!
4. Find assignments: [John, Alice, Bob]
5. Process John: decision="ACCEPTED" → Skip (already decided)
6. Process Alice: decision=null → 
   - Set decision = "PENDING"
   - Set deleted = true
   - Get delta for PENDING = 0, so penalty = 0
   - Log DECISION_PENDING activity
   - Notify Alice of expiration
   - anyUpdated = true
7. Process Bob: decision=null →
   - Set decision = "PENDING"
   - Set deleted = true
   - Log DECISION_PENDING activity
   - Notify Bob of expiration
   - anyUpdated = true
8. Trigger resolveStageByMatrix():
   - Count active assignments: John (ACCEPTED), Alice (deleted), Bob (deleted)
   - Votes: accepted=1, rejected=0, refine=0, pending=2
   - Result: Only 1 active decision → Not enough for majority
   - Action: May stay UNDERREVIEW waiting or escalate based on policy
```

---

#### Function 3: resolveStageByMatrix()

**Purpose:** Apply voting matrix logic to determine stage outcome.

**Function Flow:**

```
1. GET ALL ASSIGNMENTS FOR STAGE
   a. Find all active assignments (deleted=false) for idea/stage
   
2. COUNT VOTES
   a. Count ACCEPTED votes (includes legacy APPROVED)
   b. Count REJECTED votes
   c. Count REFINE votes
   d. Total active votes = accepted + rejected + refine
   
   e. IF total votes == 0:
      - No one decided yet → return (wait for decisions)
   
3. APPLY MATRIX LOGIC
   
   IF accepted > rejected AND accepted > refine:
      # ACCEPTED majority
      Decision = ADVANCE TO NEXT STAGE (or FINALIZE if last stage)
   
   ELSE IF rejected >= accepted AND rejected >= refine:
      # REJECTED majority
      Decision = IDEA REJECTED (final)
   
   ELSE IF refine >= accepted AND refine >= rejected:
      # REFINE majority
      Decision = REQUEST REFINEMENT
      (Allow employee to resubmit)
   
4. SPECIAL HANDLING FOR REFINE
   - Check if REFINE was already used as outcome
   - If yes, block and escalate instead
   
5. PERSIST DECISION
   a. Update idea status based on outcome
   b. Update idea stage (increment or keep)
   c. Log MATRIX_OUTCOME activity
   d. Award XP based on outcome
   e. Notify stakeholders
   
6. FINAL DECISION DETECTION
   a. IF decision is ACCEPTED and this is last stage:
      - Set idea status = ACCEPTED (final)
      - Log FINAL_DECISION activity
      - Notify employee success
   
   b. IF decision is REJECTED:
      - Set idea status = REJECTED (final)
      - Log FINAL_DECISION activity
      - Notify employee rejection
```

**Code Detail (Simplified):**

```java
private void resolveStageByMatrix(Idea idea, Integer stage, 
                                   User actor, boolean slaTriggered) {
    List<AssignedReviewerToIdea> stageAssignments = 
        reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(
            idea.getIdeaId(), stage);

    // Count votes
    long accepted = stageAssignments.stream()
        .map(a -> a.getDecision() == null ? "" : 
             a.getDecision().trim().toUpperCase())
        .filter(d -> d.equals("ACCEPTED") || d.equals("APPROVED"))
        .count();
    
    long rejected = stageAssignments.stream()
        .map(a -> a.getDecision() == null ? "" : 
             a.getDecision().trim().toUpperCase())
        .filter(d -> d.equals("REJECTED"))
        .count();
    
    long refine = stageAssignments.stream()
        .map(a -> a.getDecision() == null ? "" : 
             a.getDecision().trim().toUpperCase())
        .filter(d -> d.equals("REFINE"))
        .count();

    long votes = accepted + rejected + refine;
    if (votes == 0) return;  // No decisions yet

    // Determine outcome
    Constants.IdeaStatus outcome;
    String event = slaTriggered ? 
        EVENT_MATRIX_OUTCOME_SLA : EVENT_MATRIX_OUTCOME;

    if (accepted > rejected && accepted > refine) {
        // ACCEPTED majority
        outcome = Constants.IdeaStatus.ACCEPTED;
        
        // Check if last stage
        if (isLastStage(idea)) {
            // Final approval
            idea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);
            ReviewerTimelineUtil.logCurrentStatus(
                activityRepo, gamificationService, idea, 
                actor, stage, outcome, event, 
                true, true);  // Award XP to both
            // Notify: Idea accepted!
        } else {
            // Move to next stage
            idea.setStage(stage + 1);
            idea.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
            // Will be auto-assigned at next EOD
        }
    } 
    else if (rejected >= accepted && rejected >= refine) {
        // REJECTED majority
        idea.setIdeaStatus(Constants.IdeaStatus.REJECTED);
        ReviewerTimelineUtil.logCurrentStatus(
            activityRepo, gamificationService, idea, 
            actor, stage, outcome, event, 
            false, false);  // No XP for rejection
        // Notify: Idea rejected
    }
    else if (refine >= accepted && refine >= rejected) {
        // REFINE majority
        outcome = Constants.IdeaStatus.REFINE;
        
        // Check if REFINE was already used as outcome
        boolean refineUsed = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndEventInAndDeletedFalse(
            idea.getIdeaId(),
            Constants.ActivityType.CURRENTSTATUS,
            Constants.IdeaStatus.REFINE,
            List.of(EVENT_MATRIX_OUTCOME, EVENT_MATRIX_OUTCOME_SLA));
        
        if (refineUsed) {
            // Can't refine again → escalate to admin?
            log.warn("REFINE already used for idea={}", idea.getIdeaId());
        } else {
            idea.setIdeaStatus(Constants.IdeaStatus.REFINE);
            // Employee can resubmit after refinement
        }
    }
}
```

**Example Scenario:**

```
Idea#101, Stage 1
Votes: John=ACCEPTED, Alice=ACCEPTED, Bob=REJECTED

Execution:
1. Get assignments: [John, Alice, Bob]
2. Count votes:
   - accepted = 2 (John, Alice)
   - rejected = 1 (Bob)
   - refine = 0
   - total = 3
3. Apply matrix:
   - Is accepted(2) > rejected(1)? YES
   - Is accepted(2) > refine(0)? YES
   - Result: ACCEPTED MAJORITY
4. Is this last stage? NO (assume 2 stages)
   - Action: Advance to next stage
   - Update idea: stage=2, status=UNDERREVIEW
   - Log MATRIX_OUTCOME
   - Notify employee: "Stage 1 complete, moved to Stage 2"
5. At next EOD (assignSubmittedIdeasEndOfDay):
   - Find ideas with stage=2
   - Auto-assign reviewers for stage 2
   - Continue review process
```

---

## 3.3 ReviewerDiscussionService

**Purpose:** Enable structured feedback discussions between reviewers on ideas.

**Author:** Pavan

**Scope:** Threaded commentary system within review stage context.

### Key Functions

#### Function 1: postDiscussion()

**Purpose:** Post a discussion comment/feedback in current stage.

**Input Parameters:**
- `ideaId` (Integer) - The idea being discussed
- `userId` (Integer) - User posting discussion
- `stageId` (Integer) - Current review stage
- `text` (String) - Comment text (max 2000 chars)
- `replyParent` (Integer, Optional) - Parent comment for threading

**Function Flow:**

```
1. VALIDATION
   a. ideaId required
   b. userId required
   c. stageId required
   d. text required and not blank
   e. text length <= 2000 chars
   
2. AUTHORIZATION
   a. Find user by userId (or throw)
   b. Find idea by ideaId (or throw)
   c. Check idea not deleted
   d. Check user role = REVIEWER only
   e. Check reviewer assigned to this idea + stage:
      - Query: assigned = true AND deleted = false
      - If not assigned → throw error
   
3. RESOLVE PARENT COMMENT
   a. If replyParent provided:
      - Find parent activity by ID
      - Validate parent exists and not deleted
      - Validate parent is in same idea
      - Validate parent is in same stage
      - Validate parent is REVIEWDISCUSSION type
   
4. CREATE ACTIVITY RECORD
   a. Get XP delta for REVIEWDISCUSSION (usually +25)
   b. Create UserActivity:
      - idea = idea object
      - user = reviewer object
      - commentText = text
      - activityType = REVIEWDISCUSSION
      - event = "FOLLOWUP"
      - stageId = stageId
      - replyParent = parent activity (if provided)
      - delta = XP delta
      - createdAt = now
      - deleted = false
   c. Save activity to database
   
5. AWARD XP
   a. If delta != 0:
      - Call gamificationService.applyDelta(userId, delta)
   
6. NOTIFICATION
   a. Notify idea owner about reviewer discussion
```

**Code Detail:**

```java
public void postDiscussion(Integer ideaId, Integer userId, 
                           Integer stageId, String text, 
                           Integer replyParent) {
    // Phase 1: Validation
    if (ideaId == null) 
        throw new ReviewerBadRequestException(
            "REV_IDEAID_REQUIRED", "ideaId is required");
    if (userId == null) 
        throw new ReviewerBadRequestException(
            "REV_USER_REQUIRED", "userId is required");
    if (stageId == null) 
        throw new ReviewerBadRequestException(
            "REV_STAGE_REQUIRED", "stageId is required");
    if (text == null || text.isBlank()) 
        throw new ReviewerBadRequestException(
            "REV_TEXT_REQUIRED", "text is required");
    if (text.length() > 2000) 
        throw new ReviewerBadRequestException(
            "REV_TEXT_TOO_LONG", "text must be <= 2000");

    // Phase 2a: Find entities
    Idea idea = ideaRepo.findById(ideaId)
        .orElseThrow(() -> new ReviewerNotFoundException(
            "REV_IDEA_NOT_FOUND", "Idea not found: " + ideaId));
    if (idea.isDeleted()) 
        throw new ReviewerBadRequestException(
            "REV_IDEA_DELETED", "Idea deleted");

    User user = userRepo.findById(userId)
        .orElseThrow(() -> new ReviewerNotFoundException(
            "REV_USER_NOT_FOUND", "User not found: " + userId));

    // Phase 2b: Check stage match
    if (!Objects.equals(idea.getStage(), stageId)) {
        throw new ReviewerBadRequestException(
            "REV_STAGE_MISMATCH", 
            "Discussion allowed only in current stage");
    }

    // Phase 2c: Check role
    if (user.getRole() == null || 
        user.getRole() != Constants.Role.REVIEWER) {
        throw new ReviewerForbiddenException(
            "REV_ONLY_REVIEWER", "Only REVIEWER can post discussion");
    }

    // Phase 2d: Check assignment
    boolean assigned = reviewerAssignRepo
        .existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
            ideaId, userId, stageId);
    if (!assigned) 
        throw new ReviewerForbiddenException(
            "REV_NOT_ASSIGNED", 
            "Reviewer not assigned to idea/stage");

    // Phase 3: Resolve parent
    UserActivity parent = resolveAndValidateParent(
        replyParent, ideaId, stageId);

    // Phase 4: Create activity
    int delta = gamificationService.getDeltaForActivity(
        Constants.ActivityType.REVIEWDISCUSSION);

    UserActivity discussion = UserActivity.builder()
        .idea(idea)
        .user(user)
        .commentText(text)
        .activityType(Constants.ActivityType.REVIEWDISCUSSION)
        .event("FOLLOWUP")
        .stageId(stageId)
        .replyParent(parent)
        .delta(delta)
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();

    activityRepo.save(discussion);

    // Phase 5: Award XP
    if (delta != 0) 
        gamificationService.applyDelta(userId, delta);

    // Phase 6: Notify
    notificationHelper.notifyReviewerDiscussion(idea, user);
}
```

**Key Points:**

- **Stage Lock:** Can only discuss in current stage
- **Assignment Check:** Must be assigned reviewer
- **Threading:** Support for nested replies
- **XP Award:** Immediate XP for engagement
- **Notification:** Alerts idea owner of feedback

**Example Scenario:**

```
Idea#101, Stage=1, UNDERREVIEW

Reviewers assigned: John, Alice

John's comment:
- POST /api/reviewer/ideas/101/discussions
- { userId: 5, stageId: 1, text: "This needs more details on ROI" }
- Validation: All pass
- Check: John assigned to 101/stage1? YES
- Create: UserActivity(idea=101, user=John, text="...", delta=25)
- Award: John gets +25 XP
- Notify: idea owner gets notification "John commented: ..."

Alice replies to John:
- POST /api/reviewer/ideas/101/discussions
- { userId: 8, stageId: 1, text: "Good point, agree", replyParent: 5 }
- (where 5 is the userActivityId of John's comment)
- Validation: All pass
- Check: Alice assigned to 101/stage1? YES
- Resolve parent: Find activity#5, verify same idea/stage/type
- Create: UserActivity(idea=101, user=Alice, text="...", replyParent=5, delta=25)
- Award: Alice gets +25 XP
- Notify: idea owner gets "Alice replied to John: ..."

Result Timeline:
- John's comment → Activity#100, delta=25
- Alice's reply → Activity#101, delta=25, replyParent=100
- Both reviewers visible to idea owner
- XP awarded: John +25, Alice +25
```

---

#### Function 2: getDiscussionsForStage()

**Purpose:** Retrieve all discussions for a specific idea stage.

**Input:** ideaId, stageId

**Output:** List<ReviewerDiscussionDTO>

**Code:**

```java
public List<ReviewerDiscussionDTO> getDiscussionsForStage(
    Integer ideaId, Integer stageId) {
    return activityRepo
        .findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalseOrderByCreatedAtAsc(
            ideaId, Constants.ActivityType.REVIEWDISCUSSION, stageId)
        .stream()
        .map(this::toDTO)
        .toList();
}
```

**Function:** Queries all REVIEWDISCUSSION activities for idea/stage, ordered by time, converts to DTO.

---

#### Function 3: getDiscussionsForStagePaged()

**Purpose:** Get paginated discussions (for UI with pagination).

**Supports:** page, size, sorting by createdAt

**Code:**

```java
public PagedResponse<ReviewerDiscussionDTO> getDiscussionsForStagePaged(
    Integer ideaId, Integer stageId, Pageable pageable) {
    Page<UserActivity> page = activityRepo
        .findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalse(
            ideaId, Constants.ActivityType.REVIEWDISCUSSION, 
            stageId, pageable);
    return buildPagedResponse(page.map(this::toDTO));
}
```

---

## 3.4 ReviewerTimelineUtil

**Purpose:** Utility class for logging all timeline activities related to reviewers.

**Author:** Pavan

**Scope:** Static helper methods for creating UserActivity records.

### Class Structure

```java
@UtilityClass  // Lombok annotation - static class
public class ReviewerTimelineUtil {
    // Static methods only
    public static void logCurrentStatus(...)
    public static void logCurrentStatusWithDelta(...)
    public static void logFinalDecision(...)
    public static void logProposalFinalDecision(...)
    public static void logProposalFinalDecisionWithDelta(...)
}
```

### Key Functions

#### Function 1: logCurrentStatus()

**Purpose:** Log a status/decision activity and optionally award XP.

**Parameters:**
- `activityRepo` - Repository to save activity
- `gamificationService` - For XP calculation
- `idea` - The idea being logged
- `actor` - User performing action
- `stageId` - Current stage
- `decision` - Status/decision (IdeaStatus enum)
- `event` - Event type (STATUS_CHANGE, REVIEWER_DECISION, etc.)
- `applyXpToActor` - Award XP to actor?
- `applyXpToOwner` - Award XP to idea owner?

**Function Flow:**

```
1. Null checks - return if any critical field is null

2. Normalize decision - Convert APPROVED → ACCEPTED

3. Get XP delta for the decision status

4. Create UserActivity record:
   - idea = idea object
   - user = actor
   - activityType = CURRENTSTATUS
   - decision = decision enum
   - event = event string
   - stageId = stageId
   - delta = calculated XP
   - commentText = event (for logging)
   - createdAt = LocalDateTime.now()
   - deleted = false

5. Save activity to database

6. Award XP:
   - If applyXpToActor && delta != 0:
     * Apply delta to actor's XP
   - If applyXpToOwner && delta != 0:
     * Apply delta to idea owner's XP
```

**Code:**

```java
public static void logCurrentStatus(
    IUserActivityRepository activityRepo,
    GamificationService gamificationService,
    Idea idea,
    User actor,
    Integer stageId,
    Constants.IdeaStatus decision,
    String event,
    boolean applyXpToActor,
    boolean applyXpToOwner) {

    if (idea == null || actor == null || decision == null) {
        return;
    }
    
    // Normalize APPROVED → ACCEPTED
    if (decision == Constants.IdeaStatus.APPROVED) {
        decision = Constants.IdeaStatus.ACCEPTED;
    }

    int delta = gamificationService.getDeltaForIdeaStatus(decision);

    UserActivity ua = UserActivity.builder()
        .idea(idea)
        .user(actor)
        .activityType(Constants.ActivityType.CURRENTSTATUS)
        .decision(decision)
        .event(event)
        .stageId(stageId)
        .delta(delta)
        .commentText(event)
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();

    activityRepo.save(ua);

    if (applyXpToActor && delta != 0) {
        gamificationService.applyDelta(actor.getUserId(), delta);
    }
    if (applyXpToOwner && delta != 0 && idea.getUser() != null) {
        gamificationService.applyDelta(idea.getUser().getUserId(), delta);
    }
}
```

**Usage Examples:**

```java
// Reviewer submits ACCEPTED decision (+50 XP to reviewer)
ReviewerTimelineUtil.logCurrentStatus(
    activityRepo, gamificationService, idea, reviewer, stage,
    Constants.IdeaStatus.ACCEPTED, 
    "REVIEWER_DECISION", 
    true,   // award XP to reviewer
    false   // don't award to owner yet (individual vote)
);

// Stage moves to UNDERREVIEW (+100 XP to owner?)
ReviewerTimelineUtil.logCurrentStatus(
    activityRepo, gamificationService, idea, owner, stage,
    Constants.IdeaStatus.UNDERREVIEW, 
    "STATUS_CHANGE", 
    true,   // award to owner
    true    // double award?
);

// Idea REJECTED by matrix (-200 XP penalty)
ReviewerTimelineUtil.logCurrentStatus(
    activityRepo, gamificationService, idea, actor, stage,
    Constants.IdeaStatus.REJECTED, 
    "MATRIX_OUTCOME", 
    false,  // no XP to actor
    true    // penalty to owner
);
```

---

#### Function 2: logCurrentStatusWithDelta()

**Purpose:** Log activity with explicitly provided XP delta (override calculated).

**Additional Parameter:**
- `deltaOverride` - Explicit XP value instead of calculated

**Usage:**

```java
// Reviewer marked PENDING due to SLA (-100 XP penalty)
ReviewerTimelineUtil.logCurrentStatusWithDelta(
    activityRepo, gamificationService, idea, reviewer, stage,
    Constants.IdeaStatus.PENDING, 
    "REVIEWER_DECISION_PENDING", 
    -100,   // explicit penalty
    true,   // award to reviewer (negative value)
    false
);
```

---

#### Function 3: logFinalDecision()

**Purpose:** Log the final decision for an idea (ACCEPTED or REJECTED only).

**Parameters:**
- `finalDecision` - Must be ACCEPTED or REJECTED (no REFINE/PENDING)

**Function Flow:**

```
1. Validate finalDecision is ACCEPTED or REJECTED (throw otherwise)

2. Get XP delta for FINALDECISION activity type

3. Create UserActivity:
   - activityType = FINALDECISION (not CURRENTSTATUS)
   - decision = finalDecision
   - event = provided event string
   - delta = XP delta

4. Save activity

5. Award XP based on applyXpToActor and applyXpToOwner flags
```

**Code:**

```java
public static void logFinalDecision(
    IUserActivityRepository activityRepo,
    GamificationService gamificationService,
    Idea idea,
    User actor,
    Integer stageId,
    Constants.IdeaStatus finalDecision,
    String event,
    boolean applyXpToActor,
    boolean applyXpToOwner) {

    if (finalDecision != Constants.IdeaStatus.ACCEPTED
            && finalDecision != Constants.IdeaStatus.REJECTED) {
        throw new IllegalArgumentException(
            "FINALDECISION must be ACCEPTED or REJECTED only");
    }

    int delta = gamificationService.getDeltaForActivity(
        Constants.ActivityType.FINALDECISION);

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

    activityRepo.save(ua);

    if (applyXpToActor && delta != 0) {
        gamificationService.applyDelta(actor.getUserId(), delta);
    }
    if (applyXpToOwner && delta != 0 && idea.getUser() != null) {
        gamificationService.applyDelta(idea.getUser().getUserId(), delta);
    }
}
```

---

#### Function 4: logProposalFinalDecision()

**Purpose:** Log proposal-related final decision events.

**Difference:** Works with Proposal entity instead of Idea status only.

---

## 3.5 ReviewerStageAssignmentService

**Purpose:** Manage assignment of reviewers to specific stages within categories.

**Scope:** Admin functionality for setting up reviewer expertise mappings.

### Key Functions

#### Function 1: getAvailableReviewersList()

**Purpose:** Get list of unassigned reviewers in a department.

**Input:** deptId

**Output:** List<AvailableReviewersDTO>

**Function Flow:**

```
1. Find all users with:
   - role = REVIEWER
   - deptId = provided deptId
   - deleted = false

2. Get all reviewers already assigned to any stage:
   - Query reviewerStageRepo
   - Extract userId from each assignment
   - Create set of assigned IDs

3. Filter unassigned:
   - Keep users NOT in assigned set

4. Convert to DTO:
   - Include: userId, name, email, deptName
   - Map using ModelMapper

5. Return list
```

**Example:**

```
Department#1 has reviewers: John(#5), Alice(#8), Bob(#12)

ReviewerCategory assignments:
  - John → IT Category, Stage 1
  - Alice → Not assigned

Result getAvailableReviewersList(1):
  - Bob (unassigned)
  - Alice (unassigned)
  - NOT John (already assigned)
```

---

#### Function 2: assignReviewerToStage()

**Purpose:** Assign a reviewer to a specific stage in a category.

**Input:**
- reviewerId
- categoryId
- stageNo (stage number, must be 1 to category.stageCount)

**Function Flow:**

```
1. VALIDATION
   a. Find category or throw
   b. Find reviewer or throw
   c. Check reviewer not already assigned anywhere:
      - If assigned → throw "already assigned" error
   d. Validate stageNo is within 1 to category.stageCount
   
2. CREATE ASSIGNMENT
   a. Create ReviewerCategory object:
      - category = category object
      - reviewer = reviewer object
      - assignedStageId = stageNo
      - deleted = false
   b. Save to database
   
3. LOG
   - Log: "Assigned Reviewer#X to Category#Y at Stage Z"
   
4. RETURN true
```

**Example:**

```
Request: Assign Reviewer#5 (John) to Category#3 (Mobile), Stage#2

Validation:
- Category#3 exists? YES (has stageCount=2)
- Reviewer#5 exists? YES (is REVIEWER role)
- Already assigned? Query shows John not in any assignment
- Stage 2 valid? 2 <= stageCount(2)? YES

Create:
- ReviewerCategory(
    category=Cat#3,
    reviewer=John#5,
    assignedStageId=2
  )

Database:
- Insert into reviewer_category

Log:
- "Assigned Reviewer 5, to Category 3, at Stage 2."

Return: true
```

---

#### Function 3: assignedReviewerDetails()

**Purpose:** Get list of all assigned reviewers with their assignment details.

**Output:** List<AssignedReviewerDTO>

**Function Flow:**

```
1. Query all active ReviewerCategory records (deleted=false)

2. For each assignment:
   - Convert to AssignedReviewerDTO containing:
     * Reviewer user ID, name
     * Category ID, name
     * Assigned stage number

3. Return list
```

---

## 3.6 ReviewerService (Coordinator)

**Purpose:** Main coordinator service that orchestrates all reviewer operations.

**Scope:** High-level API for reviewer functionality from controllers.

**Key Functions:**

1. **processDecision()** - Delegates to ReviewerDecisionService
2. **postDiscussion()** - Delegates to ReviewerDiscussionService
3. **getDiscussions()** - Delegates to ReviewerDiscussionService
4. **getDashboard()** - Delegates to ReviewerDashboardService

---

## 3.7 ReviewerDashboardService

**Purpose:** Generate personalized dashboard for reviewer showing pending ideas.

**Key Function: getReviewerDashboard(filter)**

**Filters:**
- ALL - Show all assigned ideas
- UNDERREVIEW - Only ideas in review stage
- ACCEPTED - Approved ideas
- REJECTED - Rejected ideas
- REFINE - Need refinement
- PENDING - Timed out

**Output:** List<ReviewerDashboardDTO> with:
- IdeaId, title, category, stage
- Current decision (if decided)
- Days remaining until SLA
- Reviewer count for stage
- Your decision status

---

## 4. DATA MODELS

### Entity: AssignedReviewerToIdea

```java
@Entity
@Table(name = "assigned_reviewer_to_idea")
public class AssignedReviewerToIdea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "reviewerId")
    private User reviewer;              // Who is reviewing

    @ManyToOne
    @JoinColumn(name = "ideaId")
    private Idea idea;                  // What is being reviewed

    @ManyToOne
    @JoinColumn(name = "categoryId")
    private Category category;          // Which category

    private Integer stage;              // Which stage (1, 2, 3, etc.)

    private String feedback;            // Reviewer's feedback text
    private String refine;              // Refinement notes
    private String decision;            // ACCEPTED, REJECTED, REFINE, PENDING

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;            // Soft delete flag
}
```

**Purpose:** Tracks which reviewer is assigned to review which idea at which stage.

---

### Entity: ReviewerCategory

```java
@Entity
public class ReviewerCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewer_category_id")
    private Integer reviewerCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryId")
    private Category category;          // Category reviewer can review

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewerId")
    private User reviewer;              // The reviewer

    @Column(name = "assignedStageId")
    private Integer assignedStageId;    // Which stage can they review

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
```

**Purpose:** Maps reviewer expertise (which categories and stages can each reviewer handle).

---

### Entity: UserActivity

```java
@Entity
@Table(name = "user_activity")
public class UserActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userActivityId;

    @ManyToOne
    @JoinColumn(name = "ideaId")
    private Idea idea;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    private String commentText;         // Text content

    @Enumerated(EnumType.STRING)
    private Constants.VoteType voteType;

    private boolean savedIdea;

    private String event;               // Event type: STATUS_CHANGE, REVIEWER_DECISION, etc.

    private int delta;                  // XP points awarded

    @ManyToOne
    @JoinColumn(name = "replyParentId")
    private UserActivity replyParent;   // For threaded comments

    @Nullable
    private Integer stageId;            // Which stage this activity occurred in

    @Enumerated(EnumType.STRING)
    private Constants.IdeaStatus decision;  // Decision status: ACCEPTED, REJECTED, REFINE, etc.

    @Enumerated(EnumType.STRING)
    private Constants.ActivityType activityType;  // COMMENT, VOTE, REVIEWDISCUSSION, CURRENTSTATUS, etc.

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
```

**Purpose:** Immutable audit trail of all activities and decisions.

---

## 5. REPOSITORY INTERFACES

### IAssignedReviewerToIdeaRepository

```java
public interface IAssignedReviewerToIdeaRepository 
    extends JpaRepository<AssignedReviewerToIdea, Integer> {

    // Find assignment for specific idea+reviewer+stage
    Optional<AssignedReviewerToIdea> 
    findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
        Integer ideaId, Integer userId, Integer stage);

    // Get all assignments for an idea/stage
    List<AssignedReviewerToIdea> findByIdea_IdeaIdAndStageAndDeletedFalse(
        Integer ideaId, Integer stage);

    // Check if assignment exists
    boolean existsByIdea_IdeaIdAndReviewer_UserIdAndStage(
        Integer ideaId, Integer userId, Integer stage);

    // Clear decisions for stage (when idea resubmitted)
    @Modifying
    @Query("UPDATE AssignedReviewerToIdea SET decision = null " +
           "WHERE idea.ideaId = :ideaId AND stage = :stage")
    void resetDecisionsForStage(@Param("ideaId") Integer ideaId,
                                 @Param("stage") Integer stage);
}
```

---

### IReviewerCategoryRepository

```java
public interface IReviewerCategoryRepository 
    extends JpaRepository<ReviewerCategory, Integer> {

    // Find active reviewers for category+stage
    @Query("SELECT rc.reviewer.userId FROM ReviewerCategory rc " +
           "WHERE rc.category.categoryId = :categoryId " +
           "AND rc.assignedStageId = :stage " +
           "AND rc.deleted = false")
    List<Integer> findActiveReviewerUserIdsByCategoryAndStage(
        @Param("categoryId") Integer categoryId,
        @Param("stage") Integer stage);

    // Get all assignments
    List<ReviewerCategory> findByDeletedFalse();

    // Check if reviewer already assigned
    boolean existsByReviewer_UserIdAndDeletedFalse(Integer reviewerId);

    // Find specific assignment
    Optional<ReviewerCategory> 
    findByReviewer_UserIdAndCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(
        Integer reviewerId, Integer categoryId, Integer stageNo);
}
```

---

### IUserActivityRepository

```java
public interface IUserActivityRepository 
    extends JpaRepository<UserActivity, Integer> {

    // Find discussions for idea/stage
    List<UserActivity> findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalseOrderByCreatedAtAsc(
        Integer ideaId, Constants.ActivityType type, Integer stageId);

    // Paginated discussions
    Page<UserActivity> findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalse(
        Integer ideaId, Constants.ActivityType type, Integer stageId, Pageable pageable);

    // Check if STAGE_START marker exists
    boolean existsByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalse(
        Integer ideaId, Integer stageId, Constants.ActivityType type, String event);

    // Find STAGE_START activity for SLA checking
    Optional<UserActivity> 
    findFirstByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(
        Integer ideaId, Integer stageId, Constants.ActivityType type, String event);

    // Check if REFINE was used as matrix outcome
    boolean existsByIdea_IdeaIdAndActivityTypeAndDecisionAndEventInAndDeletedFalse(
        Integer ideaId, Constants.ActivityType type, 
        Constants.IdeaStatus decision, List<String> events);
}
```

---

## 6. CONTROLLERS & API ENDPOINTS

### ReviewerController

**Base Path:** `/api/reviewer`

**Endpoints:**

1. **Submit Decision**
```
POST /api/reviewer/ideas/{ideaId}/decision
Content-Type: application/json

Request Body:
{
  "decision": "ACCEPTED",        // ACCEPTED, REJECTED, REFINE
  "feedback": "Excellent idea..."
}

Response:
200 OK - "Decision processed successfully"
400 Bad Request - Validation errors
403 Forbidden - Not assigned/not reviewer
404 Not Found - Idea not found
```

2. **Get Reviewer Dashboard**
```
GET /api/reviewer/me/dashboard?filter=ALL

Query Parameters:
- filter: ALL, UNDERREVIEW, ACCEPTED, REJECTED, REFINE, PENDING (default: ALL)

Response:
200 OK - [
  {
    ideaId: 101,
    title: "AI Integration",
    category: "Technology",
    stage: 1,
    ideaStatus: "UNDERREVIEW",
    yourDecision: null,
    daysRemaining: 2,
    reviewerCount: 3
  },
  ...
]
```

3. **Post Discussion**
```
POST /api/reviewer/ideas/{ideaId}/discussions
Content-Type: application/json

Request Body:
{
  "userId": 5,
  "stageId": 1,
  "text": "Need more details...",
  "replyParent": null  // Optional, parent activity ID for reply
}

Response:
200 OK - "Discussion posted"
400 Bad Request - Validation errors
403 Forbidden - Not reviewer/not assigned
404 Not Found - Idea or parent not found
```

4. **Get Discussions (Non-Paginated)**
```
GET /api/reviewer/ideas/{ideaId}/discussions?stageId=1

Response:
200 OK - [
  {
    userActivityId: 100,
    userId: 5,
    displayName: "John",
    commentText: "Need ROI details...",
    stageId: 1,
    replyParent: null,
    createdAt: "2025-03-07T10:30:00"
  },
  {
    userActivityId: 101,
    userId: 8,
    displayName: "Alice",
    commentText: "Agree, too vague",
    stageId: 1,
    replyParent: 100,
    createdAt: "2025-03-07T10:45:00"
  }
]
```

5. **Get Discussions (Paginated)**
```
GET /api/reviewer/ideas/{ideaId}/discussions/page?stageId=1&page=0&size=20&sort=createdAt,desc

Response:
200 OK - {
  content: [...],
  page: 0,
  size: 20,
  totalElements: 45,
  totalPages: 3,
  first: true,
  last: false
}
```

6. **Get Idea Progression**
```
GET /api/reviewer/ideas/{ideaId}

Response:
200 OK - {
  ideaId: 101,
  title: "...",
  stage: 1,
  ideaStatus: "UNDERREVIEW",
  timeline: [
    { event: "STATUS_CHANGE", decision: "UNDERREVIEW", createdAt: "..." },
    { event: "STAGE_START", decision: "UNDERREVIEW", createdAt: "..." },
    { event: "REVIEWER_DECISION", decision: "ACCEPTED", createdAt: "..." }
  ]
}
```

7. **Trigger EOD Assignment (Admin Only)**
```
POST /api/reviewer/jobs/assignments/eod

Response:
202 Accepted - "EOD assignment triggered"
403 Forbidden - Not admin
```

---

### ReviewerStageAssignmentController

**Base Path:** `/api/reviewerAssignment`

**Protected:** Admin/Superadmin only

**Endpoints:**

1. **Get Available Reviewers**
```
GET /api/reviewerAssignment/getAvailableReviewersList/{deptId}

Response:
200 OK - [
  { userId: 12, name: "Alice", email: "alice@...", deptName: "IT" },
  { userId: 15, name: "Bob", email: "bob@...", deptName: "IT" }
]
```

2. **Get Categories & Stage Count**
```
GET /api/reviewerAssignment/getCategoriesAndStageCountByCategory/{deptId}

Response:
200 OK - [
  { categoryId: 3, name: "Mobile", stageCount: 2 },
  { categoryId: 5, name: "Cloud", stageCount: 3 }
]
```

3. **Assign Reviewer to Stage**
```
POST /api/reviewerAssignment/assignReviewerToStage
Content-Type: application/json

Request Body:
{
  "reviewerId": 5,
  "categoryId": 3,
  "stageNo": 2
}

Response:
201 Created - "Reviewer assignment created successfully."
400 Bad Request - Validation errors
409 Conflict - Reviewer already assigned
```

4. **Get Assigned Reviewer Details**
```
GET /api/reviewerAssignment/assignedReviewerDetails

Response:
200 OK - [
  { 
    reviewerId: 5, 
    reviewerName: "John",
    categoryId: 3,
    categoryName: "Mobile",
    assignedStageId: 1
  },
  { 
    reviewerId: 8, 
    reviewerName: "Alice",
    categoryId: 3,
    categoryName: "Mobile",
    assignedStageId: 2
  }
]
```

5. **Remove Reviewer from Stage**
```
DELETE /api/reviewerAssignment/removeReviewerFromStage?reviewerId=5&categoryId=3&stageNo=1

Response:
200 OK - "Reviewer removed successfully"
404 Not Found - Assignment not found
```

---

## 7. COMPLETE WORKFLOW EXAMPLE

### Full Idea Review Journey

```
TIMELINE: Day 1-10

DAY 1, 08:00 - Employee submits idea
├─ Idea#101 created in DRAFT status
├─ Employee fills: title, description, category
├─ Employee clicks SUBMIT
└─ Action: IdeaService.submitIdea()
   ├─ Validate: owner, status=DRAFT
   ├─ Update status: SUBMITTED
   ├─ Award +100 XP to employee
   ├─ Log: SUBMITTED activity
   └─ Save idea

DAY 1, 23:59 - EOD Auto-Assignment Job
├─ ReviewerAssignmentService.assignSubmittedIdeasEndOfDay()
├─ Find: Idea#101 status=SUBMITTED
├─ Category: Mobile
├─ Find reviewers for Mobile, stage 1: [John#5, Alice#8]
├─ For each reviewer:
│  ├─ Create AssignedReviewerToIdea(idea=101, reviewer, stage=1)
│  ├─ Notify: "You've been assigned to review Idea#101"
│  └─ Award: Initial assignment notification
├─ Update: Idea#101 status=UNDERREVIEW, stage=1
├─ Log: STATUS_CHANGE + STAGE_START activities
└─ Save idea

DAY 2, 10:00 - John Starts Review
├─ Action: ReviewerController.postDiscussion()
├─ Request: ideaId=101, userId=5, stageId=1, text="Seems promising but ROI unclear"
├─ ReviewerDiscussionService.postDiscussion()
│  ├─ Validate: John assigned to 101/stage1
│  ├─ Create: UserActivity(type=REVIEWDISCUSSION, event=FOLLOWUP)
│  ├─ Award: +25 XP to John
│  └─ Notify: Employee "John commented on your idea"
└─ Save activity

DAY 2, 10:15 - Alice Replies
├─ Action: ReviewerController.postDiscussion()
├─ Request: ideaId=101, userId=8, stageId=1, text="Agree, needs ROI model", replyParent=100
├─ ReviewerDiscussionService.postDiscussion()
│  ├─ Validate: Alice assigned, replyParent valid
│  ├─ Create: UserActivity(type=REVIEWDISCUSSION, replyParent=100)
│  ├─ Award: +25 XP to Alice
│  └─ Notify: Employee and John
└─ Save activity

DAY 2, 14:00 - John Submits Decision
├─ Action: ReviewerController.submitDecision()
├─ Request: ideaId=101, decision="ACCEPTED", feedback="Great idea, strong team"
├─ ReviewerDecisionService.processDecision()
│  ├─ Validate: John is REVIEWER, assigned to 101/stage1
│  ├─ Normalize: "ACCEPTED" → ACCEPTED
│  ├─ Check REFINE used: NO
│  ├─ Save: assignment.decision="ACCEPTED", feedback="Great..."
│  ├─ Log: REVIEWER_DECISION activity
│  ├─ Award: +50 XP to John
│  ├─ Ensure: STAGE_START marker exists
│  ├─ Check: All decided? No (Alice pending)
│  └─ Save idea
└─ Notify: Employee "John's feedback: Great idea..."

DAY 2, 15:30 - Alice Submits Decision
├─ Action: ReviewerController.submitDecision()
├─ Request: ideaId=101, decision="ACCEPTED", feedback="Team capacity concerns"
├─ ReviewerDecisionService.processDecision()
│  ├─ Similar to John's
│  ├─ Save: assignment.decision="ACCEPTED"
│  ├─ Award: +50 XP to Alice
│  └─ Check: All decided? YES!
├─ AUTO-TRIGGER: tryResolveStageIfReady()
├─ ReviewerDecisionService.resolveStageByMatrix()
│  ├─ Get assignments: [John=ACCEPTED, Alice=ACCEPTED]
│  ├─ Count: accepted=2, rejected=0, refine=0
│  ├─ Result: ACCEPTED majority
│  ├─ Is last stage (stage 1 of 2)? NO
│  ├─ Action: Advance to stage 2
│  ├─ Update: Idea#101 stage=2, status=UNDERREVIEW
│  ├─ Log: MATRIX_OUTCOME activity
│  └─ Notify: Employee "Idea approved Stage 1, moving to Stage 2"
└─ Save idea

DAY 2, 23:59 - EOD Auto-Assignment Job (Stage 2)
├─ Find: Idea#101 stage=2, status=UNDERREVIEW
├─ Category: Mobile
├─ Find reviewers for Mobile, stage 2: [Carol#10]
├─ Create: AssignedReviewerToIdea(idea=101, reviewer=Carol, stage=2)
├─ Update: Idea#101 stage=2 (stays same)
├─ Log: STAGE_START activity for stage 2
└─ Notify: Carol "You've been assigned to review Idea#101 (Stage 2)"

DAY 3-5 - Carol Reviews Stage 2
├─ Similar to John/Alice review process
├─ Carol posts discussions
├─ Carol submits decision (assume ACCEPTED)
├─ All decided: YES!
├─ Resolve: Last stage (2 of 2), ACCEPTED
├─ Update: Idea#101 status=ACCEPTED
├─ Log: MATRIX_OUTCOME + FINAL_DECISION activities
└─ Notify: Employee "Idea ACCEPTED! Congratulations"

DAY 5 - Employee Creates Proposal
├─ Action: ProposalService.createProposal()
├─ From approved idea#101
├─ Create: Proposal with status=DRAFT
├─ Employee builds proposal details
└─ Employee submits (status=PROJECTPROPOSAL)

DAY 5-10 - Admin Reviews & Approves Proposal
├─ Admin reviews proposal
├─ Admin approves: status=APPROVED
├─ Award: +200 XP to employee
├─ Log: FINAL_DECISION activity for proposal
└─ Notify: Employee "Proposal approved! Project initiated"

FINAL STATE:
├─ Idea#101: status=ACCEPTED
├─ Proposal: status=APPROVED
├─ Employee: +100 (submit) +100 (stage1) +0 (stage2) +200 (proposal) = +400 XP
├─ John: +25 (comment) +50 (decision) = +75 XP
├─ Alice: +25 (comment) +50 (decision) = +75 XP
├─ Carol: +50 (decision) = +50 XP
└─ Timeline: 15+ activities logged in user_activity table
```

---

## 8. SLA & TIMEOUT HANDLING

### SLA Timeline

```
Stage Start (Day 1 10:00 AM)
    ↓
    → 24 hours: Day 2 10:00 AM (SLA 67% elapsed)
    → 48 hours: Day 3 10:00 AM (SLA 100% elapsed)
    → 72 hours: Day 4 10:00 AM (SLA EXPIRED, trigger action)

Daily Job at 23:30 PM checks:
    if (stageStart + 3 days <= now)
        → Mark undecided reviewers as PENDING
        → Award -100 XP penalty
        → Soft delete assignment
        → Resolve stage by remaining votes
```

### Penalty Calculation

```java
int pendingDelta = gamificationService.getDeltaForIdeaStatus(PENDING);
// Result: pendingDelta might be -100 (negative XP)

int penalty = (pendingDelta == 0) ? 0 : -pendingDelta;
// penalty = 0 or 100 (absolute value negated)

ReviewerTimelineUtil.logCurrentStatusWithDelta(
    ..., penalty, true, false
);
// Award reviewer with penalty (negative XP)
```

---

## 9. SECURITY CONSIDERATIONS

### Authentication Flow

```
1. Reviewer logs in with email/password
   ↓ JwtUtil generates token with userId, email, role claims

2. Reviewer makes request with Authorization: Bearer <token>
   ↓ JwtFilter intercepts request

3. JwtFilter validates token signature
   ↓ Extracts claims (userId, email, role)

4. SecurityContextHolder.setAuthentication(authObj)
   ↓ Request proceeds to controller

5. ReviewerDecisionService.getAuthenticatedUserId()
   ↓ Gets userId from SecurityContext (NOT from request body)

6. Reviewer identity is VERIFIED from JWT, not from user input
```

### Authorization Checks

```
1. @PreAuthorize("hasAuthority('REVIEWER')")
   - Only users with REVIEWER role allowed

2. Assignment verification:
   reviewerAssignRepo.findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse()
   - Confirms this reviewer is assigned to this idea/stage
   - Prevents unauthorized review attempts

3. Stage lock:
   if (!idea.getStage().equals(stageId))
       throw error
   - Can only discuss/decide on current stage
   - Prevents skipping stages

4. Decision lock:
   if (assignment.getDecision() != null)
       throw error
   - Can't change decision after submission
   - Prevents tampering

5. REFINE limit:
   if (refineUsed && decision == REFINE)
       throw error
   - REFINE allowed only once per idea
   - Prevents infinite refinement loops
```

---

## CONCLUSION

The Reviewer Module is a sophisticated system that:

1. **Automates** reviewer assignment based on expertise
2. **Tracks** every decision and discussion
3. **Enforces** SLA deadlines with penalties
4. **Resolves** stages through democratic voting matrix
5. **Gamifies** reviewer participation with XP points
6. **Logs** complete audit trail for compliance
7. **Secures** authorization through role-based access

Each service has clear responsibilities, functions are well-validated, and the system gracefully handles edge cases like missing reviewers, SLA expirations, and REFINE loops.

The use of scheduled jobs (@Scheduled) for EOD assignment and SLA checking makes the system scalable and reduces database query load during business hours.

