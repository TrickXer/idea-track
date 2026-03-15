package com.ideatrack.main.service;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.ideatrack.main.data.Proposal;

//Done by vibhuti

public class ProposalSpecifications {

    private ProposalSpecifications() {} // utility class

    public static Specification<Proposal> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Proposal> hasIdeaId(Integer ideaId) {
        return (root, query, cb) -> cb.equal(root.get("ideaId"), ideaId);
    }

    public static Specification<Proposal> createdBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) ->
            cb.between(root.get("createdAt"), from.atStartOfDay(), to.atTime(23, 59, 59));
    }

    public static Specification<Proposal> searchText(String text) {
        return (root, query, cb) -> {
            String like = "%" + text.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like)
            );
        };
    }
}