package com.ideatrack.main.dto.proposal;

import java.time.LocalDate;

//Done by vibhuti

/**
 * Used internally between Controller → Service for GET /api/v1/proposals filters.
 */
public class ProposalListFilterDTO {
    private String status;     // e.g., "Draft", "Submitted"
    private Integer ideaId;
    private LocalDate from;    // date window start (applied to createdAt by default)
    private LocalDate to;      // date window end
    private String search;     // search across objective or idea title

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getIdeaId() { return ideaId; }
    public void setIdeaId(Integer ideaId) { this.ideaId = ideaId; }

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
}
