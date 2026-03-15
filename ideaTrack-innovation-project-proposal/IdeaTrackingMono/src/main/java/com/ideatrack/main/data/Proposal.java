
package com.ideatrack.main.data;

//imports omitted for brevity
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "proposal")
public class Proposal {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Integer proposalId;

 @ManyToOne
 @JoinColumn(name = "userId", referencedColumnName = "userId")
 private User user;

 @ManyToOne
 @JoinColumn(name = "ideaId", referencedColumnName = "ideaId")
 private Idea idea;

 private Long budget;

 private LocalDate timeLineStart;
 private LocalDate timeLineEnd;

 /** One Proposal → many Objectives (ordered by objectiveSeq) */
 @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
 @Builder.Default
 private List<Objectives> objectives = new ArrayList<>();

 @Version
 @Column(nullable = false)
 private Integer version;
 /** One Proposal → many Objectives (ordered by objectiveSeq) */

 @Enumerated(EnumType.STRING)
 @Column(name = "ideaStatus", nullable = false)
 private Constants.IdeaStatus ideaStatus;
 
 @CreatedDate
 @Column(nullable = false, updatable = false)
 private LocalDateTime createdAt;

 @LastModifiedDate
 @Column(nullable = false)
 private LocalDateTime updatedAt;

 @Column(nullable = false)
 @Builder.Default
 private boolean deleted = false;

 /* Optional convenience helpers */
 public void addObjective(Objectives objective) {
     objective.setProposal(this);
     this.objectives.add(objective);
 }

 public void removeObjective(Objectives objective) {
     this.objectives.remove(objective);
     objective.setProposal(null);
 }

 // Your placeholders (consider implementing or removing if unused)
 public String getTitle() { return idea.getTitle(); }


}
