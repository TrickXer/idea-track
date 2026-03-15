package com.ideatrack.main.data;


public class Constants {
	public enum Status{
		ACTIVE,
		INACTIVE
	}
	
	public enum Role {
        EMPLOYEE,
        REVIEWER,
        ADMIN,
        SUPERADMIN
    }
	
	public enum IdeaStatus{
		DRAFT, 
		SUBMITTED,
		UNDERREVIEW,
		ACCEPTED,
		REJECTED,
		PROJECTPROPOSAL,
		APPROVED,
		REFINE,
		PENDING
	}
	
	public enum VoteType{
		UPVOTE,
		DOWNVOTE
	}
	
	public enum ActivityType{
		COMMENT,
		VOTE,
		SAVE,
		REVIEWDISCUSSION,
		CURRENTSTATUS,
		FINALDECISION
	}
	
	public enum Scope{
		DEPARTMENT,
		CATEGORY,
		PERIOD
	}
}
 