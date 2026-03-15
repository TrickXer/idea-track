package com.ideatrack.main.repository;

import com.ideatrack.main.data.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ICategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByCategoryIdAndDeletedFalse(Integer id);
    List<Category> findAllByDeletedFalse();
    boolean existsByNameAndDepartment_DeptIdAndDeletedFalse(String name, Integer deptId);
    

@Query("""
           select (count(c) > 0)
           from Category c
           where c.deleted = false
             and lower(c.name) = lower(:name)
             and c.department.deptId = :deptId
             and c.categoryId <> :excludeId
           """)
    boolean existsActiveByNameAndDeptExcludingId(String name, Integer deptId, Integer excludeId);

	
//	Used in ReviewerStageAssignment Module, to find the list of category for a given deptarmentId. By - Advait
	List<Category> findByDepartment_DeptIdAndDeletedFalse(Integer deptId);

}