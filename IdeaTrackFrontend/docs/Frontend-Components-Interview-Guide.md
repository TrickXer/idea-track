# IdeaTrack Frontend Components Interview Guide

This guide is a quick-reference catalog of the frontend TSX units in IdeaTrack. Use it to answer interview questions like:

- What is this component?
- What does it do?
- Where does it sit in the flow?

Prepared on: March 15, 2026

## How To Use This Guide

- Read Section A first for the app shell and global providers.
- Read Section B for route-level pages.
- Read Section C for reusable and feature components.
- For interview answers, use this 3-part pattern:
  - what it is
  - what data it uses
  - what user action it enables

## A. App Shell, Providers, and Entry Units

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/main.tsx` | App bootstrap | Mounts the React app into the DOM and starts the application runtime. |
| `src/App.tsx` | App route shell | Composes global providers and declares route tree with role-protected paths. |
| `src/utils/authContext.tsx` | AuthProvider and auth hook | Stores token and decoded roles, exposes `login/logout`, and drives auth state across the app. |
| `src/context/NotificationContext.tsx` | NotificationProvider | Maintains app-wide notification state with a singleton SSE connection and unread counters. |
| `src/context/ToastContext.tsx` | ToastProvider and toast API | Provides global toast queue for success, warning, and error feedback. |

## B. Route-Level Pages

### Admin Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/admin/AdminDashboard.tsx` | AdminDashboard | Shows admin KPI cards, quick actions, and recent operational insights. |
| `src/pages/admin/AdminConsole.tsx` | AdminConsole | Main admin user-management screen for employee/reviewer accounts and assignment access. |
| `src/pages/admin/SuperAdminConsole.tsx` | SuperAdminConsole | Extended user-management console for superadmin-level role governance. |

### Auth Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/auth/Login.tsx` | Login | Collects credentials, authenticates user, and routes by role after successful login. |
| `src/pages/auth/Signup.tsx` | Signup | Registers a new user account through the public onboarding flow. |

### Employee Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/employee/EmployeeDashboard.tsx` | EmployeeDashboard | Employee landing screen with personal idea/proposal status and shortcuts. |
| `src/pages/employee/AcceptedIdeasPage.tsx` | AcceptedIdeasPage | Lists accepted ideas that can be converted into proposal drafts. |
| `src/pages/employee/ProposalCreatePage.tsx` | ProposalCreatePage | Creates a proposal from selected/accepted idea context. |
| `src/pages/employee/DraftUpdatePage.tsx` | DraftUpdatePage | Updates an existing proposal draft with edited content and attachments. |

### Idea Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/idea/IdeaForm.tsx` | IdeaForm | Create/edit screen for drafting and submitting ideas with metadata and evidence. |
| `src/pages/idea/IdeaWall.tsx` | IdeaWall | Discovery/listing page for idea cards with search and filter interactions. |
| `src/pages/idea/IdeaHierarchy.tsx` | IdeaHierarchy | Visualizes idea review hierarchy/progression across reviewers and stages. |
| `src/pages/idea/editDraftForm.tsx` | Idea draft edit page | Alternate route page used to edit existing idea drafts. |

### Proposal Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/proposal/ProposalsPage.tsx` | ProposalsPage | Parent listing page for proposal records and statuses. |
| `src/pages/proposal/PendingProposals.tsx` | PendingProposals | Focused view for proposals currently awaiting review decisions. |
| `src/pages/proposal/ProposalReview.tsx` | ProposalReview | Detailed review UI for one proposal, including evidence and decision actions. |
| `src/pages/proposal/ReviewersOverdue.tsx` | ReviewersOverdue | Admin tracker for reviewers with overdue proposal actions. |
| `src/pages/proposal/HealthSummary.tsx` | HealthSummary | Operational health dashboard of proposal pipeline metrics. |

### Reviewer Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/reviewer/ReviewerDashboard.tsx` | ReviewerDashboard | Reviewer work queue and decision surface for assigned ideas. |
| `src/pages/reviewer/ReviewerIdeaPage.tsx` | ReviewerIdeaPage | Detailed reviewer view of an idea with discussion and decision context. |
| `src/pages/reviewer/ReviewerStageAssignment.tsx` | ReviewerStageAssignment page | Route shell for assignment creation and assignment display modules. |

### Analytics Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/analytics/AdminAnalytics.tsx` | AdminAnalytics | System-wide analytics for admin-level performance and distribution insights. |
| `src/pages/analytics/EmployeeAnalytics.tsx` | EmployeeAnalytics | Individual engagement/performance analytics for employee users. |
| `src/pages/analytics/ReviewerAnalytics.tsx` | ReviewerAnalytics | Reviewer-centric analytics on throughput and review behavior. |

### Other Route Pages

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/pages/landing/LandingPage.tsx` | LandingPage | Public marketing/intro page for product positioning and navigation entry. |
| `src/pages/notifications/AllNotifications.tsx` | AllNotifications | Full-page notification center with filters, pagination, and read-state updates. |
| `src/pages/profile/ProfileHub.tsx` | ProfileHub page | Main self-service profile page with tabbed account sections. |
| `src/pages/profile/ReportCreationAndDisplay.tsx` | ReportCreationAndDisplay | Creates and displays report artifacts in the profile/reporting flow. |
| `src/pages/editDraftForm.tsx` | Global draft edit page | Cross-feature draft edit route used in older/shared draft flows. |

## C. Reusable and Feature Components

### Layout and Navigation

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/layout/Layout.tsx` | Layout | Shared authenticated shell (sidebar/header/content) for protected pages. |
| `src/components/layout/Navbar.tsx` | Navbar | Top navigation bar with profile and quick-access actions. |
| `src/components/auth/ProtectedRoute.tsx` | ProtectedRoute | Route guard that blocks unauthorized users based on token and role rules. |

### User Management Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/user/UserCard.tsx` | UserCard | Presentational card/row for a single user in management lists. |
| `src/components/user/RegisterUserModal.tsx` | RegisterUserModal | Modal for creating users, selecting role/department, and showing temp password. |
| `src/components/user/UserModal.tsx` | UserModal | Modal for updating user details and performing confirmed delete operations. |
| `src/components/user/LeaderBoard.tsx` | LeaderBoard | Placeholder/stub leaderboard component reserved for future ranking view. |

### Reviewer Assignment Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/ReviewerStageAssignment/ReviewerStageAssignment_Creation_Module.tsx` | Assignment creation module | Creates reviewer-to-stage mappings using cascading department/category/stage selections. |
| `src/components/ReviewerStageAssignment/ReviewerStageAssignment_Display_Module.tsx` | Assignment display module | Displays current reviewer-stage mappings and supports removal with confirmation. |

### Idea Domain Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/idea/IIdea.tsx` | Idea type/view unit | TSX-based idea interface/view helper used by idea components. |
| `src/components/idea/IdeaCreateRequest.tsx` | IdeaCreateRequest model/view | Defines or renders idea-create request structure for form/API alignment. |
| `src/components/idea/IdeaCard.tsx` | IdeaCard | Compact visual card for one idea in list/wall contexts. |
| `src/components/idea/IdeaCardWall.tsx` | IdeaCardWall | Collection renderer for multiple idea cards with layout controls. |
| `src/components/idea/OwnerIdeaDetail.tsx` | OwnerIdeaDetail | Detailed idea view for idea owner actions and ownership-specific controls. |
| `src/components/idea/NonOnwerIdeaDetail.tsx` | NonOwnerIdeaDetail | Detailed idea view for non-owner users with read/review focus. |
| `src/components/idea/Timeline.tsx` | Idea timeline | Renders chronological activity/progression history for an idea. |

### Hierarchy Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/Hierarchy/ReviewerNode.tsx` | ReviewerNode | Node-level UI representation of reviewer decisions inside hierarchy tree. |
| `src/components/Hierarchy/Timeline.tsx` | Hierarchy timeline | Timeline view specialized for hierarchy/event progression visualization. |
| `src/components/Hierarchy/UserProfileModal.tsx` | UserProfileModal | Modal overlay showing user profile details within hierarchy interactions. |

### Notifications and Toasts

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/notifications/NotificationBell.tsx` | NotificationBell | Header bell showing unread count and dropdown previews from notification context. |
| `src/components/notifications/NotificationItem.tsx` | NotificationItem | Reusable row/card for one notification in compact and full lists. |
| `src/components/Toast/Toast.tsx` | Toast | Single toast renderer with type-based icons and auto-dismiss behavior. |

### ProfileHub Tab Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/ProfileHub/OverviewTab.tsx` | OverviewTab | Editable personal profile fields like name, phone, bio, and basic details. |
| `src/components/ProfileHub/ActivityTab.tsx` | ActivityTab | Timeline/feed of user activity history and engagement events. |
| `src/components/ProfileHub/AchievementsTab.tsx` | AchievementsTab | Displays badges/achievements and progress-style recognition indicators. |
| `src/components/ProfileHub/SecurityTab.tsx` | SecurityTab | Password update, account security actions, and self-delete confirmation flow. |

### Proposal Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/proposals/AcceptedIdeasList.tsx` | AcceptedIdeasList | Selectable list of accepted ideas eligible for proposal creation. |
| `src/components/proposals/ProposalCreateForm.tsx` | ProposalCreateForm | Main proposal creation form with objective/details input handling. |
| `src/components/proposals/DraftProposalEditor.tsx` | DraftProposalEditor | Editor for updating proposal draft content before submission. |
| `src/components/proposals/UpdateDraftProposalForm.tsx` | UpdateDraftProposalForm | Form wrapper dedicated to updating a stored draft proposal. |
| `src/components/proposals/DeleteDraftButton.tsx` | DeleteDraftButton | Focused action component to remove proposal drafts safely. |

### Reviewer Interaction Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/reviewer/DecisionForm.tsx` | DecisionForm | Captures reviewer decision inputs (approve/reject/comment) for an idea. |
| `src/components/reviewer/DiscussionForm.tsx` | DiscussionForm | Adds reviewer discussion messages/replies in review conversations. |
| `src/components/reviewer/DiscussionList.tsx` | DiscussionList | Displays threaded reviewer discussions attached to review items. |

### Admin Utilities and Shared Components

| File | Unit | Brief summary |
| --- | --- | --- |
| `src/components/admin/CategoryManagement.tsx` | CategoryManagement | Admin CRUD screen for category definitions and stage configuration. |
| `src/components/admin/BulkIdeaConsole.tsx` | BulkIdeaConsole | Bulk operations console for large-scale idea actions and CSV export. |
| `src/components/analytics/EngagementChart.tsx` | EngagementChart | Reusable chart component for monthly/yearly engagement visualizations. |
| `src/components/ConfirmationModal/ConfirmationModal.tsx` | ConfirmationModal | Shared confirmation dialog for destructive/high-risk actions. |

## D. Interview Fast Answers (One-Liners)

Use these when the interviewer points to a component and asks for purpose quickly.

- `ProtectedRoute`: Enforces frontend access boundaries by checking auth token and allowed roles.
- `AuthProvider`: Central source of truth for token, decoded payload, roles, and login/logout actions.
- `AdminConsole`: Main operational user-management screen for admins.
- `SuperAdminConsole`: Elevated user-governance screen with higher-role control.
- `RegisterUserModal`: Onboards new users with role/department assignment and temporary credentials.
- `UserModal`: Performs selective updates and safe deletion for existing users.
- `ProfileHub`: Self-service account center for profile, activity, achievements, and security operations.
- `SecurityTab`: Handles password updates and account-sensitive actions like self-delete.
- `ReviewerStageAssignment` modules: Manage which reviewer owns which stage in the review pipeline.
- `NotificationContext` and `NotificationBell`: Deliver and display real-time user notifications.

## E. Study Strategy Before Interview

1. Memorize Section A and all role-based pages in Section B.
2. For each component in Section C, remember one input and one output.
3. Practice explaining one complete flow:
   - Login -> protected route -> admin console -> register user -> list refresh.
4. Practice explaining one governance flow:
   - Reviewer assignment creation -> mapping display -> reassignment/removal.
5. Practice explaining one self-service flow:
   - ProfileHub overview edit -> security tab password change.

If you can explain those three flows with the units listed above, you can answer most component-level interview questions confidently.