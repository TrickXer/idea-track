package com.ideatrack.main.dto;

import java.util.List;


import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagedResponse<T> {
 private List<T> content;
 private int page;
 private int size;
 private long totalElements;
 private int totalPages;
 private boolean first;
 private boolean last;
}
