package com.ideatrack.main.repository;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ideatrack.main.data.Report;
import com.ideatrack.main.data.User;

@Repository
public interface IReportRepository extends JpaRepository<Report, Integer> {
	// Used in GamificationService
	// Counts reports created by the user.
	int countByUser(User user);
	
	
//	Used in analytics module
	List<Report> findAllByDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
