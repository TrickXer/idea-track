package com.ideatrack.main.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryPojoTest {

    @Test
    @DisplayName("Category builder and defaults work as expected")
    void builder_defaults_and_getters() {
        Department dept = new Department();
        dept.setDeptId(100);
        dept.setDeptName("Engineering");

        User admin = User.builder().userId(1).name("Admin").build();

        Category cat = Category.builder()
                .name("Innovation")
                .department(dept)
                .createdByAdmin(admin)
                .reviewerCountPerStage(2)
                .stageCount(3)
                // deleted not set -> defaults to false (per entity)
                .build();

        assertNull(cat.getCategoryId(), "ID should be null before persist");
        assertEquals("Innovation", cat.getName());
        assertEquals(100, cat.getDepartment().getDeptId());
        assertEquals(1, cat.getCreatedByAdmin().getUserId());
        assertEquals(2, cat.getReviewerCountPerStage());
        assertEquals(3, cat.getStageCount());
        assertFalse(cat.isDeleted(), "deleted should default to false");
    }
}