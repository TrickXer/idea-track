package com.ideatrack.main.dto.reviewer;

import lombok.Data;

@Data
public class ProposalDiscussionRequest {

 private Integer userId;
 private Integer stageId;
 private String text;
 private Integer replyParent;  // nullable
}