package com.rreganjr.requel.project.impl;

import com.rreganjr.requel.project.Requirement;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

/** Persistent aggregate; transitions enforce review invalidation and approval prerequisites. */
@Entity
@Table(name = "workflow_requirements")
public class RequirementImpl implements Requirement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "idea_id") private IdeaImpl idea;
    @Column(name = "requirement_code", length = 255) private String requirementCode;
    @Lob @Column(name = "shall_text", nullable = false) private String shallText;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.DRAFT;
    @Column(name = "created_by", nullable = false) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @ElementCollection @CollectionTable(name = "workflow_reviews", joinColumns = @JoinColumn(name = "requirement_id"))
    @MapKeyColumn(name = "department") @MapKeyEnumerated(EnumType.STRING)
    private Map<Department, Review> reviews = new EnumMap<>(Department.class);

    protected RequirementImpl() { }
    public RequirementImpl(IdeaImpl idea, String text, String username) {
        this(Objects.requireNonNull(idea).getProjectId(), idea, text, username);
    }
    public RequirementImpl(Long projectId, IdeaImpl idea, String text, String username) {
        this.idea = idea;
        this.projectId = Objects.requireNonNull(projectId);
        this.shallText = validated(text);
        this.createdBy = username;
        this.createdAt = this.updatedAt = Instant.now();
    }
    private static String validated(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("A requirement needs a shall statement.");
        return text.strip();
    }
    public void revise(String text) {
        if (status == Status.PUBLISHED) throw new IllegalStateException("Published requirements cannot be edited.");
        String next = validated(text);
        if (!next.equals(shallText)) {
            shallText = next;
            reviews.clear();
            status = Status.DRAFT;
            touch();
        }
    }
    public void review(Department department, Result result, String comments, String username, String role) {
        if (status != Status.DRAFT && status != Status.IN_REVIEW && status != Status.REJECTED)
            throw new IllegalStateException("Reopen this requirement before reviewing it.");
        if (result == null || result == Result.NOT_REVIEWED) throw new IllegalArgumentException("Choose PASSED or FAILED.");
        reviews.put(Objects.requireNonNull(department), new Review(result, comments, username, role));
        status = reviews.values().stream().anyMatch(r -> r.result == Result.FAILED) ? Status.REJECTED : Status.IN_REVIEW;
        touch();
    }
    public boolean allReviewsPassed() {
        return Arrays.stream(Department.values()).allMatch(d -> reviews.containsKey(d) && reviews.get(d).result == Result.PASSED);
    }
    public void approve() {
        if (status != Status.IN_REVIEW || !allReviewsPassed()) throw new IllegalStateException("All three department reviews must pass before approval.");
        status = Status.APPROVED;
        touch();
    }
    public void reopen() {
        if (status != Status.APPROVED && status != Status.REJECTED) throw new IllegalStateException("Only approved or rejected requirements can be reopened.");
        reviews.clear();
        status = Status.DRAFT;
        touch();
    }
    private void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public Long getProjectId() { return projectId; }
    public IdeaImpl getIdea() { return idea; }
    public String getShallText() { return shallText; }
    public Status getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Map<Department, Review> getReviews() { return Collections.unmodifiableMap(reviews); }
    public String getRequirementCode() { return requirementCode == null ? "REQ-" + id : requirementCode; }
    public void setRequirementCode(String value) {
        if (value != null && !value.isBlank()) {
            if (value.strip().length() > 255) throw new IllegalArgumentException("Requirement ID is too long.");
            if (status == Status.PUBLISHED) throw new IllegalStateException("Published requirements cannot be edited.");
            requirementCode = value.strip();
        }
    }

    @Embeddable
    public static class Review {
        @Enumerated(EnumType.STRING) @Column(name = "result", nullable = false) private Result result;
        @Lob private String comments;
        private String reviewer;
        @Column(name = "reviewer_role") private String reviewerRole;
        @Column(name = "reviewed_at") private Instant reviewedAt;
        protected Review() { }
        Review(Result result, String comments, String reviewer, String role) {
            this.result = result; this.comments = comments; this.reviewer = reviewer;
            this.reviewerRole = role; this.reviewedAt = Instant.now();
        }
        public Result getResult() { return result; }
        public String getComments() { return comments; }
        public String getReviewer() { return reviewer; }
        public String getReviewerRole() { return reviewerRole; }
        public Instant getReviewedAt() { return reviewedAt; }
    }
}
