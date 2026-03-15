package com.ideatrack.main.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.ReviewerCategory;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.reviewerAssignment.AssignedReviewerDTO;
import com.ideatrack.main.dto.reviewerAssignment.AvailableReviewersDTO;
import com.ideatrack.main.dto.reviewerAssignment.CategoryDTO;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IReviewerStageAssignmentRepository;
import com.ideatrack.main.repository.IUserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class ReviewerStageAssignmentService {

	private final IReviewerStageAssignmentRepository reviewerStageRepo;
	private final ICategoryRepository categoryRepo;
	private final IUserRepository userRepo;
	private final ModelMapper modelMapper;

	public ReviewerStageAssignmentService(IReviewerStageAssignmentRepository reviewerStageRepo,
										  ICategoryRepository categoryRepo,
										  IUserRepository userRepo,
										  ModelMapper modelMapper) {
		this.reviewerStageRepo = reviewerStageRepo;
		this.categoryRepo = categoryRepo;
		this.userRepo = userRepo;
		this.modelMapper = modelMapper;
	}

	
	
//	Returns the list of available/unassigned Reviewer from the Reviewers table.
	public List<AvailableReviewersDTO> getAvailableReviewersList(Integer deptId) {

		List<User> allReviewersInDept = userRepo.findByRoleAndDepartment_DeptIdAndDeletedFalse(Constants.Role.REVIEWER, deptId);

		Set<Integer> assignedReviewerIds = reviewerStageRepo.findByDeletedFalse().stream().map(user -> user.getReviewer().getUserId()).collect(Collectors.toSet());
		
		List<AvailableReviewersDTO> availableReviewersDTOList = allReviewersInDept.stream().filter(user -> !assignedReviewerIds.contains(user.getUserId()))
										.map(user -> {
											AvailableReviewersDTO dto = modelMapper.map(user, AvailableReviewersDTO.class);
											
											if (user.getDepartment() != null) {
								                dto.setDeptName(user.getDepartment().getDeptName());
								            }
											return dto;								
										})
										.toList();
		
		log.info("List of unassigned reviewer: " + availableReviewersDTOList);
		
		return availableReviewersDTOList;
	}
	
	
	
	
//	Returns the categories and their respective count of Stage for the given Department Id.
	public List<CategoryDTO> getCategoriesAndStageCountByCategory(Integer deptId) {
		
		List<Category> categories = categoryRepo.findByDepartment_DeptIdAndDeletedFalse(deptId);
		
		List<CategoryDTO> categoryAndStageCountList = categories.stream().map(cat -> new CategoryDTO(cat.getCategoryId(), cat.getName(),cat.getStageCount())).toList();
		
		log.info("For Department ID: {}, Category and Stage List: {}", deptId, categoryAndStageCountList);
		
		return categoryAndStageCountList;
		
	}
	
	
//	Assigns the Reviewer to the particular Stage in the Category.
	public boolean assignReviewerToStage(Integer reviewerId, Integer categoryId, Integer stageNo) {

		Category category = categoryRepo.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFound("Category with ID " + categoryId + " not found"));
            
        User reviewer = userRepo.findById(reviewerId)
            .orElseThrow(() -> new UserNotFoundException("User with ID " + reviewerId + " not found"));

        boolean exists = reviewerStageRepo.existsByReviewer_UserIdAndDeletedFalse(reviewerId);
        if (exists) {
            throw new IllegalStateException("Reviewer is already assigned to a stage.");
        }

        if (stageNo < 1 || stageNo > category.getStageCount()) {
            throw new IllegalArgumentException("Invalid stage number. This category only has " + category.getStageCount() + " stages.");
        }


        ReviewerCategory assignment = new ReviewerCategory();
        
        assignment.setCategory(category);
        assignment.setReviewer(reviewer);
        assignment.setAssignedStageId(stageNo);

        reviewerStageRepo.save(assignment);
        
        log.info("Assigned Reviewer {}, to Category {}, at Stage {}.", reviewerId, categoryId, stageNo);
        
        return true;
    }
	
	
	
	
//	Returns the list of Reviewers and details about which Stage and Category they are assigned.
	public List<AssignedReviewerDTO> assignedReviewerDetails(){
		
		List<ReviewerCategory> assignedReviewerList = reviewerStageRepo.findByDeletedFalse();
		
		List<AssignedReviewerDTO> assignedReviewerDTOList = assignedReviewerList.stream().map(this::convertToDTO).toList();
		
		log.info("List of assigned reviewers: " + assignedReviewerDTOList);
		
		return assignedReviewerDTOList;
				
	}
	
	
//	Helper function for assignedReviewerDetails() function, to convert ReviewerCategory to AssignedReviewerDTO.
	private AssignedReviewerDTO convertToDTO (ReviewerCategory reviewerCategory) {
		
		return new AssignedReviewerDTO(reviewerCategory.getReviewer().getUserId(), reviewerCategory.getReviewer().getName(), reviewerCategory.getCategory().getCategoryId(), reviewerCategory.getCategory().getName(), reviewerCategory.getAssignedStageId());
	}
	
	
	
	
//	Removes the Reviewer assignment from the Stage of Category.()
	public boolean removeReviewerFromStage(Integer reviewerId, Integer categoryId, Integer stageNo) {

	    ReviewerCategory assignment = reviewerStageRepo.findByReviewer_UserIdAndCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(reviewerId, categoryId, stageNo)
	    								.orElseThrow(() -> new ResourceNotFoundException("No active assignment found for Reviewer ID: " + reviewerId + " in Category ID: " + categoryId + " at Stage: " + stageNo));

	    assignment.setDeleted(true);
	    reviewerStageRepo.save(assignment);
	    
	    log.info("Soft deleted assignment for Reviewer ID: {}, in Category ID: {}, at Stage: {}", reviewerId, categoryId, stageNo);
	    
	    return true;
	}

	
	
}
