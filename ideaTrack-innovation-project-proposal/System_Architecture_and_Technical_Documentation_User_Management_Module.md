# System Architecture and Technical Documentation - User Management Module

## Executive Summary

IdeaTrack is an internal platform that helps organizations collect ideas, review them, and turn approved ideas into proposals. The **User Management Module** is the foundation of this platform because it controls:

1. Who can enter the system.
2. What each person is allowed to do.
3. How user data is stored, protected, and updated.
4. How account activity remains auditable over time.

This document is written for client and stakeholder reading, while still preserving technical accuracy for developers.

---

## 1. Business Context and Purpose

Organizations often lose innovation opportunities because ideas are spread across emails, chats, and manual trackers. IdeaTrack solves this by centralizing the innovation lifecycle.

The User Management Module enables this lifecycle by ensuring every action in the system is tied to the right person, with the right permissions.

### Primary business outcomes

- Secure and role-based access to innovation workflows.
- Clear responsibility mapping (employee, reviewer, admin, superadmin).
- Reliable user onboarding and account governance.
- Compliance-friendly account history through soft-delete and audit fields.

---

## 2. Who Uses the System

### Employee

- Signs up and logs in.
- Manages own profile.
- Participates in idea-related workflows.

### Reviewer

- Logs in with reviewer permissions.
- Works within review flows and decision stages.

### Admin

- Creates and manages users.
- Controls account lifecycle for operational governance.

### Superadmin

- Has higher-level administrative control.
- Manages privileged role boundaries.

---

## 3. Architecture at a Glance

The project is a **modular monolith** built on Spring Boot. It is not a microservice-based system.

### Layered architecture

1. **Controller Layer**: REST API endpoints.
2. **Service Layer**: Business rules and permission logic.
3. **Repository Layer**: Data access through Spring Data JPA.
4. **Entity/Model Layer**: Database mapping.
5. **Exception Layer**: Standardized API error responses.

This structure ensures maintainability, separation of concerns, and predictable behavior.

---

## 4. Core User-Management Components

### Controllers

- `AuthController`: signup and login.
- `AdminController`: admin and superadmin user management.
- `ProfileController`: self-profile operations and public profile read.

### Services

- `UserService`: registration, admin create/update/delete, role constraints.
- `ProfileService`: profile update, password update, profile photo handling.
- `MyUserDetailsService`: maps app users to Spring Security user details.
- `UserProfileRules`: profile completion logic.

### Repositories

- `IUserRepository`: user queries and role-based filtering.
- `IDepartmentRepository`: department lookup and active lists.

### Security Components

- `SecurityConfig`: endpoint-level and role-level access rules.
- `JwtFilter`: token validation before protected access.
- `JwtUtil`: token creation, parsing, and expiration checks.
- `AuthUtils`: resolves authenticated user identity for secured flows.

---

## 5. End-to-End Request Workflow

Every secured request follows this sequence:

1. Client sends API request.
2. JWT is validated by `JwtFilter`.
3. Security context is populated with user and authorities.
4. Controller receives request.
5. Service validates business and permission rules.
6. Repository performs database read/write.
7. Service maps data to response DTO.
8. Controller returns structured JSON response.
9. If error occurs, `GlobalExceptionHandler` returns standardized error payload.

This creates a stable, predictable request lifecycle for both frontend and integration clients.

---

## 6. Authentication and Access Flow

### Signup flow (`POST /api/auth/signup`)

- Validates email uniqueness.
- Validates password policy.
- Resolves optional role/department.
- Hashes password with BCrypt.
- Persists active user account.

### Login flow (`POST /api/auth/login`)

- Finds user by email.
- Blocks inactive/deleted accounts.
- Verifies password hash.
- Builds user authorities.
- Generates JWT token with role claims.
- Returns token in `AuthResponse`.

### Authorization model

- Public endpoints remain explicitly open.
- Protected endpoints require valid JWT.
- Role rules enforced via `SecurityConfig` and `@PreAuthorize`.
- Admin APIs are restricted to `ADMIN` and `SUPERADMIN`.

---

## 7. User Lifecycle Management Flow (Admin View)

### Create user (`POST /api/admin/create`)

- Operator identity and permission validated.
- Target role constraints enforced.
- Email conflict check performed.
- Temporary strong password generated.
- Department resolved.
- User saved with metadata (`createdByUser`).

### List users (`GET /api/admin/all`)

- Visibility is role-aware.
- Deleted users excluded from active listing.

### Update user (`PUT /api/admin/{id}`)

- Verifies target exists and is manageable by operator role.
- Applies selective updates.
- Prevents restricted role escalation.
- Returns updated field summary.

### Delete user (`DELETE /api/admin/{id}`)

- Uses soft-delete strategy.
- Sets `deleted=true` and `status=INACTIVE`.
- Preserves historical records for traceability.

---

## 8. Profile Management Flow (User View)

### Read profile (`GET /api/profile/me`)

- Resolves current authenticated user.
- Returns profile plus gamification context.

### Update profile (`PUT /api/profile/me`)

- Validates fields such as name and phone pattern.
- Updates profile values safely.
- Recomputes completion logic.

### Upload profile photo (`POST /api/profile/me/profile-photo`)

- Allows only JPEG/PNG.
- Enforces file-size limit.
- Uses safe file path handling.
- Replaces previous profile photo when needed.

### Delete profile photo (`DELETE /api/profile/me/profile-photo`)

- Validates stored path boundaries.
- Removes file and clears profile URL.

### Change password (`PUT /api/profile/me/password`)

- Validates current password match.
- Rejects same-as-old password.
- Enforces complexity policy.
- Saves BCrypt hash only.

### Delete own profile (`DELETE /api/profile/me`)

- Soft-delete operation to preserve data lineage.

---

## 9. API Inventory (User-Management Scope)

### Authentication

- `POST /api/auth/signup`
- `POST /api/auth/login`

### Admin user operations

- `POST /api/admin/create`
- `GET /api/admin/all`
- `PUT /api/admin/{id}`
- `DELETE /api/admin/{id}`

### Profile operations

- `GET /api/profile/public/{userId}`
- `GET /api/profile/me`
- `PUT /api/profile/me`
- `POST /api/profile/me/profile-photo`
- `DELETE /api/profile/me/profile-photo`
- `PUT /api/profile/me/password`
- `DELETE /api/profile/me`
- `GET /api/profile/departments`
- `GET /api/profile/departmentID`

---

## 10. Data Model and Storage Design

### Primary tables in module scope

#### `user`

Stores identity, security, role, profile, and audit fields, including:

- `userId` (PK)
- `email` (unique)
- `password` (BCrypt hash)
- `role`, `status`
- `deptId` (FK to department)
- `phoneNo`, `profileUrl`, `bio`
- `createdByUserId` (self-reference)
- `createdAt`, `updatedAt`
- `deleted` (soft-delete)

#### `department`

Stores department metadata:

- `deptId` (PK)
- `deptName`
- audit timestamps
- `deleted`

### Relationship highlights

- Many users belong to one department.
- A user can be created by another user (`createdByUserId`).
- User IDs are referenced by idea, proposal, notification, and activity records.

---

## 11. Security Design

### Authentication

- Stateless JWT-based authentication.
- Password security through BCrypt.

### Token handling

- Secret key configured in properties.
- Token carries subject and roles.
- Expiration enforced in validation.

### Authorization

- Endpoint-level access policy in security configuration.
- Method-level access via annotations.
- Inactive/deleted users are denied operational access.

### Security posture advantages

- No plain text password storage.
- Centralized role enforcement.
- Strong account-state validation before granting access.

---

## 12. Error and Validation Strategy

`GlobalExceptionHandler` standardizes error responses across business, validation, and security failures.

### Response structure includes

- Timestamp
- HTTP status
- Error name
- Message
- Request path
- Optional field-level validation map

### Typical handled cases

- Duplicate email
- Unauthorized login
- Forbidden actions
- Invalid or expired token
- Validation failures
- Resource not found
- Data integrity issues

This consistency improves client integration reliability and support diagnostics.

---

## 13. Deployment and Runtime Overview

### Current deployment model

- Single Spring Boot backend runtime.
- MySQL database dependency.
- No API gateway or service registry in this repository.

### Typical run path

1. Provision MySQL.
2. Configure `application.properties`.
3. Start backend with Maven wrapper.
4. Client applications call backend directly on configured host/port.

---

## 14. Developer and Support Onboarding Notes

### Recommended understanding path

1. Review controllers to map API surfaces.
2. Review services for permission and business rules.
3. Review repositories for data access behavior.
4. Review `SecurityConfig`, `JwtFilter`, and `JwtUtil` for auth flow.
5. Review `GlobalExceptionHandler` for client-visible error semantics.

### Useful debug trail for user-management issues

- Token parsing and authority extraction.
- Role mismatches on protected routes.
- Account state (`status`, `deleted`) checks.
- Department resolution and email uniqueness constraints.
- Password policy violations.

---

## 15. Client-Friendly Business Explanation

The User Management Module ensures that the right people access the right parts of IdeaTrack.

In practical business terms, it provides:

- Secure onboarding and login.
- Controlled permission boundaries by role.
- Fast account operations for admins.
- Self-service profile maintenance for users.
- Safe account deactivation without losing history.

This module is the trust layer that makes the larger innovation workflow dependable for enterprise use.

---

## 16. Improvement Roadmap

### Performance

- Expand pagination in user-heavy endpoints.
- Add targeted database indexing on common filters (`email`, `role`, `status`, `deleted`, `deptId`).

### Scalability

- Move profile image storage to cloud object storage.
- Add caching for frequently requested directory/profile data.

### Security

- Externalize secrets to environment/secret manager.
- Introduce refresh token lifecycle and token revocation strategy.
- Add login rate limiting and brute-force controls.

### Maintainability

- Replace generic runtime exceptions with more granular domain exceptions.
- Resolve test-resource merge markers and streamline generated artifact handling.

---

## Appendix: Analysis Coverage Statement

- Workspace read verification completed.
- Authored source/document/config/test files reviewed.
- Technical interpretations based on current repository state.

---

## 17. Interview Technical Deep Dive (Presentation-Ready)

This section is designed for technical interviews where you need to explain not only "what" the module does, but also "how" and "why" it is implemented this way.

### 17.1 Runtime Call Flow (Controller -> Service -> Repository)

Example: `POST /api/auth/login`

1. Request reaches Spring MVC and maps to `AuthController.login`.
2. `UserService.findByEmail` loads user by unique email.
3. Business guard checks account state (`ACTIVE`, not `deleted`).
4. BCrypt compares plaintext input to stored hash.
5. `MyUserDetailsService.loadUserByUsername` creates `UserDetails` with authorities.
6. `JwtUtil.generateToken` signs JWT with `roles` claim and expiration.
7. `AuthResponse` is returned.

Why this matters in interviews:

- It shows separation of authentication concerns.
- It demonstrates secure password handling and stateless token issuance.
- It avoids exposing entity internals directly to clients.

### 17.2 Security Filter Chain Internals

For each protected request:

1. `JwtFilter` checks `Authorization` header for `Bearer` token.
2. Token is parsed; subject (email) and roles are extracted.
3. `MyUserDetailsService` validates user exists and is enabled.
4. `SecurityContext` is populated with `UsernamePasswordAuthenticationToken`.
5. Endpoint and method-level authorization checks run.

Key technical point:

- This design prevents session dependency and supports horizontal scaling because authorization state is derived from the JWT per request.

### 17.3 Authorization Enforcement Layers

The module enforces access using two layers:

1. Path-level rules (`SecurityConfig`):
	- Public: `/api/auth/**`, selected profile lookup endpoints.
	- Restricted: `/api/admin/**` requires `ADMIN` or `SUPERADMIN`.
2. Method-level rules (`@PreAuthorize`):
	- Fine-grained role guards directly on controller methods.

Interview-ready explanation:

- Path-level rules provide coarse security boundaries.
- Method-level checks provide precise business authorization.
- Using both reduces accidental privilege exposure.

### 17.4 Data Integrity and State Modeling

`User` uses a soft-delete model with two independent controls:

1. `deleted` flag for archival/logical deletion.
2. `status` enum (`ACTIVE`, `INACTIVE`) for operational usability.

Why both are useful:

- `deleted=true` preserves historical references.
- `status=INACTIVE` can suspend operations without full deletion.

Self-referential relationship:

- `createdByUserId` allows auditing account provenance (who created which user).

### 17.5 Password and Credential Strategy

Password strategy includes:

1. BCrypt hashing for all stored passwords.
2. Complexity validation (uppercase, lowercase, digit, special character).
3. Rejection of weak and reused passwords.
4. Temporary password generation for admin-created accounts.

Interview talking point:

- Hashing + strength policy addresses both breach impact and account hardening.

### 17.6 File Upload Security (Profile Photos)

The profile photo flow is implemented with explicit hardening:

1. MIME type whitelist (`image/jpeg`, `image/jpg`, `image/png`).
2. Extension whitelist (`.jpg`, `.jpeg`, `.png`).
3. File size limit (5 MB).
4. Path normalization and traversal prevention.
5. Safe replacement of old file and URL update.

Why it is interview-relevant:

- Demonstrates awareness of real-world file upload attack vectors.

### 17.7 Transaction and Consistency Considerations

Service methods are transaction-scoped where state mutation is involved.

Consistency goals:

1. User writes and related rule validations complete atomically.
2. No partial profile/photo/password updates.
3. Business exceptions prevent invalid state transitions.

### 17.8 Error Semantics and API Contract Stability

`GlobalExceptionHandler` converts internal exceptions into deterministic API semantics.

Benefits:

1. Stable error contract for frontend and external consumers.
2. Faster issue diagnosis using standardized payload fields.
3. Cleaner controller/service code by centralizing translation logic.

### 17.9 Performance Notes to Mention in Interview

Current strengths:

1. Stateless auth avoids server-side session lookup overhead.
2. Repository method naming + JPA queries keep persistence code concise.
3. DTO projections reduce unnecessary data exposure.

Current limits and improvements:

1. More endpoints should be paginated for large user datasets.
2. Indexing should be strengthened on `email`, `role`, `deleted`, `status`, `deptId`.
3. Photo storage should move to object storage for multi-instance scaling.

### 17.10 Architecture Tradeoffs (Good Interview Material)

Why modular monolith was effective here:

1. Simpler deployment and debugging compared to early microservices.
2. Faster delivery with shared transaction boundary.
3. Lower operational complexity.

Tradeoff to acknowledge:

1. As scale and team size grow, bounded modules may need service extraction.

---

## 18. Interview Q&A Preparation (Direct Answers)

### Q1. Why JWT instead of server sessions?

JWT supports stateless authentication, making the system easier to scale horizontally and simpler for API clients. It also avoids centralized session-store coupling.

### Q2. How do you prevent unauthorized admin actions?

By combining path-level access restrictions in `SecurityConfig` with method-level role checks, and service-level business guards that validate operator permissions against target roles.

### Q3. How is account deletion handled safely?

The module uses soft-delete (`deleted=true`) plus status inactivation, preserving historical references for audit/compliance while blocking future operational access.

### Q4. How is password security implemented?

Passwords are never stored in plain text. They are BCrypt-hashed, validated against complexity rules, and checked for reuse in password-change flows.

### Q5. How do you ensure API errors are reliable for frontend teams?

A centralized exception handler maps validation, auth, data, and domain errors into one consistent response schema with status, message, path, and optional field-level details.

### Q6. What would you improve first for production hardening?

1. Move secrets out of properties into a secret manager.
2. Add rate limiting and brute-force protection to login.
3. Introduce refresh token lifecycle and revocation strategy.
4. Expand pagination/indexing for high-volume operations.

### Q7. How would you explain this module to a non-technical interviewer?

It is the security and identity backbone of IdeaTrack. It ensures only the right person can perform the right action, while keeping every user action controlled, traceable, and compliant.

---

## 19. One-Minute Interview Pitch

"I implemented and documented the User Management layer of IdeaTrack as the trust foundation of the platform. The module uses JWT-based stateless authentication, role-based authorization, soft-delete lifecycle controls, BCrypt password security, and centralized error semantics. Architecturally, it follows a layered modular-monolith approach with clear controller-service-repository boundaries. I can explain request flow from token validation to business enforcement to persistence, and I can also discuss tradeoffs, scaling paths, and production hardening steps."
