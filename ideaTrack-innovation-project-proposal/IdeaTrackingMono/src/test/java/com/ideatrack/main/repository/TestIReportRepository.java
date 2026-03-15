package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.Report;
import com.ideatrack.main.data.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestIReportRepository {

    @Autowired
    private IReportRepository reportRepo;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testSaveReport() {
        Department dept = new Department();
        dept.setDeptId(999); 
        dept.setDeptName("Security");
        dept.setDeleted(false);
        
        dept = entityManager.persistFlushFind(dept);

        User user = User.builder()
                .name("Admin")
                .email("admin999" + "@test.com")
                .department(dept)
                .role(Constants.Role.ADMIN)
                .password("password")
                .build();
        
        user = entityManager.persistFlushFind(user);

        Report reportObj = new Report();
        reportObj.setScope(Constants.Scope.DEPARTMENT);
        reportObj.setDataOf("Security");
        reportObj.setIdeasSubmitted(50);
        reportObj.setApprovedCount(10);
        reportObj.setParticipationCount(100);
        reportObj.setUser(user);
        reportObj.setDeleted(false);

        Report savedReport = reportRepo.save(reportObj);

        assertNotNull(savedReport.getId(), "Report ID should not be null");
        
        Report fetchedReport = reportRepo.findById(savedReport.getId()).orElse(null);
        
        assertNotNull(fetchedReport);
        assertEquals("Security", fetchedReport.getDataOf());
        assertEquals(dept.getDeptId(), fetchedReport.getUser().getDepartment().getDeptId());
    }
}