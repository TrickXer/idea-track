package com.ideatrack.main.service;

import org.springframework.data.jpa.domain.Specification;

import com.ideatrack.main.data.Objectives;

public class ObjectiveReviewSpecifications {
    private ObjectiveReviewSpecifications() {}

    public static Specification<Objectives> belongsToProposal(Integer proposalId) {
        return (root, query, cb) -> cb.equal(root.get("proposal").get("proposalId"), proposalId);
    }
    public static Specification<Objectives> hasProof(Boolean hasProof) {
        if (hasProof == null) return (root, query, cb) -> cb.conjunction();
        return hasProof
                ? (root, query, cb) -> cb.isNotNull(root.get("proofFilePath"))
                : (root, query, cb) -> cb.isNull(root.get("proofFilePath"));
    }
    public static Specification<Objectives> mandatoryEquals(Boolean mandatory) {
        if (mandatory == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("mandatory"), mandatory);
    }
    public static Specification<Objectives> proofContentTypeEquals(String contentType) {
        if (contentType == null || contentType.isBlank()) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("proofContentType"), contentType);
    }
    public static Specification<Objectives> searchText(String q) {
        if (q == null || q.isBlank()) return (root, query, cb) -> cb.conjunction();
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like)
        );
    }

}