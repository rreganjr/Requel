package com.rreganjr.requel.project.impl;

import com.rreganjr.requel.project.Requirement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequirementWorkflowTest {
    private RequirementImpl draft() {
        return new RequirementImpl(new IdeaImpl(1L, "An exportable report", null, null),
            "The system shall export reports.", "author");
    }
    private void passAll(RequirementImpl r) {
        for (Department d : Department.values()) r.review(d, Result.PASSED, "Reviewed", "reviewer", d.name());
    }
    @Test void approvalRequiresEveryDepartmentToPass() {
        var r = draft();
        r.review(Department.QUALITY, Result.PASSED, "", "analyst", "Analyst");
        assertThrows(IllegalStateException.class, r::approve);
        r.review(Department.FEASIBILITY, Result.PASSED, "", "engineer", "Engineer");
        r.review(Department.TESTABILITY, Result.FAILED, "Not measurable", "tester", "TestEngineer");
        assertFalse(r.allReviewsPassed());
        assertThrows(IllegalStateException.class, r::approve);
        r.review(Department.TESTABILITY, Result.PASSED, "Measurable now", "tester", "TestEngineer");
        r.approve();
        assertEquals(Status.APPROVED, r.getStatus());
    }
    @Test void changingApprovedContentInvalidatesAllReviews() {
        var r = draft(); passAll(r); r.approve();
        r.revise("The system shall export reports as PDF.");
        assertEquals(Status.DRAFT, r.getStatus());
        assertTrue(r.getReviews().isEmpty());
        assertThrows(IllegalStateException.class, r::approve);
    }
    @Test void unchangedContentPreservesReviews() {
        var r = draft(); passAll(r); r.approve();
        r.revise(r.getShallText());
        assertTrue(r.allReviewsPassed());
        assertEquals(Status.APPROVED, r.getStatus());
    }
    @Test void reopeningApprovalRequiresFreshReviews() {
        var r = draft(); passAll(r); r.approve(); r.reopen();
        assertEquals(Status.DRAFT, r.getStatus());
        assertFalse(r.allReviewsPassed());
        assertThrows(IllegalArgumentException.class, () -> r.review(Department.QUALITY, Result.NOT_REVIEWED, "", "user", "Analyst"));
    }
}
