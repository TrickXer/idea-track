package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.exception.DuplicateEmailException;
import com.ideatrack.main.repository.IDepartmentRepository;
import com.ideatrack.main.repository.IUserRepository;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository userRepository;
    private final IDepartmentRepository departmentRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserProfileRules profileRules;

    private static final String PASSWORD_REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    private static final String SPECIALS = "@#$%^&+=!";
    private static final String POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + SPECIALS;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ---------- Public Queries ----------
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    //  NEW: returns users based on operator role
    public List<UserResponse> getAllUsersForOperator(String operatorEmail) {
        User operator = getOperator(operatorEmail);
        List<Constants.Role> allowedRoles = allowedViewRoles(operator.getRole());

        return userRepository.findAllByDeletedFalseAndRoleIn(allowedRoles)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public DepartmentListResponse getAllDepartmentNames() {
        List<String> names = departmentRepository.findAllByDeletedFalse()
                .stream()
                .map(Department::getDeptName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new DepartmentListResponse(names);
    }

    // ---------- Signup ----------
    public User registerUserFromAuth(AuthRequest request) {

        if (userRepository.existsByEmail(request.getEmail()))
        {
            throw new DuplicateEmailException("Email is already in use!");
        }
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(parseRoleOrDefault(request.getRole(), Constants.Role.EMPLOYEE));
        user.setStatus(Constants.Status.ACTIVE);
        user.setDeleted(false);

        Department dept = resolveDepartmentByName(request.getDeptName());
        if (dept != null) user.setDepartment(dept);

        return userRepository.save(user);
    }

    // ---------- Create (ADMIN/SUPERADMIN) ----------
    public UserCreateResponse createUserByOperator(String operatorEmail, UserCreateRequest req) {
        User operator = getOperator(operatorEmail);

        if (!StringUtils.hasText(req.getEmail())) throw new IllegalArgumentException("Email is required");
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email is already in use!");
        }
        Constants.Role targetRole = parseRoleOrDefault(req.getRole(), Constants.Role.EMPLOYEE);
        enforceCreatePermission(operator.getRole(), targetRole);

        String tempPassword = generateTempPassword(12);
        validatePasswordStrength(tempPassword);

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRole(targetRole);

        user.setStatus(Constants.Status.ACTIVE);
        user.setDeleted(false);
        user.setProfileCompleted(false);
        user.setTotalXP(0);

        Department dept = resolveDepartmentByName(req.getDeptName());
        if (dept != null) user.setDepartment(dept);

        user.setCreatedByUser(operator);

        User saved = userRepository.save(user);

        return new UserCreateResponse(
                "User created successfully.",
                toUserResponse(saved),
                tempPassword
        );
    }

    // ---------- Update (ADMIN/SUPERADMIN) ----------
    public UserUpdateResponse updateUserByOperator(String operatorEmail, Integer targetUserId, UserUpdateRequest req) {
        User operator = getOperator(operatorEmail);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetUserId));

        if (target.isDeleted()) throw new RuntimeException("Cannot update a deleted user.");

        enforceManagePermission(operator.getRole(), target.getRole());

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email is already in use!");
        }

        // Track which fields updated
        List<String> updatedFields = new ArrayList<>();

        if (StringUtils.hasText(req.getName()) && !req.getName().equals(target.getName())) {
            target.setName(req.getName());
            updatedFields.add("name");
        }

        if (StringUtils.hasText(req.getEmail()) && !req.getEmail().equalsIgnoreCase(target.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new RuntimeException("Email is already in use!");
            }
            target.setEmail(req.getEmail());
            updatedFields.add("email");
        }

        if (StringUtils.hasText(req.getRole())) {
            Constants.Role newRole = parseRoleOrDefault(req.getRole(), target.getRole());

            // operator can’t promote to SUPERADMIN via update
            if (newRole == Constants.Role.SUPERADMIN) {
                throw new RuntimeException("Cannot assign SUPERADMIN role via update.");
            }

            // enforce if operator is ADMIN, cannot set ADMIN
            enforceCreatePermission(operator.getRole(), newRole);

            if (newRole != target.getRole()) {
                target.setRole(newRole);
                updatedFields.add("role");
            }
        }

        if (StringUtils.hasText(req.getDeptName())) {
            Department dept = resolveDepartmentByName(req.getDeptName());
            if (dept != null && (target.getDepartment() == null ||
                    !dept.getDeptId().equals(target.getDepartment().getDeptId()))) {
                target.setDepartment(dept);
                updatedFields.add("department");
            }
        }

        if (StringUtils.hasText(req.getPhoneNo()) && !Objects.equals(req.getPhoneNo(), target.getPhoneNo())) {
            target.setPhoneNo(req.getPhoneNo());
            updatedFields.add("phoneNo");
        }

        if (StringUtils.hasText(req.getBio()) && !Objects.equals(req.getBio(), target.getBio())) {
            target.setBio(req.getBio());
            updatedFields.add("bio");
        }

        if (StringUtils.hasText(req.getProfileUrl()) && !Objects.equals(req.getProfileUrl(), target.getProfileUrl())) {
            target.setProfileUrl(req.getProfileUrl());
            updatedFields.add("profileUrl");
        }

        User saved = userRepository.save(target);

        String message;
        if (updatedFields.isEmpty()) {
            message = "No changes detected. User was not modified.";
        } else {
            message = "User updated successfully. Updated fields: " + String.join(", ", updatedFields) + ".";
        }

        return new UserUpdateResponse(message, toUserResponse(saved));
    }

    // ---------- Delete (ADMIN/SUPERADMIN) ----------
    public ApiMessageResponse deleteUserByOperator(String operatorEmail, Integer targetUserId) {
        User operator = getOperator(operatorEmail);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetUserId));

        enforceManagePermission(operator.getRole(), target.getRole());

        target.setDeleted(true);
        target.setStatus(Constants.Status.INACTIVE);
        userRepository.save(target);

        return new ApiMessageResponse("User with ID " + targetUserId + " has been soft-deleted successfully.");
    }

    // ---------- Helpers ----------
    private User getOperator(String operatorEmail) {
        return userRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new RuntimeException("Operator user not found: " + operatorEmail));
    }

    private List<Constants.Role> allowedViewRoles(Constants.Role operatorRole) {
        if (operatorRole == Constants.Role.SUPERADMIN) {
            return Arrays.asList(Constants.Role.ADMIN, Constants.Role.REVIEWER, Constants.Role.EMPLOYEE);
        }
        if (operatorRole == Constants.Role.ADMIN) {
            return Arrays.asList(Constants.Role.REVIEWER, Constants.Role.EMPLOYEE);
        }
        throw new RuntimeException("Not allowed to view users.");
    }

    private void enforceCreatePermission(Constants.Role operatorRole, Constants.Role targetRole) {
        // Cannot create SUPERADMIN via API
        if (targetRole == Constants.Role.SUPERADMIN) {
            throw new RuntimeException("Cannot create SUPERADMIN via this API.");
        }

        if (operatorRole == Constants.Role.SUPERADMIN) {
            // SUPERADMIN can create ADMIN/REVIEWER/EMPLOYEE
            return;
        }

        if (operatorRole == Constants.Role.ADMIN) {
            // ADMIN can only create REVIEWER/EMPLOYEE
            if (targetRole == Constants.Role.ADMIN) {
                throw new RuntimeException("ADMIN cannot create another ADMIN.");
            }
            return;
        }

        throw new RuntimeException("Not allowed to create users.");
    }

    private void enforceManagePermission(Constants.Role operatorRole, Constants.Role targetRole) {
        // SUPERADMIN can manage ADMIN/REVIEWER/EMPLOYEE (not SUPERADMIN)
        if (operatorRole == Constants.Role.SUPERADMIN) {
            if (targetRole == Constants.Role.SUPERADMIN) {
                throw new RuntimeException("SUPERADMIN cannot manage another SUPERADMIN.");
            }
            return;
        }

        // ADMIN can manage REVIEWER/EMPLOYEE only
        if (operatorRole == Constants.Role.ADMIN) {
            if (targetRole == Constants.Role.ADMIN || targetRole == Constants.Role.SUPERADMIN) {
                throw new RuntimeException("ADMIN cannot manage ADMIN/SUPERADMIN users.");
            }
            return;
        }

        throw new RuntimeException("Not allowed to manage users.");
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long.");
        }
        if (!password.matches(PASSWORD_REGEX)) {
            throw new RuntimeException(
                    "Password too weak! Must contain at least one uppercase, one lowercase, one digit, and one special character (@#$%^&+=!)."
            );
        }
    }

    private String generateTempPassword(int length) {
        int finalLen = Math.max(8, length);

        List<Character> chars = new ArrayList<>();
        chars.add((char) ('A' + RANDOM.nextInt(26)));
        chars.add((char) ('a' + RANDOM.nextInt(26)));
        chars.add((char) ('0' + RANDOM.nextInt(10)));
        chars.add(SPECIALS.charAt(RANDOM.nextInt(SPECIALS.length())));

        while (chars.size() < finalLen) {
            chars.add(POOL.charAt(RANDOM.nextInt(POOL.length())));
        }

        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(finalLen);
        for (char c : chars) sb.append(c);

        String result = sb.toString();
        if (!result.matches(PASSWORD_REGEX)) return generateTempPassword(length);
        return result;
    }

    private Constants.Role parseRoleOrDefault(String roleStr, Constants.Role defaultRole) {
        if (!StringUtils.hasText(roleStr)) return defaultRole;
        try {
            return Constants.Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid role. Allowed values: EMPLOYEE, REVIEWER, ADMIN, SUPERADMIN");
        }
    }

    private Department resolveDepartmentByName(String deptName) {
        if (!StringUtils.hasText(deptName)) return null;

        return departmentRepository.findByDeptNameIgnoreCaseAndDeletedFalse(deptName.trim())
                .orElseThrow(() -> new RuntimeException("Department not found with name: " + deptName));
    }

    private UserResponse toUserResponse(User user) {
        Integer deptId = user.getDepartment() != null ? user.getDepartment().getDeptId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getDeptName() : null;

        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .deptId(deptId)
                .deptName(deptName)
                .phoneNo(user.getPhoneNo())
                .profileUrl(user.getProfileUrl())
                .bio(user.getBio())
                .status(user.getStatus())
                .totalXP(user.getTotalXP())
                .profileCompleted(profileRules.isProfileCompleted(user))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deleted(user.isDeleted())
                .build();
    }

	public List<DepartmentIDDTOListResponse> getAllDepartmentID() {

		List<DepartmentIDDTOListResponse> deptIdList = departmentRepository.findAllByDeletedFalse()
														.stream()
														.map(dept -> new DepartmentIDDTOListResponse(dept.getDeptId(), dept.getDeptName()))
														.toList();
		
		return deptIdList;
	}
    
    
    
    
    
}


