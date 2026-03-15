package com.ideatrack.main.service;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.category.CategoryCreateRequest;
import com.ideatrack.main.dto.category.CategoryResponseDTO;
import com.ideatrack.main.dto.category.CategoryUpdateRequest;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.DepartmentNotFound;
import com.ideatrack.main.exception.DuplicateCategoryException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IDepartmentRepository;
import com.ideatrack.main.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Category not found: ";

    private final ICategoryRepository categoryRepo;
    private final IDepartmentRepository departmentRepo;
    private final IUserRepository userRepo;
    private final ModelMapper modelMapper;

    public CategoryResponseDTO create(CategoryCreateRequest request)
            throws DepartmentNotFound, UserNotFoundException, DuplicateCategoryException {

        log.info("Creating category: name='{}', deptId={}, createdByAdminId={}, reviewerCountPerStage={}, stageCount={}",
                request.getName(), request.getDepartmentId(), request.getCreatedByAdminId(),
                request.getReviewerCountPerStage(), request.getStageCount());

        Department dept = departmentRepo.findById(request.getDepartmentId())
                .orElseThrow(() -> {
                    log.warn("DepartmentNotFound: deptId={}", request.getDepartmentId());
                    return new DepartmentNotFound("Department not found: " + request.getDepartmentId());
                });

        User creator = userRepo.findById(request.getCreatedByAdminId())
                .orElseThrow(() -> {
                    log.warn("UserNotFound: createdByAdminId={}", request.getCreatedByAdminId());
                    return new UserNotFoundException("User not found: " + request.getCreatedByAdminId());
                });

        boolean exists = categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse(
                request.getName(), request.getDepartmentId());
        if (exists) {
            log.warn("DuplicateCategoryException: name='{}' already exists in deptId={}",
                    request.getName(), request.getDepartmentId());
            throw new DuplicateCategoryException(
                    "Category '" + request.getName() + "' already exists in department " + request.getDepartmentId());
        }

        Category entity = Category.builder()
                .name(request.getName())
                .department(dept)
                .createdByAdmin(creator)
                .reviewerCountPerStage(request.getReviewerCountPerStage())
                .stageCount(request.getStageCount())
                .deleted(false)
                .build();

        Category saved = categoryRepo.save(entity);
        log.info("Category created: categoryId={}, name='{}', deptId={}",
                saved.getCategoryId(), saved.getName(), saved.getDepartment() != null ? saved.getDepartment().getDeptId() : null);

        return modelMapper.map(saved, CategoryResponseDTO.class);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getById(Integer id) throws CategoryNotFound {
        log.debug("Fetching category by id={}", id);
        Category category = categoryRepo.findByCategoryIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("CategoryNotFound: id={}", id);
                    return new CategoryNotFound(CATEGORY_NOT_FOUND_MESSAGE + id);
                });

        log.info("Fetched category: id={}, name='{}'", category.getCategoryId(), category.getName());
        return modelMapper.map(category, CategoryResponseDTO.class);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAll() {
        log.debug("Fetching all non-deleted categories");
        List<CategoryResponseDTO> list = categoryRepo.findAllByDeletedFalse()
                .stream()
                .map(c -> modelMapper.map(c, CategoryResponseDTO.class))
                .toList();
        log.info("Fetched {} categories", list.size());
        return list;
    }

    public CategoryResponseDTO update(Integer id, CategoryUpdateRequest request)
            throws CategoryNotFound, DepartmentNotFound, UserNotFoundException, DuplicateCategoryException {

        log.info("Updating category: id={}, name='{}', deptId={}, createdByAdminId={}, reviewerCountPerStage={}, stageCount={}",
                id, request.getName(), request.getDepartmentId(), request.getCreatedByAdminId(),
                request.getReviewerCountPerStage(), request.getStageCount());

        Category category = categoryRepo.findByCategoryIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("CategoryNotFound: id={}", id);
                    return new CategoryNotFound(CATEGORY_NOT_FOUND_MESSAGE + id);
                });

        // Map simple scalars (nulls skipped if ModelMapper has setSkipNullEnabled(true))
        modelMapper.map(request, category);
        log.debug("Mapped scalars via ModelMapper for categoryId={}", id);

        // Update relationships only when IDs are provided
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepo.findById(request.getDepartmentId())
                    .orElseThrow(() -> {
                        log.warn("DepartmentNotFound (update): deptId={}", request.getDepartmentId());
                        return new DepartmentNotFound("Department not found: " + request.getDepartmentId());
                    });
            category.setDepartment(dept);
            log.debug("Updated department for categoryId={} to deptId={}", id, dept.getDeptId());
        }

        if (request.getCreatedByAdminId() != null) {
            User creator = userRepo.findById(request.getCreatedByAdminId())
                    .orElseThrow(() -> {
                        log.warn("UserNotFound (update): createdByAdminId={}", request.getCreatedByAdminId());
                        return new UserNotFoundException("User not found: " + request.getCreatedByAdminId());
                    });
            category.setCreatedByAdmin(creator);
            log.debug("Updated createdByAdmin for categoryId={} to userId={}", id, creator.getUserId());
        }

        // === ID-aware uniqueness check within the SAME department ===
        // Allow same name when it's the same record; allow same name across different departments.
        if (request.getName() != null) {
            String newName = request.getName().trim();      // normalize to avoid accidental duplicates
            log.debug("Normalizing name: '{}' -> '{}'", request.getName(), newName);
            category.setName(newName);                      // ensure entity uses normalized value

            if (category.getDepartment() != null) {
                boolean conflict = categoryRepo.existsActiveByNameAndDeptExcludingId(
                        newName,
                        category.getDepartment().getDeptId(),
                        category.getCategoryId()
                );
                if (conflict) {
                    log.warn("DuplicateCategoryException on update: name='{}' exists in deptId={} for another record",
                            newName, category.getDepartment().getDeptId());
                    throw new DuplicateCategoryException("Another category with the same name exists in this department.");
                }
            } else {
                log.debug("Department is null during name uniqueness check for categoryId={}", id);
            }
        }

        Category saved = categoryRepo.save(category);
        log.info("Category updated: id={}, name='{}', deptId={}",
                saved.getCategoryId(), saved.getName(),
                saved.getDepartment() != null ? saved.getDepartment().getDeptId() : null);

        return modelMapper.map(saved, CategoryResponseDTO.class);
    }

    public void deleteSoft(Integer id) throws CategoryNotFound {
        log.info("Soft-deleting category: id={}", id);
        Category category = categoryRepo.findByCategoryIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("CategoryNotFound (delete): id={}", id);
                    return new CategoryNotFound(CATEGORY_NOT_FOUND_MESSAGE + id);
                });
        category.setDeleted(true);
        categoryRepo.save(category);
        log.info("Category soft-deleted: id={}", id);
    }
}
