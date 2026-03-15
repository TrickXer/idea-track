
# IdeaTrack – Internal Innovation & Project Proposal Management System

> **Purpose:** A web-based system to capture, evaluate, and manage innovation ideas and project proposals within an organization.  
> **Scope:** This README describes each functional module and their core entities/features.

---

## Table of Contents
- User Management
- [IdeaSubmission & Collaboration
- Review & Approval Workflow
- Project Proposal Management
- [Innovation Analytics](#otifications & In-App Alerts

---

## User Management My Module

Handles user registration, authentication, and role-based access control.

**Key Features**
- Register as **Employee**, **Reviewer**, or **Admin**
- Login and manage personal profiles
- Enforce role-based permissions

**Core Entity**
- `User`
  - `UserID`
  - `Name`
  - `Role` (Employee | Reviewer | Admin)
  - `Email`
  - `Department`
  - `Status` (Active | Inactive)

---

## Idea Submission & Collaboration

Enables employees to submit ideas and collaborate via comments and votes.

**Key Features**
- Submit new ideas with title, description, and category
- Internal commenting and voting on ideas
- Track idea lifecycle status

**Core Entities**
- `Idea`
  - `IdeaID`
  - `Title`
  - `Description`
  - `CategoryID`
  - `SubmittedByUserID`
  - `SubmittedDate`
  - `Status` (Draft | UnderReview | Approved | Rejected)
-  `User_activity`
  - `userActivityId`
  - `IdeaId`
  - `UserId`
  - `CommentText`
  - `VoteTYpe`
  - `SavedIdea`
  - `Event`
  - `Delta` 
  - `RefinementMessage`
  - `ReplyParent`
  - `ActivityType`   
---

## Review & Approval Workflow

Orchestrates multi-stage evaluation of submitted ideas.

**Key Features**
- Assign reviewers to ideas
- Capture structured feedback and decisions
- Maintain review history

**Core Entity**
- `Review`
  - `ReviewID`
  - `IdeaID`
  - `ReviewerID`
  - `Feedback`
  - `Decision` (Approve | Reject)
  - `ReviewDate`

---

## Detailed Project Proposal Management

Transforms approved ideas into detailed project proposals with planning metadata.

**Key Features**
- Create proposals from approved ideas
- Define objectives, timelines, and resource needs
- Track proposal status and progress

**Core Entity**
- `Proposal`
  - `ProposalID`
  - `IdeaID`
  - `Title`
  - `Objectives`
  - `EstimatedBudget`
  - `TimelineStart`
  - `TimelineEnd`
  - `Status` (Draft | Submitted | Approved)

---

## Innovation Analytics

Provides insights and metrics on innovation activities.

**Key Features**
- Trend analysis for idea submissions
- Approval rate and participation metrics
- Department/category/period-based reporting

**Core Entity**
- `InnovationReport`
  - `ReportID`
  - `Scope` (Department | Category | Period)
  - `Metrics` (IdeasSubmitted, ApprovalRate, ParticipationCount)
  - `GeneratedDate`

---

## Notifications & In-App Alerts

Delivers in-app notifications for key events and updates.

**Key Features**
- Alerts for review decisions, comments, and proposal updates
- Read/unread status tracking

**Core Entity**
- `Notification`
  - `NotificationID`
  - `UserID`
  - `Type` (StatusChange | Feedback | Comment)
  - `Message`
  - `Status` (Unread | Read)
  - `CreatedDate`

  - `Hail Hydra`
---

## Intialize the repo

- git clone <clone url> 
- open this workspace in sts
- import the projects
- create `application.properties` in `IdeaTrackingMono\src\main\resources`
- and copy the values from `example.application.properties` and change the password to your local sql password
- and create the `ideaProject` in database