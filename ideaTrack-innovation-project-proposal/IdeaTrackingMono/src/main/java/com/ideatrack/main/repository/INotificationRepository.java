package com.ideatrack.main.repository;

import com.ideatrack.main.data.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface INotificationRepository extends JpaRepository<Notification, Integer> {

    // ----------- Paged Search for a User (with optional filters) -----------
    @Query("""
           SELECT n FROM Notification n
           WHERE n.deleted = false
             AND n.user.userId = :userId
             AND (:status IS NULL OR n.notificationStatus = :status)
             AND (:pushed IS NULL OR n.isPushed = :pushed)
             AND (:from IS NULL OR n.createdAt >= :from)
             AND (:to   IS NULL OR n.createdAt <= :to)
           ORDER BY n.createdAt DESC
           """)
    Page<Notification> searchForUser(
            @Param("userId") Integer userId,
            @Param("status") String status,           // "UNREAD", "READ", or null = ALL
            @Param("pushed") Boolean pushed,          // true/false or null = ALL
            @Param("from") LocalDateTime from,        // nullable
            @Param("to") LocalDateTime to,            // nullable
            Pageable pageable
    );

    // ----------- Unpushed scan for SSE (global / per-user) -----------
    List<Notification> findTop200ByIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc();

    List<Notification> findTop200ByUser_UserIdAndIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc(Integer userId);

    // ----------- Backlog for SSE (recent unread for user) -----------
    List<Notification> findTop50ByUser_UserIdAndNotificationStatusAndDeletedFalseOrderByCreatedAtDesc(
            Integer userId, String notificationStatus);

    // ----------- Bulk updates (efficient) -----------
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isPushed = true WHERE n.notificationId IN :ids")
    int markPushed(@Param("ids") Collection<Integer> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Notification n
           SET n.notificationStatus = 'READ'
           WHERE n.notificationId IN :ids
             AND n.user.userId = :userId
           """)
    int markRead(@Param("userId") Integer userId, @Param("ids") Collection<Integer> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Notification n
           SET n.notificationStatus = 'READ'
           WHERE n.user.userId = :userId
             AND n.deleted = false
             AND n.notificationStatus <> 'READ'
           """)
    int markAllReadByUser(@Param("userId") Integer userId);
}