package com.ideatrack.main.data;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @JsonIgnore
    private String password;
    private String name;

    @ManyToOne
    @JoinColumn(name = "deptId", referencedColumnName = "deptId")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Constants.Role role;

    @Enumerated(EnumType.STRING)
    private Constants.Status status;
    
    @Builder.Default
    private int totalXP=0;
    private String phoneNo;
    
    @Column(unique = true, nullable = false)
    private String email;
    private String profileUrl;
    private String bio;

    @ManyToOne
    @JoinColumn(name = "createdByUserId", referencedColumnName = "userId")
    private User createdByUser;

    private boolean profileCompleted;
    private String resetTokenHash;
    private LocalDateTime resetTokenExpiry;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

}
