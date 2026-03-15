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
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "idea")
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ideaId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String problemStatement;

    private Integer stage;   
    
    @Column(columnDefinition = "TEXT")
    private String reviewerFeedback;

    @Enumerated(EnumType.STRING)
    private Constants.IdeaStatus ideaStatus;   

    @ManyToOne
    @JoinColumn(name = "categoryId", referencedColumnName = "categoryId")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    private User user;

    private String tag;
    private String thumbnailURL;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;   

}
