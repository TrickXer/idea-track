package com.ideatrack.main.dto.admin;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

//Done by vibhuti

//Request to remove one or more reviewers from a proposal stage.
 
@Builder
public class AdminRemoveReviewersRequest {

    @NotEmpty(message = "reviewerIds cannot be empty")
    private List<Long> reviewerIds;

    public List<Long> getReviewerIds() {
        return reviewerIds;
    }
    public void setReviewerIds(List<Long> reviewerIds) {
        this.reviewerIds = reviewerIds;
    }
	
}
