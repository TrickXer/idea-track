package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
// ⚠️ Using the same imports/pattern as your Idea repo test:
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.Notification;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.Constants;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class INotificationRepositoryTest {

    @Autowired
    private INotificationRepository notificationRepo;

    @Autowired
    private TestEntityManager entityManager;

    private Department testDept;
    private User testUser;

    @BeforeEach
    void setup() {
        Department dept = new Department();
        dept.setDeptId(1);
        dept.setDeptName("R&D");
        testDept = entityManager.persistFlushFind(dept);

        testUser = User.builder()
                .name("Akash")
                .email("akash@test.com")
                .role(Constants.Role.EMPLOYEE)
                .department(testDept)
                .deleted(false)
                .build();
        testUser = entityManager.persistFlushFind(testUser);
    }

    // -------------------------------
    // Helpers
    // -------------------------------

    private Notification createNotificationForUser(
            User user,
            String status,          // "UNREAD" | "READ"
            boolean pushed,
            boolean deleted,
            LocalDateTime createdAt,
            String title
    ) {
        // Persist first — auditing will set createdAt to "now" and it is updatable=false, so we can't truly override it.
        Notification n = Notification.builder()
                .user(user)
                .notificationType("SYSTEM")
                .notificationTitle(title)
                .notificationMessage("Msg")
                .priority("LOW")
                .notificationStatus(status)
                .isPushed(pushed)
                .deleted(deleted)
                .build();

        n = entityManager.persistFlushFind(n);

        // We keep these lines for parity with your Idea tests; note createdAt is updatable=false.
        // They won't change the DB column value but also won't hurt.
        setCreatedAt(n, createdAt);
        setUpdatedAt(n, createdAt.plusMinutes(1));
        n = entityManager.merge(n);
        entityManager.flush();

        return n;
    }

    private void setCreatedAt(Notification n, LocalDateTime ts) {
        try {
            Field f = Notification.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(n, ts);
        } catch (Exception ex) {
            // Swallowing is fine for tests; createdAt is updatable=false anyway.
        }
    }

    private void setUpdatedAt(Notification n, LocalDateTime ts) {
        try {
            Field f = Notification.class.getDeclaredField("updatedAt");
            f.setAccessible(true);
            f.set(n, ts);
        } catch (Exception ex) {
            // Ignore in tests
        }
    }

    // -------------------------------
    // searchForUser() JPQL
    // -------------------------------

    @Test
    @DisplayName("Search for user with filters and DESC order by createdAt")
    void testSearchForUserWithFilters() {
        User other = User.builder()
                .name("Other")
                .email("other@test.com")
                .role(Constants.Role.EMPLOYEE)
                .department(testDept)
                .deleted(false)
                .build();
        other = entityManager.persistFlushFind(other);

        // Create data; createdAt will be "now" due to auditing/updatable=false.
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "A"); // included
        createNotificationForUser(testUser, "READ",   false, false, LocalDateTime.now(), "B"); // excluded by status
        createNotificationForUser(testUser, "UNREAD", true,  false, LocalDateTime.now(), "C"); // excluded by pushed
        createNotificationForUser(testUser, "UNREAD", false, true,  LocalDateTime.now(), "DELETED"); // excluded by deleted

        // other user (excluded by user)
        createNotificationForUser(other, "UNREAD", false, false, LocalDateTime.now(), "OtherUser");

        // Use a time window that includes "now"
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(7);
        LocalDateTime to   = now.plusDays(1);

        Page<Notification> page = notificationRepo.searchForUser(
                testUser.getUserId(),
                "UNREAD",
                Boolean.FALSE,
                from,
                to,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertEquals(1, page.getTotalElements(), "Only one notification should match filters");
        assertEquals("A", page.getContent().get(0).getNotificationTitle());
    }

    @Test
    @DisplayName("Search for user with status=null & pushed=null returns all (non-deleted)")
    void testSearchForUserAllStatusAndPushed() {
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "A");
        createNotificationForUser(testUser, "READ",   true,  false, LocalDateTime.now(), "B");
        createNotificationForUser(testUser, "UNREAD", true,  false, LocalDateTime.now(), "C");

        Page<Notification> page = notificationRepo.searchForUser(
                testUser.getUserId(),
                null,    // ALL statuses
                null,    // ALL pushed states
                null,
                null,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        var titles = page.getContent().stream().map(Notification::getNotificationTitle).toList();
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("A"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("B"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("C"));
        assertFalse(page.getContent().stream().anyMatch(Notification::isDeleted));
    }

    // -------------------------------
    // Unpushed finders
    // -------------------------------

    @Test
    @DisplayName("Global unpushed (ASC by createdAt) — contains our inserted notifications")
    void testFindTop200GlobalUnpushedAsc() {
        // We can't guarantee our rows are the first, because the DB may already have older unpushed rows.
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "A");
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "B");
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "C");

        // Exclusions
        createNotificationForUser(testUser, "UNREAD", true,  false, LocalDateTime.now(), "Pushed");
        createNotificationForUser(testUser, "UNREAD", false, true,  LocalDateTime.now(), "Deleted");

        List<Notification> list = notificationRepo.findTop200ByIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc();

        var titles = list.stream().map(Notification::getNotificationTitle).toList();
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("A"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("B"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("C"));
    }

    @Test
    @DisplayName("Per-user unpushed oldest-first (ASC by createdAt)")
    void testFindTop200UnpushedPerUser() {
        User other = User.builder()
                .name("Other")
                .email("other@test.com")
                .role(Constants.Role.EMPLOYEE)
                .department(testDept)
                .deleted(false)
                .build();
        other = entityManager.persistFlushFind(other);

        LocalDateTime now = LocalDateTime.now();
        createNotificationForUser(testUser, "UNREAD", false, false, now.minusMinutes(3), "U1-1");
        createNotificationForUser(testUser, "UNREAD", false, false, now.minusMinutes(2), "U1-2");
        createNotificationForUser(other,    "UNREAD", false, false, now.minusMinutes(1), "U2-1");

        List<Notification> listU1 =
                notificationRepo.findTop200ByUser_UserIdAndIsPushedFalseAndDeletedFalseOrderByCreatedAtAsc(testUser.getUserId());

        assertEquals(2, listU1.size());
        assertEquals("U1-1", listU1.get(0).getNotificationTitle());
        assertEquals("U1-2", listU1.get(1).getNotificationTitle());
    }

    // -------------------------------
    // Backlog (recent unread)
    // -------------------------------

    @Test
    @DisplayName("Recent unread backlog contains the newly created unread notifications for the user")
    void testBacklogRecentUnreadDesc() {
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "A");
        createNotificationForUser(testUser, "UNREAD", false, false, LocalDateTime.now(), "B");
        createNotificationForUser(testUser, "READ",   false, false, LocalDateTime.now(), "C"); // excluded

        List<Notification> list =
                notificationRepo.findTop50ByUser_UserIdAndNotificationStatusAndDeletedFalseOrderByCreatedAtDesc(
                        testUser.getUserId(), "UNREAD");

        var titles = list.stream().map(Notification::getNotificationTitle).toList();
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("A"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("B"));
        org.junit.jupiter.api.Assertions.assertFalse(titles.contains("C"));
    }

    // -------------------------------
    // Bulk updates
    // -------------------------------

    @Test
    @Transactional
    @DisplayName("markPushed(): updates count equals matched rows (even if already true)")
    void testMarkPushed() {
        LocalDateTime t = LocalDateTime.now();
        Notification n1 = createNotificationForUser(testUser, "UNREAD", false, false, t.minusMinutes(3), "A");
        Notification n2 = createNotificationForUser(testUser, "UNREAD", false, false, t.minusMinutes(2), "B");
        Notification n3 = createNotificationForUser(testUser, "UNREAD", true,  false, t.minusMinutes(1), "AlreadyPushed");

        int updated = notificationRepo.markPushed(List.of(n1.getNotificationId(), n2.getNotificationId(), n3.getNotificationId()));

        // JPQL UPDATE counts matched rows, not only changed rows -> expect 3
        assertEquals(3, updated);

        entityManager.clear();
        Notification r1 = entityManager.find(Notification.class, n1.getNotificationId());
        Notification r2 = entityManager.find(Notification.class, n2.getNotificationId());
        Notification r3 = entityManager.find(Notification.class, n3.getNotificationId());

        assertTrue(r1.isPushed());
        assertTrue(r2.isPushed());
        assertTrue(r3.isPushed());
    }

    @Test
    @Transactional
    @DisplayName("markRead(): sets status READ for owned ids only")
    void testMarkReadEnforcesOwnership() {
        User other = User.builder()
                .name("Other")
                .email("other@test.com")
                .role(Constants.Role.EMPLOYEE)
                .department(testDept)
                .deleted(false)
                .build();
        other = entityManager.persistFlushFind(other);

        LocalDateTime t = LocalDateTime.now();

        Notification u1n1 = createNotificationForUser(testUser, "UNREAD", false, false, t.minusMinutes(3), "U1-A");
        Notification u1n2 = createNotificationForUser(testUser, "UNREAD", false, false, t.minusMinutes(2), "U1-B");
        Notification u2n1 = createNotificationForUser(other,    "UNREAD", false, false, t.minusMinutes(1), "U2-X");

        int updated = notificationRepo.markRead(testUser.getUserId(), List.of(
                u1n1.getNotificationId(), u1n2.getNotificationId(), u2n1.getNotificationId() // includes other's id
        ));
        assertEquals(2, updated);

        entityManager.clear();
        assertEquals("READ", entityManager.find(Notification.class, u1n1.getNotificationId()).getNotificationStatus());
        assertEquals("READ", entityManager.find(Notification.class, u1n2.getNotificationId()).getNotificationStatus());
        assertEquals("UNREAD", entityManager.find(Notification.class, u2n1.getNotificationId()).getNotificationStatus());
    }

    @Test
    @Transactional
    @DisplayName("markAllReadByUser(): sets all non-deleted and non-READ to READ for the user")
    void testMarkAllReadByUser() {
        User other = User.builder()
                .name("Other")
                .email("other@test.com")
                .role(Constants.Role.EMPLOYEE)
                .department(testDept)
                .deleted(false)
                .build();
        other = entityManager.persistFlushFind(other);

        LocalDateTime t = LocalDateTime.now();

        Notification a = createNotificationForUser(testUser, "UNREAD", false, false, t.minusMinutes(30), "A");
        Notification b = createNotificationForUser(testUser, "READ",   false, false, t.minusMinutes(20), "B");  // stays READ
        Notification c = createNotificationForUser(testUser, "UNREAD", false, true,  t.minusMinutes(10), "C");  // deleted -> ignored
        Notification d = createNotificationForUser(other,    "UNREAD", false, false, t.minusMinutes(5),  "V");  // other user

        int updated = notificationRepo.markAllReadByUser(testUser.getUserId());
        assertEquals(1, updated);

        entityManager.clear();
        assertEquals("READ", entityManager.find(Notification.class, a.getNotificationId()).getNotificationStatus());
        assertEquals("READ", entityManager.find(Notification.class, b.getNotificationId()).getNotificationStatus());
        assertEquals("UNREAD", entityManager.find(Notification.class, c.getNotificationId()).getNotificationStatus());
        assertEquals("UNREAD", entityManager.find(Notification.class, d.getNotificationId()).getNotificationStatus());
    }

    // Local assert helper (to mirror your style)
    private static void assertTrue(boolean condition) { org.junit.jupiter.api.Assertions.assertTrue(condition); }
}