package com.ideatrack.main.data;

import jakarta.annotation.Nullable;
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
@Table(name = "user_activity")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userActivityId;

    @ManyToOne
    @JoinColumn(name = "ideaId", referencedColumnName = "ideaId")
    private Idea idea;

    @ManyToOne
    @JoinColumn(name = "userId", referencedColumnName = "userId")
    private User user;

    private String commentText;

    @Enumerated(EnumType.STRING)
    private Constants.VoteType voteType;

    private boolean savedIdea;

    private String event;
    
    @Builder.Default
    private int delta = 0;

    @ManyToOne
    @JoinColumn(name = "replyParentId", referencedColumnName = "userActivityId")
    private UserActivity replyParent;

    @Nullable
    private Integer stageId;
    
    @Enumerated(EnumType.STRING)
    private Constants.IdeaStatus decision;

    @Enumerated(EnumType.STRING)
    private Constants.ActivityType activityType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
    
    
}
 