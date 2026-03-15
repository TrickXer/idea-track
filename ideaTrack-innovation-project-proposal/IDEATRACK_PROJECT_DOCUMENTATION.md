# IdeaTrack - Innovation & Project Proposal Management System
## Complete Project Documentation

---

## Table of Contents
1. Project Overview
2. System Architecture
3. Technology Stack
4. Core Modules & Features
5. Database Schema
6. API Endpoints
7. Security & Authentication
8. Business Logic Flow
9. Deployment & Configuration

---

## 1. PROJECT OVERVIEW

### Purpose
IdeaTrack is a comprehensive web-based system designed to capture, evaluate, and manage innovation ideas and project proposals within an organization. It provides a structured workflow for employees to submit ideas, reviewers to evaluate them through multiple stages, and admins to manage the entire innovation pipeline.

### Key Objectives
- Enable employees to submit innovative ideas across different categories
- Implement multi-stage review process with multiple reviewers per stage
- Track idea lifecycle from submission to project proposal creation
- Gamify the process with XP points and achievements
- Provide analytics and dashboards for insights
- Maintain audit trail through detailed activity logging

### Project Type
Spring Boot 4.0.1 microservice built with:
- Java 17
- Spring Data JPA
- MySQL Database
- JWT Authentication
- REST API Architecture

---

## 2. SYSTEM ARCHITECTURE

### Layered Architecture Pattern

The project follows a classic layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────┐
│              REST API Controllers                │
│  (Request Handling & Response Formatting)       │
├─────────────────────────────────────────────────┤
│         Business Logic Services Layer            │
│  (Core Business Rules & Workflows)              │
├─────────────────────────────────────────────────┤
│    Data Access Layer (Repositories)             │
│  (Database Queries & Persistence)               │
├─────────────────────────────────────────────────┤
│       Entity Layer (JPA Entities)                │
│  (Domain Models & Database Mapping)             │
├─────────────────────────────────────────────────┤
│         MySQL Database                          │
└─────────────────────────────────────────────────┘
```

### Key Components

#### 1. Controllers (REST API)
- Request validation
- Authentication checks
- Response formatting
- Route mapping

**Main Controllers:**
- `ReviewerController` - Reviewer operations
- `IdeaController` - Idea management
- `ProposalController` - Proposal management
- `AdminController` - Admin operations
- `AuthController` - Authentication
- `CategoryController` - Category management
- `NotificationController` - Notification endpoints

#### 2. Services (Business Logic)
Implement the core business rules and workflows:

**Key Services:**
- `ReviewerAssignmentService` - Auto-assign reviewers to submitted ideas
- `ReviewerDecisionService` - Handle reviewer decisions and SLA management
- `ReviewerDiscussionService` - Enable reviewer discussions on ideas
- `IdeaService` - Complete idea lifecycle management
- `ProposalService` - Project proposal creation and management
- `GamificationService` - XP points and achievements
- `NotificationHelper` - Send notifications to users

#### 3. Data Access Layer (Repositories)
JPA repositories for database operations:

**Key Repositories:**
- `IIdeaRepository` - Idea queries
- `IAssignedReviewerToIdeaRepository` - Reviewer assignments
- `IReviewerCategoryRepository` - Reviewer-category mappings
- `IUserRepository` - User queries
- `IUserActivityRepository` - Activity logging

#### 4. Entity Models
Domain objects mapped to database tables:

**Core Entities:**
- `User` - Application users with roles
- `Idea` - Innovation ideas submitted by employees
- `Category` - Idea categories
- `AssignedReviewerToIdea` - Tracks reviewer assignments per idea/stage
- `ReviewerCategory` - Maps reviewers to categories and stages
- `UserActivity` - Logs all user activities and decisions
- `Proposal` - Project proposals from approved ideas
- `Notification` - Notification messages
- `Department` - Organizational departments

---

## 3. TECHNOLOGY STACK

### Backend Framework
- **Spring Boot 4.0.1** - Application framework
- **Spring Data JPA** - ORM and data access
- **Spring Security** - Authentication and authorization
- **Spring Scheduling** - Scheduled tasks (cron jobs)

### Database
- **MySQL** - Relational database
- **JPA Auditing** - Automatic timestamp management

### Development Tools
- **Lombok** - Reduce boilerplate code
- **ModelMapper** - DTO mapping
- **JUnit 5 & Mockito** - Testing framework
- **Maven** - Build tool

### Additional Libraries
- **springdoc-openapi** - Swagger/OpenAPI documentation
- **MySQL Connector J** - Database driver
- **JWT (JSON Web Token)** - Token-based authentication

### Java Version
- **Java 17 LTS** - Long-term support version

---

## 4. CORE MODULES & FEATURES

### 4.1 USER MANAGEMENT MODULE

**Purpose:** Register, authenticate, and manage users with different roles.

**Key Features:**
- User registration with role assignment
- Email-based login and JWT authentication
- Role-based access control (RBAC)
- Profile management
- Department hierarchy

**User Roles:**
1. **EMPLOYEE** - Can submit ideas and vote/comment
2. **REVIEWER** - Can review ideas across assigned categories/stages
3. **ADMIN** - Can manage categories, assign reviewers, view analytics
4. **SUPERADMIN** - Full system access

**Database Table:** `user`
- userId (PK)
- name, email, password
- role (EMPLOYEE, REVIEWER, ADMIN, SUPERADMIN)
- departmentId (FK)
- status (ACTIVE, INACTIVE)
- createdAt, updatedAt, deleted

---

### 4.2 IDEA SUBMISSION & COLLABORATION MODULE

**Purpose:** Enable employees to submit ideas and collaborate through comments/votes.

**Key Features:**
- Create, edit, delete ideas (draft status)
- Submit ideas for review
- Add comments and votes
- Save/bookmark ideas
- Track idea status and lifecycle
- Idea categorization

**Idea Lifecycle Statuses:**
```
DRAFT 
  ↓ (submit)
SUBMITTED 
  ↓ (auto-assign reviewers)
UNDERREVIEW 
  ↓ (all reviewers decide)
ACCEPTED / REJECTED / REFINE
  ├─ ACCEPTED → PROJECTPROPOSAL → APPROVED
  ├─ REJECTED (end)
  └─ REFINE → SUBMITTED (resubmit after refinement)
```

**Database Tables:**
- `idea` - Core idea data
- `user_activity` - Comments, votes, activities
- `category` - Idea categories

**Key Services:**
- `IdeaService` - Create, update, submit, search ideas
- `UserActivityService` - Manage user interactions

---

### 4.3 REVIEW & APPROVAL WORKFLOW MODULE

**Purpose:** Orchestrate multi-stage evaluation of submitted ideas.

**Key Features:**
- Multi-stage review process (typically 2-3 stages per category)
- Assign reviewers to ideas per stage
- Collect structured feedback and decisions
- Enforce SLA timelines (3 days per stage)
- Automatic reviewer assignment at end of day
- Matrix-based decision resolution (majority voting)

**Review Decision Types:**
1. **ACCEPTED** - Reviewer approves the idea
2. **REJECTED** - Reviewer rejects the idea
3. **REFINE** - Request improvements (allowed once per idea)
4. **PENDING** - SLA expired without decision (auto-marked)

**Matrix Resolution Logic (for each stage):**
- If MAJORITY ACCEPTED → Move to next stage
- If MAJORITY REJECTED → Idea rejected
- If MAJORITY REFINE → Send back for refinement
- If SLA EXPIRED → Mark pending (penalty XP)

**Key Services:**
- `ReviewerAssignmentService` - Auto-assign reviewers (EOD scheduled job)
- `ReviewerDecisionService` - Process decisions and resolve stages
- `ReviewerDiscussionService` - Enable feedback discussions
- `ReviewerTimelineUtil` - Log activities and award XP

**Key Entities:**
- `AssignedReviewerToIdea` - Tracks which reviewer reviews which idea at which stage
- `ReviewerCategory` - Maps reviewer expertise (reviewer → category → stage)
- `UserActivity` - Logs decision history, discussions, timeline events

---

### 4.4 PROPOSAL MANAGEMENT MODULE

**Purpose:** Transform approved ideas into detailed project proposals.

**Key Features:**
- Create proposals from approved ideas
- Define project objectives, timelines, resources
- Track proposal status and progress
- Assign proposal owners and teams
- Project costing and budget tracking

**Proposal Lifecycle:**
```
APPROVED Idea → Create DRAFT Proposal
                    ↓
            PROJECTPROPOSAL Status
                    ↓
            Submit for Final Review
                    ↓
            APPROVED / REJECTED
```

**Database Table:** `proposal`
- proposalId (PK)
- ideaId (FK to approved idea)
- title, description, scope
- budget, resources, timeline
- status (DRAFT, PROJECTPROPOSAL, APPROVED, REJECTED)
- createdAt, updatedAt, deleted

**Key Services:**
- `ProposalService` - CRUD operations on proposals
- `ProposalReviewService` - Final proposal review workflow

---

### 4.5 GAMIFICATION MODULE

**Purpose:** Motivate user engagement through XP points and achievements.

**Key Features:**
- Award XP for idea submission, votes, comments
- Award XP for reviewer activities (decisions, discussions)
- Track user progression and rankings
- Leaderboard functionality
- Achievement badges

**XP System:**
- Idea Submission: +100 XP
- Idea Accepted: +500 XP (to owner)
- Idea Rejected: -200 XP (penalty)
- Review Decision: +50 XP (per decision)
- Discussion/Comment: +25 XP
- SLA Timeout: -100 XP (penalty for reviewer)

**Database Table:** `user_profile` (or extended User)
- userId (PK)
- totalXp, level, achievements
- badges, rankings

**Key Services:**
- `GamificationService` - Award/deduct XP
- `ProfileService` - User profile and progress
- `AnalyticService` - Rankings and leaderboards

---

### 4.6 NOTIFICATION MODULE

**Purpose:** Keep users informed of relevant activities and decisions.

**Key Features:**
- In-app notifications
- Email notifications (optional)
- Notification preferences management
- Real-time notification streaming
- Notification history

**Notification Types:**
- Idea assigned to reviewer
- Feedback posted on idea
- Stage completed (decision made)
- SLA expiration warning
- Idea approved/rejected
- Proposal status changes

**Database Table:** `notification`
- notificationId (PK)
- userId (FK)
- message, type, status
- relatedIdeaId, relatedProposalId
- createdAt, isRead

**Key Services:**
- `NotificationHelper` - Create and send notifications
- `NotificationService` - Manage notification preferences
- `ReactiveNotificationStreamService` - Real-time notifications

---

### 4.7 ANALYTICS MODULE

**Purpose:** Provide insights into innovation metrics and trends.

**Key Features:**
- Idea submission trends
- Review completion rates
- Approval/rejection statistics
- Department performance metrics
- User engagement analytics
- Proposal success rates

**Key Services:**
- `AnalyticService` - Generate analytics reports
- `AdminDashboardService` - Admin dashboard data
- `ReviewerDashboardService` - Reviewer-specific dashboard

---

## 5. DATABASE SCHEMA

### Core Tables

#### 1. User Table
```sql
CREATE TABLE user (
    userId INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role ENUM('EMPLOYEE', 'REVIEWER', 'ADMIN', 'SUPERADMIN'),
    deptId INT,
    status ENUM('ACTIVE', 'INACTIVE'),
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (deptId) REFERENCES department(deptId)
);
```

#### 2. Department Table
```sql
CREATE TABLE department (
    deptId INT PRIMARY KEY AUTO_INCREMENT,
    deptName VARCHAR(255),
    description TEXT,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN
);
```

#### 3. Category Table
```sql
CREATE TABLE category (
    categoryId INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    description TEXT,
    deptId INT,
    stageCount INT,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (deptId) REFERENCES department(deptId)
);
```

#### 4. Idea Table
```sql
CREATE TABLE idea (
    ideaId INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    description TEXT,
    problemStatement TEXT,
    userId INT,
    categoryId INT,
    stage INT,
    ideaStatus ENUM('DRAFT', 'SUBMITTED', 'UNDERREVIEW', 'ACCEPTED', 
                    'REJECTED', 'PROJECTPROPOSAL', 'APPROVED', 'REFINE', 'PENDING'),
    reviewerFeedback TEXT,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (categoryId) REFERENCES category(categoryId)
);
```

#### 5. AssignedReviewerToIdea Table
```sql
CREATE TABLE assigned_reviewer_to_idea (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ideaId INT,
    reviewerId INT,
    categoryId INT,
    stage INT,
    decision VARCHAR(50),
    feedback TEXT,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (ideaId) REFERENCES idea(ideaId),
    FOREIGN KEY (reviewerId) REFERENCES user(userId),
    FOREIGN KEY (categoryId) REFERENCES category(categoryId)
);
```

#### 6. ReviewerCategory Table
```sql
CREATE TABLE reviewer_category (
    reviewerCategoryId INT PRIMARY KEY AUTO_INCREMENT,
    reviewerId INT,
    categoryId INT,
    assignedStageId INT,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (reviewerId) REFERENCES user(userId),
    FOREIGN KEY (categoryId) REFERENCES category(categoryId)
);
```

#### 7. UserActivity Table
```sql
CREATE TABLE user_activity (
    userActivityId INT PRIMARY KEY AUTO_INCREMENT,
    ideaId INT,
    userId INT,
    commentText TEXT,
    voteType ENUM('UPVOTE', 'DOWNVOTE'),
    savedIdea BOOLEAN,
    event VARCHAR(100),
    delta INT,
    stageId INT,
    decision ENUM('ACCEPTED', 'REJECTED', 'REFINE', 'PENDING'),
    activityType ENUM('COMMENT', 'VOTE', 'SAVE', 'REVIEWDISCUSSION', 
                      'CURRENTSTATUS', 'FINALDECISION'),
    replyParentId INT,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (ideaId) REFERENCES idea(ideaId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (replyParentId) REFERENCES user_activity(userActivityId)
);
```

#### 8. Notification Table
```sql
CREATE TABLE notification (
    notificationId INT PRIMARY KEY AUTO_INCREMENT,
    userId INT,
    message TEXT,
    type VARCHAR(50),
    status VARCHAR(50),
    relatedIdeaId INT,
    relatedProposalId INT,
    createdAt TIMESTAMP,
    isRead BOOLEAN,
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (relatedIdeaId) REFERENCES idea(ideaId)
);
```

#### 9. Proposal Table
```sql
CREATE TABLE proposal (
    proposalId INT PRIMARY KEY AUTO_INCREMENT,
    ideaId INT,
    userId INT,
    title VARCHAR(255),
    description TEXT,
    scope TEXT,
    objectives TEXT,
    budget DECIMAL(10,2),
    timeline TEXT,
    resources TEXT,
    status ENUM('DRAFT', 'PROJECTPROPOSAL', 'APPROVED', 'REJECTED'),
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP,
    deleted BOOLEAN,
    FOREIGN KEY (ideaId) REFERENCES idea(ideaId),
    FOREIGN KEY (userId) REFERENCES user(userId)
);
```

---

## 6. API ENDPOINTS

### Authentication Endpoints
```
POST   /api/auth/register           - Register new user
POST   /api/auth/login              - Login and get JWT token
POST   /api/auth/refresh            - Refresh JWT token
```

### Idea Endpoints
```
POST   /api/ideas                   - Create new idea
GET    /api/ideas/{ideaId}          - Get idea details
PUT    /api/ideas/{ideaId}          - Update idea (draft/refine only)
DELETE /api/ideas/{ideaId}          - Delete idea (soft delete)
POST   /api/ideas/{ideaId}/submit   - Submit idea for review
GET    /api/ideas                   - Search/list ideas (paginated)
GET    /api/ideas/user/{userId}     - Get user's ideas
```

### Reviewer Endpoints
```
POST   /api/reviewer/ideas/{ideaId}/decision         - Submit review decision
GET    /api/reviewer/me/dashboard                    - Get reviewer dashboard
POST   /api/reviewer/ideas/{ideaId}/discussions      - Post discussion comment
GET    /api/reviewer/ideas/{ideaId}/discussions      - Get discussions
GET    /api/reviewer/ideas/{ideaId}/discussions/page - Get paginated discussions
GET    /api/reviewer/ideas/{ideaId}                  - Get idea progression
POST   /api/reviewer/jobs/assignments/eod            - Trigger EOD assignment (ADMIN)
```

### Reviewer Assignment Endpoints
```
GET    /api/reviewerAssignment/getAvailableReviewersList/{deptId}
GET    /api/reviewerAssignment/getCategoriesAndStageCountByCategory/{deptId}
POST   /api/reviewerAssignment/assignReviewerToStage
GET    /api/reviewerAssignment/assignedReviewerDetails
DELETE /api/reviewerAssignment/removeReviewerFromStage
```

### Proposal Endpoints
```
POST   /api/proposals               - Create proposal from approved idea
GET    /api/proposals/{proposalId}  - Get proposal details
PUT    /api/proposals/{proposalId}  - Update proposal
DELETE /api/proposals/{proposalId}  - Delete proposal
GET    /api/proposals               - List proposals (paginated)
```

### Admin Endpoints
```
GET    /api/admin/dashboard         - Admin dashboard
GET    /api/admin/users             - List users
PUT    /api/admin/users/{userId}    - Update user
DELETE /api/admin/users/{userId}    - Delete user
GET    /api/admin/categories        - List categories
POST   /api/admin/categories        - Create category
```

### Notification Endpoints
```
GET    /api/notifications           - Get notifications
POST   /api/notifications/{id}/read - Mark notification as read
DELETE /api/notifications/{id}      - Delete notification
```

---

## 7. SECURITY & AUTHENTICATION

### Authentication Flow

#### 1. User Registration
```
User → POST /api/auth/register
       (name, email, password, role)
       ↓
Service validates input
       ↓
Hash password with BCrypt
       ↓
Save user to database
       ↓
Response with userId
```

#### 2. User Login
```
User → POST /api/auth/login
       (email, password)
       ↓
Find user by email
       ↓
Verify password (BCrypt comparison)
       ↓
Generate JWT token (access + refresh)
       ↓
Response with token
```

#### 3. JWT Token Validation
```
Request with Bearer token
       ↓
JwtFilter extracts token
       ↓
JwtUtil validates token signature
       ↓
Extract user claims (userId, email, role)
       ↓
Create Authentication object
       ↓
Set in SecurityContextHolder
       ↓
Request proceeds to controller
```

### Authorization

**Role-Based Access Control:**
```
@PreAuthorize("hasAuthority('ADMIN')")           - Only ADMIN
@PreAuthorize("hasAuthority('REVIEWER')")        - Only REVIEWER
@PreAuthorize("hasAnyAuthority('ADMIN','REVIEWER')") - ADMIN or REVIEWER
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN')") - SUPERADMIN or ADMIN
```

### Key Security Classes

#### 1. JwtUtil - JWT Token Handling
- Generate token from user claims
- Validate token signature
- Extract claims (userId, email, role)
- Handle token expiration

#### 2. JwtFilter - Request Filtering
- Extract Bearer token from Authorization header
- Validate token
- Set authentication context
- Forward request to controller

#### 3. SecurityConfig - Spring Security Configuration
- Configure HTTP security
- Set authentication manager
- Configure password encoding (BCrypt)
- Enable CORS if needed

#### 4. MyUserDetailsService - User Loading
- Load user by email
- Convert User entity to UserDetails
- Used by Spring Security for authentication

---

## 8. BUSINESS LOGIC FLOW

### 8.1 Idea Submission Flow

```
1. Employee creates idea
   - Save in DRAFT status
   - Award +100 XP

2. Employee submits idea
   - Change status to SUBMITTED
   - Award +100 XP
   - Send notification to employee

3. End of Day (23:59 job trigger)
   - Find all SUBMITTED ideas
   - For each idea:
     * Find category
     * Find active reviewers for category + stage 1
     * Create AssignedReviewerToIdea records
     * Change idea status to UNDERREVIEW
     * Log STATUS_CHANGE and STAGE_START activities
     * Notify reviewers of assignment
```

### 8.2 Review Workflow

```
1. Reviewer reviews assigned idea
   - Verify reviewer is assigned to idea at current stage
   - Submit decision (ACCEPTED / REJECTED / REFINE)
   - Provide feedback
   - Log REVIEWER_DECISION activity
   - Award XP based on decision
   - Mark assignment.decision

2. System checks if all reviewers decided
   - Count ACCEPTED, REJECTED, REFINE votes
   - Apply matrix resolution logic:
     
     ┌─────────────────────────────────┐
     │ Matrix Resolution Logic         │
     ├─────────────────────────────────┤
     │ If ACCEPTED majority            │
     │   → Advance to next stage       │
     │   → If last stage               │
     │     * Set idea ACCEPTED         │
     │     * Log FINAL_DECISION        │
     │     * Notify employee success   │
     │                                  │
     │ If REJECTED majority            │
     │   → Set idea REJECTED           │
     │   → Log FINAL_DECISION          │
     │   → Notify employee rejection   │
     │                                  │
     │ If REFINE majority              │
     │   → Set idea REFINE             │
     │   → Allow employee to resubmit  │
     │   → Notify employee refinement  │
     │                                  │
     │ If SLA expired (3 days)         │
     │   → Mark as PENDING             │
     │   → Remove reviewers (soft delete)
     │   → Apply penalty XP            │
     │   → Resubmit to next available  │
     │     reviewers                   │
     └─────────────────────────────────┘

3. SLA Check (daily at 23:30)
   - Find all UNDERREVIEW ideas
   - For each idea:
     * Check stage start time
     * If > 3 days:
       - Mark non-decided reviewers as PENDING
       - Apply -100 XP penalty
       - Soft delete reviewer assignment
       - Trigger matrix resolution
       - Notify reviewers of SLA expiration
```

### 8.3 Reviewer Discussion Flow

```
1. Reviewer posts discussion/comment
   - Verify reviewer assigned to idea at stage
   - Save discussion with:
     * Text, userId, ideaId, stageId
     * Optional replyParent (threaded)
   - Award +25 XP
   - Notify idea owner

2. Get discussions
   - Fetch all REVIEWDISCUSSION activities
   - Filter by ideaId, stageId
   - Order by createdAt
   - Support pagination
```

### 8.4 Proposal Creation Flow

```
1. Idea reaches ACCEPTED status after all stages
   - Automatically create draft PROJECTPROPOSAL
   - Or manually create by employee

2. Employee builds proposal
   - Add title, description, scope
   - Define objectives, timeline
   - Estimate budget and resources
   - Save as DRAFT

3. Employee submits proposal
   - Change status to PROJECTPROPOSAL
   - Log proposal submission activity
   - Send notification to admins

4. Admin reviews and approves/rejects
   - Review proposal details
   - Approve → APPROVED status
   - Or Reject → REJECTED status
   - Award XP based on approval
   - Notify employee
```

---

## 9. DEPLOYMENT & CONFIGURATION

### Application Properties

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/ideatrack

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/ideatrack_db
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=your-secret-key-min-256-chars
jwt.expiration=86400000
jwt.refresh.expiration=604800000

# Timezone
app.tz=Asia/Kolkata

# Scheduler Cron Expressions
reviewer.assign.cron=0 59 23 * * *
reviewer.sla.cron=0 30 23 * * *

# Email Configuration (Optional)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-password
```

### Build & Run

```bash
# Using Maven
mvn clean install
mvn spring-boot:run

# Using mvnw (Maven Wrapper)
./mvnw clean install
./mvnw spring-boot:run

# Docker
docker build -t ideatrack:latest .
docker run -p 8080:8080 ideatrack:latest
```

### Database Setup

```bash
# Create database
mysql -u root -p
CREATE DATABASE ideatrack_db;
USE ideatrack_db;

# Hibernate will auto-create tables based on entities
# Or manually run SQL scripts in resources/sql/ folder
```

---

## 10. KEY DESIGN PATTERNS & BEST PRACTICES

### 1. Service Layer Pattern
- All business logic in service classes
- Controllers delegate to services
- Services are transactional (@Transactional)

### 2. Repository Pattern
- JPA repositories abstract database access
- Custom query methods for complex queries
- Lazy loading for performance

### 3. DTO (Data Transfer Object)
- Convert entities to DTOs for API responses
- Hide sensitive fields
- Decoupling domain from API

### 4. Soft Delete Pattern
- `deleted` column instead of hard delete
- Preserves audit trail
- Recoverable data

### 5. Activity Logging
- Log important business events in UserActivity table
- Enable audit trail and analytics
- Track status changes and decisions

### 6. Scheduled Tasks
- EOD reviewer auto-assignment
- Daily SLA checking
- Scalable batch processing

### 7. Security
- JWT token-based authentication
- Role-based access control
- Password hashing (BCrypt)

---

## CONCLUSION

IdeaTrack is a production-ready innovation management system that:
- Streamlines idea submission and evaluation
- Automates multi-stage review process
- Engages users through gamification
- Provides comprehensive analytics
- Maintains security and audit trails

The modular architecture allows for easy extension and maintenance of the system. Each module can be developed and tested independently while integrating seamlessly with the overall system.

