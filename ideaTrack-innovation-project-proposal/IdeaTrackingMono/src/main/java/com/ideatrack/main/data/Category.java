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
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;

    @ManyToOne
    @JoinColumn(name = "deptId", referencedColumnName = "deptId")
    private Department department;

    private String name;

    @ManyToOne
    @JoinColumn(name = "createdByAdmin", referencedColumnName = "userId")
    private User createdByAdmin;

    private int reviewerCountPerStage;
    private int stageCount;

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
