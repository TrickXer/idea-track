# IdeaTrack User Management Module Documentation

Unified system documentation for the IdeaTrack User Management module across frontend and backend layers. This document is written to serve two purposes at once:

- present the module clearly to clients and stakeholders
- act as a complete interview study guide for explaining the project end to end

Prepared on: March 15, 2026

## 1. Executive Summary

IdeaTrack is an internal innovation platform where employees submit ideas, reviewers evaluate them through stage-based workflows, and administrators manage governance, access, and operations. The User Management module is the trust layer of the platform. It controls who can enter the system, what each person is allowed to do, how profile data is maintained, and how user ownership is enforced across the larger idea and proposal lifecycle.

From a business perspective, this module solves a governance problem. Organizations cannot run a reliable innovation workflow unless identity, permissions, and account lifecycle are controlled. From a technical perspective, this module combines a React frontend with a Spring Boot backend, JWT-based authentication, role-based authorization, soft-delete account management, profile operations, and reviewer-stage assignment.

If you understand this document, you can explain:

- how users are created, authenticated, and authorized
- how frontend and backend responsibilities are split
- how admins and superadmins govern accounts
- how profiles, passwords, and profile photos are managed
- how reviewer assignment affects business workflow routing
- what security, validation, and scalability decisions shape the module

## 2. Business Context

Organizations often lose valuable ideas because workflows are fragmented across email, spreadsheets, chat messages, and informal approvals. IdeaTrack centralizes that process into one structured platform. The User Management module is foundational because every workflow depends on identity and controlled access.

### Business outcomes enabled by this module

- secure access to the platform
- clear role boundaries
- accountable user actions
- administrator control over onboarding and deactivation
- traceable account history through soft-delete and audit fields
- correct routing of reviewer responsibilities by department, category, and stage

### User roles in the system

| Role | Business responsibility | Typical capabilities |
| --- | --- | --- |
| `EMPLOYEE` | participates in innovation submission and follow-up | log in, manage own profile, create ideas, convert accepted ideas into proposals |
| `REVIEWER` | evaluates assigned ideas | log in, review ideas, participate in review discussions, submit decisions |
| `ADMIN` | operational governance | create and manage employees and reviewers, manage categories and assignments |
| `SUPERADMIN` | privileged governance | manage high-privilege accounts and broader system access boundaries |

## 3. System Overview

IdeaTrack is implemented as a browser-based React frontend backed by a Spring Boot modular monolith. The frontend handles user interaction, route protection, forms, notifications, and rendering. The backend handles business rules, persistence, authentication, security enforcement, validation, and audit-safe account lifecycle changes.

### End-to-end architecture flow

```text
Browser
  -> React SPA
    -> Providers and protected routes
      -> Feature pages and modal workflows
        -> Axios / SSE integration layer
          -> Spring Boot REST API
            -> Security filter chain
              -> Controllers
                -> Services
                  -> Repositories
                    -> MySQL database
```

### Architectural style by layer

| Layer | Style | Why it fits |
| --- | --- | --- |
| Frontend | feature-oriented SPA | keeps pages, components, and UI flows modular without overengineering |
| Backend | layered modular monolith | keeps business logic centralized and easy to debug while still separating concerns |
| Security | stateless JWT | simple API-friendly authentication model with role claims |
| Data access | Spring Data JPA repositories | concise persistence layer with readable query patterns |

## 4. What the User Management Module Includes

The module is broader than just an admin user list. In this project, user management includes identity, access, governance, self-service profile maintenance, and workflow ownership.

### Functional scope

- signup and login
- token-based session handling
- route-level and endpoint-level authorization
- admin and superadmin user CRUD operations
- department-aware user creation
- self-service profile management
- password change flow
- profile photo upload and deletion
- soft-delete user lifecycle handling
- reviewer assignment to categories and stages

### Why reviewer assignment belongs here

Reviewer-stage assignment is not just workflow configuration. It is a user-governance feature because it determines which reviewer account is responsible for a given review stage. That means it directly affects access, ownership, and operational accountability.

## 5. Frontend Architecture

The frontend is built with React 19, TypeScript, Vite, React Router, Axios, and React Context. It does not use Redux or React Query. Instead, it keeps global concerns small and stores most feature data locally in the page or modal that owns it.

### Frontend stack

| Area | Implementation | Purpose |
| --- | --- | --- |
| UI | React 19.2.0 | component rendering and state-driven UI |
| Language | TypeScript 5.9 | typed props, DTOs, safer API calls |
| Build | Vite 7 | fast development and bundling |
| Routing | `react-router-dom` 7.13 | protected routes and role-aware navigation |
| HTTP | Axios 1.13 | shared base client and interceptors |
| State | React Context + local state | auth, notifications, toasts globally; feature state locally |
| Forms | controlled state and `react-hook-form` | flexible admin and profile forms |
| Charts | Recharts | analytics pages |
| Styling | CSS, CSS modules, Bootstrap, inline styles | practical mixed styling approach |

### Frontend user-management locations

- `src/utils/authContext.tsx`: auth state, token decode, login, logout
- `src/components/auth/ProtectedRoute.tsx`: route guard
- `src/pages/admin/AdminConsole.tsx`: admin user management
- `src/pages/admin/SuperAdminConsole.tsx`: superadmin user management
- `src/components/user/RegisterUserModal.tsx`: create user flow
- `src/components/user/UserModal.tsx`: edit and delete user flow
- `src/pages/profile/ProfileHub.tsx`: self-service profile center
- `src/components/ProfileHub/OverviewTab.tsx`: profile editing
- `src/components/ProfileHub/SecurityTab.tsx`: password change and self-delete
- `src/pages/reviewer/ReviewerStageAssignment.tsx`: reviewer assignment shell
- `src/components/ReviewerStageAssignment/*`: assignment creation and display modules
- `src/utils/adminApi.ts`, `src/utils/profileApi.ts`, `src/utils/reviewerAssignmentApi.ts`: API integration

### Frontend runtime composition

The app shell composes providers in this order:

1. `AuthProvider`
2. `NotificationProvider`
3. `ToastProvider`
4. `BrowserRouter`

This means the entire route tree can access authentication state, real-time notifications, and toast feedback.

### Frontend state strategy

#### Global state

- auth token and decoded payload
- current user roles
- real-time notification state
- unread notification count
- toast queue

#### Local feature state

- user lists and search filters
- selected user for editing
- open and close state for modals
- profile edit forms
- reviewer assignment dropdown selections

This is a pragmatic design. The project avoids introducing a heavy global state library for data that is only relevant inside one feature screen.

### Frontend component responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| `AdminConsole` | manages reviewer and employee accounts | loads users, filters by tab, opens create and edit flows |
| `SuperAdminConsole` | manages higher-privilege account scope | similar structure with broader role allowance |
| `RegisterUserModal` | creates a new account | fetches departments, submits payload, shows temporary password |
| `UserModal` | updates or deletes a selected user | sends only changed fields and confirms destructive actions |
| `ProfileHub` | self-service account center | profile, achievements, activity, security |
| `OverviewTab` | edits profile attributes | validates user-facing profile inputs |
| `SecurityTab` | changes password and deletes own profile | contains password rules and delete confirmation |
| `ReviewerStageAssignment_Creation_Module` | assigns reviewers to stages | uses cascading dropdowns |
| `ReviewerStageAssignment_Display_Module` | displays and removes assignments | loads existing mappings and confirms removal |
| `ProtectedRoute` | enforces frontend access boundaries | checks auth presence and allowed roles |

### Frontend request pattern

```text
User action
  -> page or modal handler
    -> API utility call in src/utils
      -> backend response
        -> local state update
          -> toast feedback
            -> re-rendered UI
```

## 6. Backend Architecture

The backend is a Spring Boot modular monolith with a layered architecture. It is not split into microservices. That is a reasonable choice for this project because the user-management logic is closely related to profile, security, department, and review-governance logic.

### Backend layered structure

| Layer | Responsibility |
| --- | --- |
| Controller | expose REST endpoints |
| Service | enforce business rules and permissions |
| Repository | query and persist data |
| Entity and Model | represent database tables and relationships |
| Security | validate tokens and role access |
| Exception | standardize error responses |

### Core backend user-management components

#### Controllers

- `AuthController`: signup and login
- `AdminController`: admin and superadmin account management
- `ProfileController`: self-profile operations and public profile reads

#### Services

- `UserService`: create, update, delete, role rules, account lifecycle logic
- `ProfileService`: profile update, password change, profile photo handling
- `MyUserDetailsService`: integrates application users with Spring Security
- `UserProfileRules`: computes profile completeness logic

#### Repositories

- `IUserRepository`: user retrieval and role-aware filtering
- `IDepartmentRepository`: department lookup and active department access

#### Security components

- `SecurityConfig`: endpoint and role policies
- `JwtFilter`: validates bearer tokens on requests
- `JwtUtil`: creates and parses JWTs
- `AuthUtils`: resolves authenticated user identity inside secured flows

#### Error handling

- `GlobalExceptionHandler`: converts internal failures into consistent API responses

## 7. Authentication and Authorization Model

Authentication and authorization are split cleanly across frontend and backend responsibilities.

### Frontend responsibilities

- collect login credentials
- call login endpoint
- store returned JWT in localStorage
- decode roles for UI-level behavior
- protect routes with `ProtectedRoute`
- attach bearer token to Axios requests

### Backend responsibilities

- verify credentials
- hash passwords with BCrypt
- generate signed JWTs with role claims
- validate tokens on protected requests
- enforce endpoint and method-level role access
- block inactive or deleted users

### Login flow

```text
Frontend login form
  -> POST /api/auth/login
    -> backend verifies email and password
      -> backend issues JWT with role claims
        -> frontend stores token
          -> frontend decodes roles
            -> user redirected to role-appropriate route
```

### Signup and onboarding model

The system supports signup and also admin-created accounts. In admin-created flows, the backend enforces role rules, resolves the department, generates a strong temporary password, and persists audit metadata about who created the account.

### Authorization layers

| Layer | Where it happens | Purpose |
| --- | --- | --- |
| UI route protection | frontend `ProtectedRoute` | stops casual navigation to restricted screens |
| API path restrictions | backend `SecurityConfig` | defines coarse endpoint boundaries |
| fine-grained permission checks | backend service or `@PreAuthorize` rules | prevents invalid role escalation or unsafe operations |

### Why both frontend and backend checks matter

Frontend checks improve user experience and guide navigation. Backend checks provide actual security. The frontend alone is never trusted as the enforcement layer.

## 8. User Lifecycle Flows

This section is the most important part for client presentations and interviews because it explains what the module actually does at runtime.

### 8.1 Create user

#### Frontend flow

1. Admin opens `RegisterUserModal`.
2. The modal loads departments.
3. Admin enters name, email, department, and role.
4. The modal submits the payload through `createUser()` in `adminApi.ts`.
5. The UI shows the generated temporary password on success.
6. The user list refreshes.

#### Backend flow

1. `AdminController` receives `POST /api/admin/create`.
2. `UserService` validates operator permissions.
3. Email uniqueness is checked.
4. Department is resolved.
5. A strong temporary password is generated.
6. Password is BCrypt-hashed.
7. User is saved with audit metadata such as `createdByUser`.
8. Response returns the created account and temporary credential payload.

### 8.2 List users

#### Frontend flow

1. Admin console loads all users.
2. Tabs segment accounts by role.
3. Search filters visible users.

#### Backend flow

1. `AdminController` receives `GET /api/admin/all`.
2. `UserService` applies role-aware visibility logic.
3. Soft-deleted users are excluded from active listing.
4. Response is mapped to DTOs.

### 8.3 Update user

#### Frontend flow

1. Admin opens `UserModal` from a `UserCard`.
2. The modal fetches departments if needed.
3. Only changed fields are included in the update payload.
4. Success closes the modal and refreshes the console list.

#### Backend flow

1. `AdminController` receives `PUT /api/admin/{id}`.
2. `UserService` verifies that the target user exists.
3. Business rules validate whether the operator may manage the target role.
4. Selective field updates are applied.
5. Escalation to restricted roles is prevented.
6. Updated DTO is returned.

### 8.4 Delete user

#### Frontend flow

1. Admin clicks delete in `UserModal`.
2. `ConfirmationModal` asks for explicit confirmation.
3. On confirm, `deleteUser()` is called.
4. The list reloads after success.

#### Backend flow

1. `AdminController` receives `DELETE /api/admin/{id}`.
2. `UserService` checks permission to delete the target user.
3. Soft-delete is applied.
4. Account is marked `deleted=true` and `status=INACTIVE`.
5. Historical data is preserved for traceability.

### 8.5 Read and update own profile

#### Frontend flow

1. Logged-in user opens `ProfileHub`.
2. `fetchMyProfile()` loads current profile data.
3. `OverviewTab` edits name, phone, and bio.
4. `SecurityTab` changes password or requests self-delete.
5. Upload and delete profile-photo actions update the UI immediately.

#### Backend flow

1. `ProfileController` resolves the authenticated user.
2. `ProfileService` validates and updates allowed fields.
3. Password updates require current password validation and policy checks.
4. Profile photo uploads enforce file type, size, and path safety.
5. Self-delete applies soft-delete rather than hard deletion.

### 8.6 Login and role routing

#### Frontend flow

1. User submits credentials.
2. The frontend stores the JWT under `jwt-token`.
3. Roles are decoded.
4. The user is redirected based on role.

#### Backend flow

1. Email is located.
2. Password hash is verified.
3. Inactive or deleted accounts are blocked.
4. JWT is generated with subject and role claims.
5. `AuthResponse` is returned.

### 8.7 Reviewer-stage assignment

#### Frontend flow

1. Admin opens the Stage Assign area.
2. The creation module loads departments, categories, stages, and available reviewers.
3. Cascading dropdown selection prevents invalid combinations.
4. Assignment is submitted.
5. The display module shows existing mappings and supports confirmed removal.

#### Backend flow

The backend exposes reviewer-assignment endpoints that resolve valid reviewers, valid category-stage pairs, create mappings, list mappings, and remove mappings. This is where workflow ownership becomes concrete.

## 9. API Inventory

### Authentication

- `POST /api/auth/signup`
- `POST /api/auth/login`

### Admin user operations

- `GET /api/admin/all`
- `POST /api/admin/create`
- `PUT /api/admin/{id}`
- `DELETE /api/admin/{id}`
- `PATCH /api/users/{userId}/status`

### Profile operations

- `GET /api/profile/me`
- `PUT /api/profile/me`
- `DELETE /api/profile/me`
- `PUT /api/profile/me/password`
- `POST /api/profile/me/profile-photo`
- `DELETE /api/profile/me/profile-photo`
- `GET /api/profile/public/{userId}`
- `GET /api/profile/departments`
- `GET /api/profile/departmentID`

### Reviewer assignment operations

- reviewer-assignment list by department
- categories and stage-count lookup by department
- assign reviewer to stage
- load assigned reviewer details
- remove reviewer from stage

### Supporting frontend service modules

- `src/utils/adminApi.ts`
- `src/utils/profileApi.ts`
- `src/utils/reviewerAssignmentApi.ts`
- `src/utils/restApi.ts`
- `src/utils/axiosInterceptor.ts`

## 10. Data Model and Persistence Design

The user-management module depends on stable identity and audit-safe storage. The backend data model reflects that.

### Core entities

#### `user`

Stores identity, security, profile, role, and audit state, including:

- `userId`
- `email`
- `password` as BCrypt hash
- `role`
- `status`
- `deptId`
- `phoneNo`
- `profileUrl`
- `bio`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `deleted`

#### `department`

Stores department metadata and supports user-to-department association.

### Important relationship rules

- many users belong to one department
- one user may create another user in admin workflows
- user identity is referenced by ideas, proposals, notifications, and activity records

### Why soft-delete is important here

User accounts are tied to business history. Hard deletion would break traceability. Soft-delete preserves lineage while still deactivating operational access.

## 11. Security Design

This module is security-sensitive because it governs account creation, authentication, role scope, and identity-linked actions.

### Security controls in place

- JWT-based stateless authentication
- BCrypt password hashing
- role claims inside tokens
- backend token validation on protected requests
- endpoint-level and method-level authorization
- inactive and deleted account blocking
- profile photo upload validation
- client-side route guards for navigation control

### Password strategy

- passwords are never stored in plain text
- password changes require the current password
- weak or reused passwords are rejected
- admin-created users receive temporary strong passwords

### File upload security for profile photos

- MIME type whitelist
- extension whitelist
- file-size limits
- path normalization and traversal prevention
- safe replacement of old files

### Security tradeoff to understand

The frontend stores JWTs in localStorage. That is simple and common for SPAs, but it means XSS prevention remains important because a successful script injection could expose tokens.

## 12. Validation and Error Handling

Validation is split between frontend usability checks and backend trust enforcement.

### Frontend validation examples

- profile name, phone, and bio checks in `OverviewTab`
- password complexity feedback in `SecurityTab`
- department and role selection during account creation
- destructive confirmation before delete actions

### Backend validation examples

- duplicate email rejection
- role and operator permission validation
- department resolution checks
- password policy enforcement
- account-state validation during login and secured operations

### Error handling design

#### Frontend

- Axios response interceptors map common failures to readable toasts
- feature screens manage loading, empty, and failure states locally
- confirmation and retry patterns reduce accidental destructive actions

#### Backend

- `GlobalExceptionHandler` standardizes error payloads
- responses include status, message, path, and validation details where relevant

### Typical handled errors

- duplicate email
- unauthorized login
- forbidden actions
- invalid or expired token
- validation failure
- resource not found
- data integrity issues

## 13. Supporting UX Patterns

While not the core of user management, some surrounding frontend patterns materially improve the module.

### Toast feedback

`ToastContext` provides a global toast queue so create, update, delete, and validation events are surfaced clearly to the user.

### Real-time notifications

`NotificationContext` maintains a singleton SSE connection. This matters because user-facing system feedback remains live without opening duplicate notification streams when multiple notification components mount.

### Modal-driven workflows

The project uses modal-based create, edit, and delete flows instead of separate form pages. This keeps admin operations quick and focused.

## 14. Deployment and Runtime View

### Frontend runtime

- Vite-based React SPA
- communicates with backend through Axios and SSE
- current base URL in the frontend is hardcoded to `http://localhost:8091`

### Backend runtime

- single Spring Boot application
- MySQL database dependency
- no API gateway or service registry in the documented setup

### Typical run path

1. provision MySQL
2. configure backend properties
3. start the Spring Boot backend
4. run the frontend
5. users access the React application, which calls the backend directly

## 15. Architecture Strengths and Tradeoffs

### Strengths

- clear separation of frontend and backend responsibilities
- layered backend is easy to debug and explain
- React Context is used only where it adds value
- role-based governance is visible in both UI and API layers
- soft-delete model supports audit-friendly operations
- reviewer assignment extends access control into real workflow routing

### Tradeoffs

- all frontend routes are eagerly loaded today
- admin user lists do not yet use pagination
- localStorage JWT storage is simple but not the strongest session model
- frontend base URL is hardcoded instead of environment-driven
- the frontend has both `auth.ts` helpers and `authContext.tsx`, which creates some overlap

## 16. Client Presentation Summary

If you need to explain this module to a client, the cleanest framing is this:

The User Management module is the control center that keeps IdeaTrack secure, organized, and accountable. It ensures that the right people get the right access, that administrators can onboard and manage users safely, that employees can maintain their own profiles, and that reviewer responsibilities are assigned correctly to support the business workflow. The design is scalable because identity, permissions, profile operations, and workflow ownership are already separated into clear frontend and backend layers.

## 17. Interview Study Guide

This section is designed so you can explain the project confidently without needing separate notes.

### 2-minute explanation

IdeaTrack is a role-based innovation platform. The User Management module is responsible for authentication, authorization, user lifecycle operations, self-service profile management, and reviewer-stage assignment. On the frontend, React pages and modal components handle admin CRUD operations, profile editing, password changes, and route protection. On the backend, a Spring Boot layered architecture handles user creation, permission checks, JWT issuance and validation, password hashing, department resolution, and soft-delete account management. The design balances practical simplicity with solid governance and security controls.

### 5-minute deep explanation

At a high level, the module is split into frontend orchestration and backend enforcement. The frontend uses `AuthContext` to store the JWT and decode roles, then uses `ProtectedRoute` to control access to role-specific pages. Admins manage users through `AdminConsole` and `SuperAdminConsole`, which open modal components for user creation and editing. `RegisterUserModal` handles onboarding, including department selection and temporary-password display. `UserModal` handles partial updates and deletion through confirmation flows. For self-service account operations, `ProfileHub` allows the signed-in user to update profile details, change passwords, and manage profile photos. Reviewer-stage assignment is part of this same governance area because it decides which reviewer owns which review step.

On the backend, the request first passes through JWT validation in the security filter chain. Then controllers route the request to service methods, which enforce business rules and role constraints before calling repositories. Passwords are hashed with BCrypt, tokens carry role claims, and soft-delete ensures accounts can be deactivated without losing historical references. This layered model makes the system easier to secure, debug, and explain.

### Call flow you should be able to say aloud

```text
Login request
  -> AuthController
    -> User lookup and password verification
      -> JWT generation
        -> frontend stores token
          -> role-based redirect

Admin create user
  -> RegisterUserModal
    -> POST /api/admin/create
      -> UserService validates operator and role rules
        -> department resolution + temporary password generation
          -> BCrypt hash + save
            -> frontend refreshes user list
```

### Interview-ready technical highlights

- React Context is used for auth, notifications, and toasts, while feature data stays local
- JWT makes authentication stateless and easier to scale horizontally
- backend security is enforced at both route and business-rule levels
- soft-delete preserves audit history instead of breaking references
- reviewer assignment is a governance feature, not just a UI convenience
- modal-based admin workflows keep CRUD interactions fast and focused

### Good architecture decisions to defend

- choosing a modular monolith instead of microservices for a tightly related domain
- keeping the frontend simple instead of introducing Redux prematurely
- using both frontend and backend authorization checks for better UX and real security
- separating admin-managed account operations from self-service profile operations

### Limitations you can mention honestly

- add server-side pagination for larger user populations
- move frontend configuration like base URLs to environment variables
- consider refresh tokens and revocation strategy for stronger session handling
- add caching or request orchestration for heavier frontend data usage
- consider cloud object storage for profile photos in larger deployments

### Common interview questions and direct answers

#### Why JWT instead of server sessions?

JWT supports stateless authentication, which is simpler for APIs and easier to scale because each request carries its own authorization context.

#### How do you prevent unauthorized admin actions?

The frontend restricts access with protected routes, but the real protection happens on the backend through `SecurityConfig`, token validation, method-level or service-level role checks, and business rules about which operator can manage which target user.

#### Why use soft-delete for users?

Because user records are referenced by ideas, proposals, notifications, and audit history. Soft-delete preserves that lineage while still blocking future operational access.

#### What is the difference between admin user management and profile management?

Admin user management is governed account control over other users. Profile management is self-service editing by the currently authenticated user.

#### Why is reviewer-stage assignment part of user management?

Because it determines which reviewer account is responsible for which review stage. That is a user-governance decision tied directly to system permissions and accountability.

## 18. Improvement Roadmap

### Performance improvements

- paginate admin user lists
- add server-side filtering
- lazy-load larger frontend routes
- introduce smarter client-side data caching when needed

### Security improvements

- externalize secrets fully in backend runtime configuration
- add refresh-token and revocation strategy
- add login throttling and brute-force protection

### Scalability improvements

- move profile images to object storage
- add targeted database indexes on common filters such as `email`, `role`, `status`, `deleted`, and `deptId`

### Maintainability improvements

- remove duplicated auth helper responsibilities in the frontend
- standardize environment-based configuration for API hosts
- keep exception types more domain-specific as the backend grows

## 19. Final Conclusion

The IdeaTrack User Management module is the backbone of platform trust. It is not only a login feature and not only an admin CRUD screen. It combines identity, permission boundaries, self-service profile operations, reviewer responsibility mapping, and audit-safe lifecycle control. The frontend provides the operational workflows and user experience. The backend provides the real enforcement, persistence, and security guarantees. Together, they create a module that is understandable to clients, defensible in interviews, and strong enough to support the rest of the innovation platform.