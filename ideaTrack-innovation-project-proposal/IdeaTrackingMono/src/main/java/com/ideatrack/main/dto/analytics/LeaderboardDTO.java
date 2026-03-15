package com.ideatrack.main.dto.analytics;


import com.ideatrack.main.data.Constants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDTO {
	
	private Integer userId;
	private String userName;
	private Constants.Role role;
	private int xp;
	private int rank; 

}
