package com.ideatrack.main.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * One Proposal can have many Objectives.
 * Each Objective can store exactly one "proof" (metadata fields).
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "objective",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_objective_proposal_seq", columnNames = {"proposal_id", "objective_seq"})
    }
)
public class Objectives {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Surrogate PK (recommended)

    /** FK → Proposal (required) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    @JsonIgnore
    private Proposal proposal;

    /**
     * Per‑proposal incremental number (1..n).
     * Keep this if you need stable ordering/IDs visible to users.
     */
    @NotNull
    @Column(name = "objective_seq", nullable = false)
    private Integer objectiveSeq;

    /** <= 150 chars */
    @NotBlank
    @Column(length = 150, nullable = false)
    private String title;

    /** <= 2000 chars */
    @NotBlank
    @Column(length = 2000, nullable = false)
    private String description;

    /** Business flag: exactly one objective per proposal must be mandatory (enforced in service) */
   // @NotNull
    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory;

    // ---- Single "proof" metadata (one objective → one proof) ----
    @Column(length = 255)
    private String proofFileName;      // e.g., "proof.pdf" or "proof.jpg"

    @Column(length = 512)
    private String proofFilePath;      // e.g., uploads/proposals/{proposalId}/objectives/{objectiveSeq}/proof.{ext}

    @Column(length = 64)
    private String proofContentType;   // "application/pdf" or "image/jpeg"

    private Long proofSizeBytes;       // <= 25 MB (validated in service)

    // ---- Auditing ----
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}