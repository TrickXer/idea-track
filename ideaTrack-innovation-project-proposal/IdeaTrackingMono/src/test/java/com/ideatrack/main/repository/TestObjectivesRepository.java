package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.*;
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
class TestObjectivesRepository {

	    @Autowired
	    private IProposalRepository proposalRepository;

	    @Autowired
	    private TestEntityManager em;

	    // ------------------------------- Helpers -------------------------------

	    private User newUser(String name) {
	        User u = new User();
	        u.setName(name);
	        u.setEmail(name.toLowerCase() + "@test.com");
	        u.setRole(Constants.Role.EMPLOYEE);
	        u.setDeleted(false);
	        return u;
	    }

	    private Idea newIdea() {
	        Idea i = new Idea();
	        i.setTitle("Idea-" + System.nanoTime());
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
	        // set other optional fields if your entity requires them
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

	    private <T> T persist(T entity) {
	        return em.persistFlushFind(entity);
	    }

	    // ------------------------------- Tests ---------------------------------

	    @Test
	    @DisplayName("countByUser: should count proposals authored by a user")
	    void countByUser_shouldCountAllProposalsForUser() {
	        User alice = persist(newUser("Alice"));
	        User bob   = persist(newUser("Bob"));

	        Idea i1 = persist(newIdea());
	        Idea i2 = persist(newIdea());
	        Idea i3 = persist(newIdea());
	        Idea i4 = persist(newIdea());

	        // 3 for Alice, 1 for Bob (include a deleted one to see behavior; countByUser does not filter by deleted)
	        persist(newProposal(alice, i1, false, IdeaStatus.DRAFT));
	        persist(newProposal(alice, i2, true,  IdeaStatus.SUBMITTED));
	        persist(newProposal(alice, i3, false, IdeaStatus.UNDERREVIEW));
	        persist(newProposal(bob,   i4, false, IdeaStatus.DRAFT));

	        int aliceCount = proposalRepository.countByUser(alice);
	        int bobCount   = proposalRepository.countByUser(bob);

	        assertEquals(3, aliceCount);
	        assertEquals(1, bobCount);
	    }

	    @Test
	    @DisplayName("existsByIdea_IdeaIdAndDeletedFalse: true only for non-deleted proposals of the idea")
	    void existsByIdeaIdAndDeletedFalse() {
	        User u = persist(newUser("U"));
	        Idea idea = persist(newIdea());
	        Idea idea2 = persist(newIdea());

	        // One non-deleted proposal for idea
	        persist(newProposal(u, idea, false, IdeaStatus.DRAFT));
	        // Deleted proposal for idea2
	        persist(newProposal(u, idea2, true, IdeaStatus.DRAFT));

	        boolean exists1 = proposalRepository.existsByIdea_IdeaIdAndDeletedFalse(idea.getIdeaId());
	        boolean exists2 = proposalRepository.existsByIdea_IdeaIdAndDeletedFalse(idea2.getIdeaId());
	        boolean exists3 = proposalRepository.existsByIdea_IdeaIdAndDeletedFalse(999999);

	        assertTrue(exists1);
	        assertFalse(exists2);
	        assertFalse(exists3);
	    }

	    @Test
	    @DisplayName("findByProposalIdAndDeletedFalse: returns only when not deleted")
	    void findByProposalIdAndDeletedFalse() {
	        User u = persist(newUser("U1"));
	        Idea idea = persist(newIdea());

	        Proposal nonDeleted = persist(newProposal(u, idea, false, IdeaStatus.DRAFT));
	        Proposal deleted    = persist(newProposal(u, idea, true,  IdeaStatus.DRAFT));

	        assertTrue(proposalRepository.findByProposalIdAndDeletedFalse(nonDeleted.getProposalId()).isPresent());
	        assertFalse(proposalRepository.findByProposalIdAndDeletedFalse(deleted.getProposalId()).isPresent());
	    }

	    @Test
	    @DisplayName("existsByProposalIdAndDeletedFalseAndIdeaStatus: respects both deleted flag and status")
	    void existsByIdDeletedFalseAndStatus() {
	        User u = persist(newUser("U2"));
	        Idea idea = persist(newIdea());

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
	        Idea idea = persist(newIdea());
	        Proposal p = persist(newProposal(u, idea, false, IdeaStatus.DRAFT));

	        var locked = proposalRepository.findForUpdate(p.getProposalId());
	        assertTrue(locked.isPresent());
	        assertEquals(p.getProposalId(), locked.get().getProposalId());
	        // Note: Actual lock contention tests require multi-threaded / separate transactions.
	    }
	}