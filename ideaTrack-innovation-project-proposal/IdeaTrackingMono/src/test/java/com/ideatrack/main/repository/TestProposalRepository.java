package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Constants.IdeaStatus;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;

//Done By Vibhuti	//tested 100%

@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestProposalRepository {

    @Autowired
    private IProposalRepository proposalRepository;

    @Autowired
    private TestEntityManager em;

    // ----------------------------- Helpers -----------------------------

    private <T> T persist(T entity) {
        return em.persistFlushFind(entity);
    }

    private User newUser(String name) {
        User u = new User();
        u.setName(name);
        u.setEmail(name.toLowerCase() + "@test.com");
        u.setRole(Constants.Role.EMPLOYEE);
        u.setDeleted(false);
        return u;
    }

    private Idea newIdea(String title) {
        Idea i = new Idea();
        i.setTitle(title);
        i.setDeleted(false);
        i.setStage(1);
        i.setIdeaStatus(IdeaStatus.DRAFT);
        return i;
    }

    private Proposal newProposal(User author, Idea idea, boolean deleted, IdeaStatus status) {
        Proposal p = new Proposal();
        p.setUser(author);
        p.setIdea(idea);
        p.setDeleted(deleted);
        p.setIdeaStatus(status);
        return p;
    }

    private AssignedReviewerToIdea newAssignment(Idea idea, User reviewer, int stage, boolean deleted) {
        AssignedReviewerToIdea a = new AssignedReviewerToIdea();
        a.setIdea(idea);
        a.setReviewer(reviewer);
        a.setStage(stage);
        a.setDeleted(deleted);
        return a;
    }

    // ----------------------------- Tests -----------------------------

    @Test
    @DisplayName("countByUser: counts proposals authored by the given user")
    void countByUser_shouldWork() {
        User alice = persist(newUser("Alice"));
        User bob   = persist(newUser("Bob"));

        Idea i1 = persist(newIdea("I-1"));
        Idea i2 = persist(newIdea("I-2"));
        Idea i3 = persist(newIdea("I-3"));
        Idea i4 = persist(newIdea("I-4"));

        // 3 for Alice (including one deleted), 1 for Bob
        persist(newProposal(alice, i1, false, IdeaStatus.DRAFT));
        persist(newProposal(alice, i2, true,  IdeaStatus.SUBMITTED));
        persist(newProposal(alice, i3, false, IdeaStatus.UNDERREVIEW));
        persist(newProposal(bob,   i4, false, IdeaStatus.DRAFT));

        assertEquals(3, proposalRepository.countByUser(alice));
        assertEquals(1, proposalRepository.countByUser(bob));
    }

    @Test
    @DisplayName("existsByIdea_IdeaIdAndDeletedFalse: true only for non-deleted proposals attached to idea")
    void existsByIdeaIdAndDeletedFalse() {
        User u = persist(newUser("U"));
        Idea idea1 = persist(newIdea("Idea-A"));
        Idea idea2 = persist(newIdea("Idea-B"));

        persist(newProposal(u, idea1, false, IdeaStatus.DRAFT));      // should count
        persist(newProposal(u, idea2, true,  IdeaStatus.UNDERREVIEW)); // deleted

        assertTrue(proposalRepository.existsByIdea_IdeaIdAndDeletedFalse(idea1.getIdeaId()));
        assertFalse(proposalRepository.existsByIdea_IdeaIdAndDeletedFalse(idea2.getIdeaId()));
        assertFalse(proposalRepository.existsByIdea_IdeaIdAndDeletedFalse(999999)); // non-existent idea
    }

    @Test
    @DisplayName("findByProposalIdAndDeletedFalse: returns only when not deleted")
    void findByProposalIdAndDeletedFalse() {
        User u = persist(newUser("U2"));
        Idea idea = persist(newIdea("Idea-X"));

        Proposal active  = persist(newProposal(u, idea, false, IdeaStatus.DRAFT));
        Proposal deleted = persist(newProposal(u, idea, true,  IdeaStatus.DRAFT));

        assertTrue(proposalRepository.findByProposalIdAndDeletedFalse(active.getProposalId()).isPresent());
        assertFalse(proposalRepository.findByProposalIdAndDeletedFalse(deleted.getProposalId()).isPresent());
    }

    @Test
    @DisplayName("existsByProposalIdAndDeletedFalseAndIdeaStatus: respects deleted and status filters")
    void existsByIdDeletedFalseAndStatus() {
        User u = persist(newUser("U3"));
        Idea idea = persist(newIdea("Idea-Y"));

        Proposal p1 = persist(newProposal(u, idea, false, IdeaStatus.UNDERREVIEW));
        Proposal p2 = persist(newProposal(u, idea, false, IdeaStatus.SUBMITTED));
        Proposal p3 = persist(newProposal(u, idea, true,  IdeaStatus.UNDERREVIEW)); // deleted

        assertTrue(proposalRepository.existsByProposalIdAndDeletedFalseAndIdeaStatus(
                p1.getProposalId(), IdeaStatus.UNDERREVIEW));
        assertFalse(proposalRepository.existsByProposalIdAndDeletedFalseAndIdeaStatus(
                p2.getProposalId(), IdeaStatus.UNDERREVIEW)); // wrong status
        assertFalse(proposalRepository.existsByProposalIdAndDeletedFalseAndIdeaStatus(
                p3.getProposalId(), IdeaStatus.UNDERREVIEW)); // deleted
    }

    @Test
    @DisplayName("findForUpdate: pessimistic write lock query returns non-deleted proposal")
    void findForUpdate_pessimistic() {
        User u = persist(newUser("LockUser"));
        Idea idea = persist(newIdea("Lock-Idea"));
        Proposal p = persist(newProposal(u, idea, false, IdeaStatus.DRAFT));

        var locked = proposalRepository.findForUpdate(p.getProposalId());
        assertTrue(locked.isPresent());
        assertEquals(p.getProposalId(), locked.get().getProposalId());
        // Note: actual lock contention requires multi-tx/threads and is DB-specific (not covered here).
    }

    
}
