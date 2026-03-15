package com.ideatrack.main.controller;

import com.ideatrack.main.dto.category.CategoryCreateRequest;
import com.ideatrack.main.dto.category.CategoryResponseDTO;
import com.ideatrack.main.dto.category.CategoryUpdateRequest;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.DepartmentNotFound;
import com.ideatrack.main.exception.DuplicateCategoryException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestCategoryController {

    @Autowired
    private CategoryController categoryController;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("POST /api/categories - 201 Created + Location + body")
    void create_201() throws Exception {
        var req = CategoryCreateRequest.builder()
                .name("Innovation").departmentId(201).createdByAdminId(5001)
                .reviewerCountPerStage(2).stageCount(3).build();

        var created = CategoryResponseDTO.builder()
                .categoryId(77).name("Innovation")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .deleted(false).build();

        Mockito.when(categoryService.create(Mockito.any(CategoryCreateRequest.class)))
               .thenReturn(created);

        ResponseEntity<CategoryResponseDTO> resp = categoryController.create(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isEqualTo(URI.create("/api/categories/77"));
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCategoryId()).isEqualTo(77);
        assertThat(resp.getBody().getName()).isEqualTo("Innovation");
    }

    @Test
    @DisplayName("POST /api/categories - DuplicateCategoryException bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void create_409_duplicate_bubbles() throws Exception {
        var req = CategoryCreateRequest.builder()
                .name("Innovation").departmentId(201).createdByAdminId(5001)
                .reviewerCountPerStage(1).stageCount(2).build();

        Mockito.when(categoryService.create(Mockito.any(CategoryCreateRequest.class)))
               .thenThrow(new DuplicateCategoryException("Duplicate"));

        assertThrows(DuplicateCategoryException.class, () -> categoryController.create(req));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - 200 OK")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    void getById_200() throws Exception {
        var dto = CategoryResponseDTO.builder().categoryId(22).name("Design").build();
        Mockito.when(categoryService.getById(22)).thenReturn(dto);

        ResponseEntity<CategoryResponseDTO> resp = categoryController.getById(22);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCategoryId()).isEqualTo(22);
        assertThat(resp.getBody().getName()).isEqualTo("Design");
    }

    @Test
    @DisplayName("GET /api/categories/{id} - CategoryNotFound bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    void getById_404_bubbles() {
        Mockito.when(categoryService.getById(999)).thenThrow(new CategoryNotFound("Category not found: 999"));
        assertThrows(CategoryNotFound.class, () -> categoryController.getById(999));
    }

    @Test
    @DisplayName("GET /api/categories - 200 OK")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    void getAll_200() {
        var a = CategoryResponseDTO.builder().categoryId(1).name("A").build();
        var b = CategoryResponseDTO.builder().categoryId(2).name("B").build();

        Mockito.when(categoryService.getAll()).thenReturn(List.of(a, b));

        ResponseEntity<List<CategoryResponseDTO>> resp = categoryController.getAll();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull().hasSize(2);
        assertThat(resp.getBody().get(0).getName()).isEqualTo("A");
        assertThat(resp.getBody().get(1).getName()).isEqualTo("B");
    }

    @Test
    @DisplayName("PATCH /api/categories/{id} - 200 OK")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void update_200() throws Exception {
        var req = CategoryUpdateRequest.builder()
                .name("NewName").reviewerCountPerStage(3).stageCount(4).build();
        var updated = CategoryResponseDTO.builder().categoryId(77).name("NewName").build();

        Mockito.when(categoryService.update(eq(77), Mockito.any(CategoryUpdateRequest.class)))
               .thenReturn(updated);

        ResponseEntity<CategoryResponseDTO> resp = categoryController.update(77, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCategoryId()).isEqualTo(77);
        assertThat(resp.getBody().getName()).isEqualTo("NewName");
    }

    
    @Test
    @DisplayName("PATCH /api/categories/{id} - DuplicateCategoryException bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void update_409_duplicate_bubbles() throws Exception {
        var req = CategoryUpdateRequest.builder().name("Same").build();
    
        Mockito.when(categoryService.update(eq(77), Mockito.any(CategoryUpdateRequest.class)))
               .thenThrow(new DuplicateCategoryException("Another category with the same name exists"));
    
        assertThrows(DuplicateCategoryException.class, () -> categoryController.update(77, req));
    }
    
    @Test
    @DisplayName("PATCH /api/categories/{id} - CategoryNotFound bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void update_404_category_bubbles() throws Exception {
        var req = CategoryUpdateRequest.builder().name("X").build();
    
        Mockito.when(categoryService.update(eq(77), Mockito.any(CategoryUpdateRequest.class)))
               .thenThrow(new CategoryNotFound("Category not found: 77"));
    
        assertThrows(CategoryNotFound.class, () -> categoryController.update(77, req));
    }
    
    @Test
    @DisplayName("PATCH /api/categories/{id} - DepartmentNotFound bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void update_404_department_bubbles() throws Exception {
        var req = CategoryUpdateRequest.builder().departmentId(999).build();
    
        Mockito.when(categoryService.update(eq(77), Mockito.any(CategoryUpdateRequest.class)))
               .thenThrow(new DepartmentNotFound("Department not found: 999"));
    
        assertThrows(DepartmentNotFound.class, () -> categoryController.update(77, req));
    }
    
    @Test
    @DisplayName("PATCH /api/categories/{id} - UserNotFound bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void update_404_user_bubbles() throws Exception {
        var req = CategoryUpdateRequest.builder().createdByAdminId(9999).build();
    
        Mockito.when(categoryService.update(eq(77), Mockito.any(CategoryUpdateRequest.class)))
               .thenThrow(new UserNotFoundException("User not found: 9999"));
    
        assertThrows(UserNotFoundException.class, () -> categoryController.update(77, req));
    }
    
    @Test
    @DisplayName("DELETE /api/categories/{id} - 204 No Content")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void delete_204() throws Exception {
        ResponseEntity<Void> resp = categoryController.delete(55);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Mockito.verify(categoryService).deleteSoft(55);
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - CategoryNotFound bubbles")
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    void delete_404_bubbles() throws Exception {
        Mockito.doThrow(new CategoryNotFound("Category not found: 999"))
               .when(categoryService).deleteSoft(999);
        assertThrows(CategoryNotFound.class, () -> categoryController.delete(999));
    }
}