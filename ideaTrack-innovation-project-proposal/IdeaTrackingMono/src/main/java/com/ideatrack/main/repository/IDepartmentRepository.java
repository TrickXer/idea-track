package com.ideatrack.main.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ideatrack.main.data.Department;
import java.util.List;


@Repository
public interface IDepartmentRepository extends JpaRepository<Department, Integer> {

    // You can add custom query methods if needed, for example:
    Department findByDeptName(String deptName);

    boolean existsByDeptName(String deptName);
    
    Optional<Department> findByDeptId(Integer deptId);

    List<Department> findAllByDeletedFalse();

    // resolve by name (case-insensitive) and only active
    Optional<Department> findByDeptNameIgnoreCaseAndDeletedFalse(String deptName);
}
