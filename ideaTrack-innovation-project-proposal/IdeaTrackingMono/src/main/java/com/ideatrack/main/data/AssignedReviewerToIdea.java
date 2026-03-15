package com.ideatrack.main.data;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "assigned_reviewer_to_idea")
public class AssignedReviewerToIdea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "reviewerId", referencedColumnName = "userId")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "categoryId", referencedColumnName = "categoryId")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "ideaId", referencedColumnName = "ideaId")
    private Idea idea;

    private Integer stage;   // ✅ stage reviewer is assigned to

    private String feedback;
    private String refine;
    private String decision;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;   // ✅ defaults to false

}
