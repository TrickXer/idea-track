
package com.ideatrack.main.data;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"category", "reviewer"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@EntityListeners(AuditingEntityListener.class)

public class ReviewerCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewer_category_id")
    @EqualsAndHashCode.Include
    private Integer reviewerCategoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoryId", referencedColumnName = "categoryId", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewerId", referencedColumnName = "userId", nullable = false)
    private User reviewer;

    @Column(name = "assignedStageId", nullable = false)
    private Integer assignedStageId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
