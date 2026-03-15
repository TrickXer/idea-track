
package com.ideatrack.main.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Constants.IdeaStatus;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.analytics.CategoryCountDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IIdeaRepository extends JpaRepository<Idea, Integer> {

    List<Idea> findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus status);
    List<Idea> findByTag(String tag);

    // Used in GamificationService
    // Counts ideas submitted by the user; used for submitter badge thresholds (e.g., 1, 5, 10+ ideas).
    int countByUser(User user);
    // Counts user ideas that reached final statuses (e.g., ACCEPTED,REJECTED), driving reviewer/admin-type badge milestones.
    int countByUserAndIdeaStatusIn(User user, List<Constants.IdeaStatus> statuses);


    //Used in idea Service
    //findAllByUser_UserIdAndDeletedFalse
    List<Idea>findAllByUser_UserIdAndDeletedFalse(Integer userId);

    //For testing
    long countByDeletedFalse();

    //Used in Analytics Service Module
    long countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end);
    long countByDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
			SELECT new com.ideatrack.main.dto.analytics.CategoryCountDTO(
			i.category.categoryId,
			i.category.name,
			COUNT(i)
			)
			FROM Idea i
			WHERE i.user.userId = :userId
			AND i.deleted = false
			GROUP BY i.category.categoryId, i.category.name
			ORDER BY COUNT(i) DESC
			""")
    List<CategoryCountDTO> findCategoryCountsForUser(@Param("userId") Integer userId);

    //To search the ideas
    @Query("""
        SELECT i FROM Idea i
        WHERE (:includeDeleted = true OR i.deleted = false)
          AND (:categoryId IS NULL OR i.category.categoryId = :categoryId)
          AND (:userId IS NULL OR i.user.userId = :userId)
          AND (:status IS NULL OR i.ideaStatus = :status)
          AND (:stage IS NULL OR i.stage = :stage)
          AND (
               :q IS NULL
               OR LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(i.description) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(i.problemStatement) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(i.tag) LIKE LOWER(CONCAT('%', :q, '%'))
          )
    """)
    Page<Idea> searchIdeas(
            @Param("q") String q,
            @Param("categoryId") Integer categoryId,
            @Param("userId") Integer userId,
            @Param("status") Constants.IdeaStatus status,
            @Param("stage") Integer stage,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable
    );

    //Used in Analytics Service Module
    long countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus status, LocalDateTime start,
                                                             LocalDateTime end);


    @Query("""
			SELECT new com.ideatrack.main.dto.analytics.CategoryCountDTO(
			i.category.categoryId,
			i.category.name,
			COUNT(i)
			)
			FROM Idea i
			WHERE i.createdAt >= :start AND i.createdAt < :end
			AND i.deleted = false
			AND i.ideaStatus = APPROVED
			GROUP BY i.category.categoryId, i.category.name
			ORDER BY COUNT(i) DESC
			""")
    List<CategoryCountDTO> findTotalCategoryCountCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    // Finds ideas by ID but filters out soft-deleted ones
    List<Idea> findAllByIdeaIdInAndDeletedFalse(Collection<Integer> ideaIds);

    // Finds all ideas by ID regardless of deleted status
    List<Idea> findAllByIdeaIdIn(Collection<Integer> ideaIds);


    @Query("""
            SELECT i FROM Idea i
            WHERE (:includeDeleted = true OR i.deleted = false)
              AND i.category.categoryId = :categoryId
            """)
    List<Idea> findAllByCategoryForExport(@Param("categoryId") Integer categoryId,
                                          @Param("includeDeleted") boolean includeDeleted);

    @Query("""
            SELECT i FROM Idea i
            WHERE (:includeDeleted = true OR i.deleted = false)
            """)
    List<Idea> findAllForExport(@Param("includeDeleted") boolean includeDeleted);

    //Analytics Report DEPARTMENT scope
    long countByCategory_Department_DeptIdAndCreatedAtBetween(Integer deptId, LocalDateTime start, LocalDateTime end);

    long countByIdeaStatusAndCategory_Department_DeptIdAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus status, Integer deptId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT i.user.userId) FROM Idea i " +
            "WHERE i.category.department.deptId = :deptId " +
            "AND i.createdAt BETWEEN :start AND :end AND i.deleted = false")
    long countDistinctUsersByDepartment(@Param("deptId") Integer deptId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);



    //Analytics Report CATEGORY scope
    long countByCategory_CategoryIdAndCreatedAtBetween(Integer catId, LocalDateTime start, LocalDateTime end);

    long countByIdeaStatusAndCategory_CategoryIdAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus status, Integer catId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT i.user.userId) FROM Idea i " +
            "WHERE i.category.categoryId = :catId " +
            "AND i.createdAt BETWEEN :start AND :end AND i.deleted = false")
    long countDistinctUsersByCategory(@Param("catId") Integer catId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    //Analytics Report PERIOD scope
    @Query("SELECT COUNT(DISTINCT i.user.userId) FROM Idea i " +
            "WHERE i.createdAt BETWEEN :start AND :end AND i.deleted = false")
    long countDistinctUsersByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
	List<Idea> findByUser_UserIdAndIdeaStatusInAndDeletedFalse(Integer userId, List<IdeaStatus> acceptedLikeStatuses);
}
