package com.ideatrack.main.repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.proposal.ProposalCreateRequestDTO;

@Repository
public interface IUserRepository extends JpaRepository<User, Integer>{
	@Query("""
			SELECT u 
			FROM User u
			WHERE u.deleted = false 
			ORDER BY u.totalXP DESC, u.name ASC

			""")
	List<User> findTopNUser(Pageable pageable);
	
	
	@Query("""
			SELECT COUNT(u)
			FROM User u 
			WHERE u.totalXP > :userXp
			""")
    int countByXpGreaterThan(@Param("userXp") Integer userXp);

	
	@Query("SELECT COUNT(ua) FROM UserActivity ua " +
	       "WHERE ua.user.department.deptId = :deptId " +
	       "AND ua.activityType = :type " +
	       "AND ua.createdAt BETWEEN :start AND :end " +
	       "AND ua.deleted = false")
	    long countByDeptAndActivityType(@Param("deptId") Integer deptId, 
	                                    @Param("type") Constants.ActivityType type, 
	                                    @Param("start") LocalDateTime start, 
	                                    @Param("end") LocalDateTime end);
	

	//Optional<User> findById(ProposalCreateRequestDTO proposalCreateRequestDTO);
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
    List<User> findAllByDeletedFalse();


    // Used to filter users that ADMIN/SUPERADMIN is allowed to see
    List<User> findAllByDeletedFalseAndRoleIn(List<Constants.Role> roles);
    
//  Used in ReviewerStageAssignment Module to find all the Reviewers for a particular Department. - Advait
    List<User> findByRoleAndDepartment_DeptIdAndDeletedFalse(Constants.Role role, Integer deptId);
	Optional<User> findFirstByDepartment_DeptIdAndRoleAndDeletedFalse(Integer deptId, Constants.Role role);
}
