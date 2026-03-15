package com.ideatrack.main.controller;

import com.ideatrack.main.dto.category.CategoryCreateRequest;
import com.ideatrack.main.dto.category.CategoryResponseDTO;
import com.ideatrack.main.dto.category.CategoryUpdateRequest;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.DepartmentNotFound;
import com.ideatrack.main.exception.DuplicateCategoryException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    
    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryCreateRequest request)
            throws DepartmentNotFound, UserNotFoundException, DuplicateCategoryException {

        log.info("HTTP POST /api/categories - Create category requested");
        log.debug("Create payload: name='{}', departmentId={}, createdByAdminId={}, reviewerCountPerStage={}, stageCount={}",
                request.getName(), request.getDepartmentId(), request.getCreatedByAdminId(),
                request.getReviewerCountPerStage(), request.getStageCount());

        CategoryResponseDTO created = categoryService.create(request);

        log.info("Category created successfully: categoryId={}, name='{}'", created.getCategoryId(), created.getName());
        return ResponseEntity
                .created(URI.create("/api/categories/" + created.getCategoryId()))
                .body(created);
    }

    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','EMPLOYEE','REVIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getById(@PathVariable Integer id)
            throws CategoryNotFound {

        log.info("HTTP GET /api/categories/{} - Fetch category by id", id);
        CategoryResponseDTO response = categoryService.getById(id);
        log.info("Fetched category: id={}, name='{}'", response.getCategoryId(), response.getName());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','EMPLOYEE','REVIEWER')")
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAll() {
        log.info("HTTP GET /api/categories - Fetch all categories (non-deleted)");
        List<CategoryResponseDTO> list = categoryService.getAll();
        log.info("Fetched {} categories", list.size());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(@PathVariable Integer id,
                                                      @Valid @RequestBody CategoryUpdateRequest request)
            throws CategoryNotFound, DepartmentNotFound, UserNotFoundException, DuplicateCategoryException {

        log.info("HTTP PATCH /api/categories/{} - Update category requested", id);
        log.debug("Update payload: name='{}', departmentId={}, createdByAdminId={}, reviewerCountPerStage={}, stageCount={}",
                request.getName(), request.getDepartmentId(), request.getCreatedByAdminId(),
                request.getReviewerCountPerStage(), request.getStageCount());

        CategoryResponseDTO updated = categoryService.update(id, request);

        log.info("Category updated successfully: id={}, name='{}'", updated.getCategoryId(), updated.getName());
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id)
            throws CategoryNotFound {

        log.info("HTTP DELETE /api/categories/{} - Soft delete requested", id);
        categoryService.deleteSoft(id);
        log.info("Category soft-deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}