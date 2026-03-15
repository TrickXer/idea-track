package com.ideatrack.main.dto.profilegamification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates owner (idea.user) header info for the hierarchy response.
 * Keeps owner-related fields grouped under a single "owner" object in the payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerInfoDTO {
    private Integer ownerUserId;
    private String  ownerName;
    private String  ownerRole;
    private String  ownerDept;

    private String  ownerPhoneNo;
    private String  ownerEmail;
    private String  ownerProfileUrl;
    private String  ownerBio;
}