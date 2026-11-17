package com.rreganjr.requel.project;

import java.time.Instant;

/** A versioned requirement whose content must pass three independent department reviews. */
public interface Requirement {
    enum Status { DRAFT, IN_REVIEW, APPROVED, PUBLISHED, REJECTED }
    enum Department { QUALITY, FEASIBILITY, TESTABILITY }
    enum Result { NOT_REVIEWED, PASSED, FAILED }
    Long getId();
    long getVersion();
    Long getProjectId();
    String getShallText();
    Status getStatus();
    Idea getIdea();
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
