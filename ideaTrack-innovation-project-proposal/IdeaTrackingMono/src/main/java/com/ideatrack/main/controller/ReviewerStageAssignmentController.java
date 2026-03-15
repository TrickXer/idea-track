package com.ideatrack.main.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ideatrack.main.dto.reviewerAssignment.AssignedReviewerDTO;
import com.ideatrack.main.dto.reviewerAssignment.AvailableReviewersDTO;
import com.ideatrack.main.dto.reviewerAssignment.CategoryDTO;
import com.ideatrack.main.dto.reviewerAssignment.ReviewerAssignmentDTO;
import com.ideatrack.main.service.ReviewerStageAssignmentService;

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
@RequestMapping("/api/reviewerAssignment")
public class ReviewerStageAssignmentController {

	private final ReviewerStageAssignmentService servObj;

	public ReviewerStageAssignmentController(ReviewerStageAssignmentService servObj) {
		this.servObj = servObj;
	}
	
	
//	Returns the list of available/unassigned Reviewer from the Reviewers table.
	@GetMapping("/getAvailableReviewersList/{deptId}")
	public ResponseEntity<List<AvailableReviewersDTO>> getAvailableReviewersList(@PathVariable Integer deptId){
		
		List<AvailableReviewersDTO> availableReviewerDTOList = servObj.getAvailableReviewersList(deptId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(availableReviewerDTOList);
		
	}
	
	
	
//	Returns the categories and their respective count of Stage for the given Department Id.
	@GetMapping("/getCategoriesAndStageCountByCategory/{deptId}")
	public ResponseEntity<List<CategoryDTO>> getCategoriesAndStageCountByCategory(@PathVariable Integer deptId){
		
		List<CategoryDTO> categoryAndStageCountList = servObj.getCategoriesAndStageCountByCategory(deptId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(categoryAndStageCountList);
	}
	
	
	
//	Assigns the Reviewer to the particular Stage in the Category.
	@PostMapping("/assignReviewerToStage")
	public ResponseEntity<String> assignReviewerToStage(@RequestBody ReviewerAssignmentDTO request) {
        
		String returnString = "Reviewer assignment failed.";
		
        boolean status = servObj.assignReviewerToStage(request.getReviewerId(), request.getCategoryId(), request.getStageNo());
        
        if(status)
        {
        	returnString = "Reviewer assignment created successfully.";
        }
        
        return ResponseEntity
        		.status(HttpStatus.CREATED)
                .body(returnString);
    
    }
	
	
	
//	Returns the list of Reviewers and details about which Stage and Category they are assigned.
	@GetMapping("/assignedReviewerDetails")
	public ResponseEntity<List<AssignedReviewerDTO>> assignedReviewerDetails(){
		
		List<AssignedReviewerDTO> assignmentList = servObj.assignedReviewerDetails();
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(assignmentList);
	}
	
	
	
//	Removes the Reviewer assignment from the Stage of Category.
	@DeleteMapping("/removeReviewerFromStage")
	public ResponseEntity<String> removeReviewerFromStage(@RequestParam Integer reviewerId, @RequestParam Integer categoryId, @RequestParam Integer stageNo){
		
		String returnString = "Failed to remove the Reviewer assignment with Reviewer ID: "+reviewerId;
		
		boolean status = servObj.removeReviewerFromStage(reviewerId, categoryId, stageNo);
		
		if(status)
		{
			returnString = "Successfully removed the assignment for Reviewer ID: " + reviewerId + ", in Category ID: " + categoryId + ", at Stage: " + stageNo;
		}
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(returnString);
	}
	
	
}
