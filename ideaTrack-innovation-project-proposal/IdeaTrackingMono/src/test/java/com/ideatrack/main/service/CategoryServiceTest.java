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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private ICategoryRepository categoryRepo;
    @Mock private IDepartmentRepository departmentRepo;
    @Mock private IUserRepository userRepo;

    private ModelMapper modelMapper;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        // Rebuild service with real mapper (since @InjectMocks won't inject a runtime-created bean)
        categoryService = new CategoryService(categoryRepo, departmentRepo, userRepo, modelMapper);
    }

    // ---------------------------
    // create()
    // ---------------------------

    @Test
    @DisplayName("create(): success")
    void create_success() throws Exception {
        CategoryCreateRequest req = CategoryCreateRequest.builder()
                .name("Innovation")
                .departmentId(201)
                .createdByAdminId(5001)
                .reviewerCountPerStage(2)
                .stageCount(3)
                .build();

        Department dept = new Department();
        dept.setDeptId(201);
        dept.setDeptName("Test-Engineering");

        User admin = User.builder().userId(5001).name("Test Admin").build();

        when(departmentRepo.findById(201)).thenReturn(Optional.of(dept));
        when(userRepo.findById(5001)).thenReturn(Optional.of(admin));
        when(categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse("Innovation", 201))
                .thenReturn(false);

        Category saved = Category.builder()
                .categoryId(77)
                .name("Innovation")
                .department(dept)
                .createdByAdmin(admin)
                .reviewerCountPerStage(2)
                .stageCount(3)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(categoryRepo.save(any(Category.class))).thenReturn(saved);

        CategoryResponseDTO out = categoryService.create(req);

        assertNotNull(out);
        assertEquals(77, out.getCategoryId());
        assertEquals("Innovation", out.getName());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepo, times(1)).save(captor.capture());
        Category toSave = captor.getValue();
        assertEquals("Innovation", toSave.getName());
        assertEquals(2, toSave.getReviewerCountPerStage());
        assertEquals(3, toSave.getStageCount());
        assertFalse(toSave.isDeleted());
    }

    @Test
    @DisplayName("create(): DepartmentNotFound")
    void create_departmentNotFound() {
        CategoryCreateRequest req = CategoryCreateRequest.builder()
                .name("Innovation")
                .departmentId(999)
                .createdByAdminId(5001)
                .reviewerCountPerStage(1)
                .stageCount(2)
                .build();

        when(departmentRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFound.class, () -> categoryService.create(req));
        verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("create(): UserNotFound")
    void create_userNotFound() {
        CategoryCreateRequest req = CategoryCreateRequest.builder()
                .name("Innovation")
                .departmentId(201)
                .createdByAdminId(9999)
                .reviewerCountPerStage(1)
                .stageCount(2)
                .build();

        when(departmentRepo.findById(201)).thenReturn(Optional.of(new Department()));
        when(userRepo.findById(9999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> categoryService.create(req));
        verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("create(): DuplicateCategoryException when same name exists in department")
    void create_duplicate() {
        CategoryCreateRequest req = CategoryCreateRequest.builder()
                .name("Innovation")
                .departmentId(201)
                .createdByAdminId(5001)
                .reviewerCountPerStage(1)
                .stageCount(2)
                .build();

        when(departmentRepo.findById(201)).thenReturn(Optional.of(new Department()));
        when(userRepo.findById(5001)).thenReturn(Optional.of(new User()));
        when(categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse("Innovation", 201))
                .thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () -> categoryService.create(req));
        verify(categoryRepo, never()).save(any());
    }

    // ---------------------------
    // getById()
    // ---------------------------

    @Test
    @DisplayName("getById(): success")
    void getById_success() throws Exception {
        Category entity = Category.builder()
                .categoryId(33)
                .name("Design")
                .deleted(false)
                .build();
        when(categoryRepo.findByCategoryIdAndDeletedFalse(33)).thenReturn(Optional.of(entity));

        CategoryResponseDTO out = categoryService.getById(33);

        assertNotNull(out);
        assertEquals(33, out.getCategoryId());
        assertEquals("Design", out.getName());
    }

    @Test
    @DisplayName("getById(): not found throws CategoryNotFound")
    void getById_notFound() {
        when(categoryRepo.findByCategoryIdAndDeletedFalse(999)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFound.class, () -> categoryService.getById(999));
    }

    // ---------------------------
    // getAll()
    // ---------------------------

    @Test
    @DisplayName("getAll(): maps all non-deleted categories")
    void getAll_maps() {
        Category a = Category.builder().categoryId(1).name("A").deleted(false).build();
        Category b = Category.builder().categoryId(2).name("B").deleted(false).build();
        when(categoryRepo.findAllByDeletedFalse()).thenReturn(List.of(a, b));

        List<CategoryResponseDTO> list = categoryService.getAll();

        assertEquals(2, list.size());
        assertEquals("A", list.get(0).getName());
        assertEquals("B", list.get(1).getName());
        verify(categoryRepo, times(1)).findAllByDeletedFalse();
    }

    // ---------------------------
    // update()
    // ---------------------------

    @Test
    @DisplayName("update(): success with name normalization, department and createdByAdmin updates")
    void update_success_full() throws Exception {
        Department dept201 = new Department(); dept201.setDeptId(201);
        Department dept202 = new Department(); dept202.setDeptId(202);
        User admin1 = User.builder().userId(5001).name("Admin1").build();
        User admin2 = User.builder().userId(5002).name("Admin2").build();

        Category existing = Category.builder()
                .categoryId(77)
                .name("OldName")
                .department(dept201)
                .createdByAdmin(admin1)
                .reviewerCountPerStage(1)
                .stageCount(2)
                .deleted(false)
                .build();

        when(categoryRepo.findByCategoryIdAndDeletedFalse(77)).thenReturn(Optional.of(existing));
        when(departmentRepo.findById(202)).thenReturn(Optional.of(dept202));
        when(userRepo.findById(5002)).thenReturn(Optional.of(admin2));

        when(categoryRepo.existsActiveByNameAndDeptExcludingId(eq("NewName"), eq(202), any()))
        .thenReturn(false);

        when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryUpdateRequest req = CategoryUpdateRequest.builder()
                .name("   NewName   ")  // ensure trimming
                .departmentId(202)
                .createdByAdminId(5002)
                .reviewerCountPerStage(3)
                .stageCount(4)
                .build();

        CategoryResponseDTO out = categoryService.update(77, req);

        assertNotNull(out);
        assertEquals("NewName", out.getName());
        // Validate that repository was invoked for conflict check with normalized values
        verify(categoryRepo).existsActiveByNameAndDeptExcludingId(eq("NewName"), eq(202), any());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepo).save(captor.capture());
        Category saved = captor.getValue();
        assertEquals("NewName", saved.getName());
        assertEquals(202, saved.getDepartment().getDeptId());
        assertEquals(5002, saved.getCreatedByAdmin().getUserId());
        assertEquals(3, saved.getReviewerCountPerStage());
        assertEquals(4, saved.getStageCount());
    }

    @Test
    @DisplayName("update(): throws DuplicateCategoryException on name conflict in same department")
    void update_conflict_duplicate() {
        Department dept201 = new Department(); dept201.setDeptId(201);
        Category existing = Category.builder()
                .categoryId(77)
                .name("OldName")
                .department(dept201)
                .deleted(false)
                .build();

        when(categoryRepo.findByCategoryIdAndDeletedFalse(77)).thenReturn(Optional.of(existing));

        when(categoryRepo.existsActiveByNameAndDeptExcludingId(eq("Same"), eq(201), any()))
        		.thenReturn(true);


        CategoryUpdateRequest req = CategoryUpdateRequest.builder()
                .name("Same")
                .build();

        assertThrows(DuplicateCategoryException.class, () -> categoryService.update(77, req));
        verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("update(): throws DepartmentNotFound if provided departmentId not found")
    void update_departmentNotFound() {
        Category existing = Category.builder()
                .categoryId(77)
                .name("X")
                .deleted(false)
                .build();

        when(categoryRepo.findByCategoryIdAndDeletedFalse(77)).thenReturn(Optional.of(existing));
        when(departmentRepo.findById(999)).thenReturn(Optional.empty());

        CategoryUpdateRequest req = CategoryUpdateRequest.builder()
                .departmentId(999)
                .build();

        assertThrows(DepartmentNotFound.class, () -> categoryService.update(77, req));
        verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("update(): throws UserNotFound if provided createdByAdminId not found")
    void update_userNotFound() {
        Department dept201 = new Department(); dept201.setDeptId(201);
        Category existing = Category.builder()
                .categoryId(77)
                .name("X")
                .department(dept201)
                .deleted(false)
                .build();

        when(categoryRepo.findByCategoryIdAndDeletedFalse(77)).thenReturn(Optional.of(existing));
        when(userRepo.findById(9999)).thenReturn(Optional.empty());

        CategoryUpdateRequest req = CategoryUpdateRequest.builder()
                .createdByAdminId(9999)
                .build();

        assertThrows(UserNotFoundException.class, () -> categoryService.update(77, req));
        verify(categoryRepo, never()).save(any());
    }

    // ---------------------------
    // deleteSoft()
    // ---------------------------

    @Test
    @DisplayName("deleteSoft(): success marks entity deleted=true and saves")
    void deleteSoft_success() throws Exception {
        Category existing = Category.builder()
                .categoryId(55)
                .name("ToDelete")
                .deleted(false)
                .build();
        when(categoryRepo.findByCategoryIdAndDeletedFalse(55)).thenReturn(Optional.of(existing));

        categoryService.deleteSoft(55);

        assertTrue(existing.isDeleted());
        verify(categoryRepo).save(existing);
    }

    @Test
    @DisplayName("deleteSoft(): not found throws CategoryNotFound")
    void deleteSoft_notFound() {
        when(categoryRepo.findByCategoryIdAndDeletedFalse(999)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFound.class, () -> categoryService.deleteSoft(999));
        verify(categoryRepo, never()).save(any());
    }
}