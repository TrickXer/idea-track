package com.ideatrack.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DepartmentListResponse {
    private List<String> deptNames;
}


