package com.rreganjr.requel.requirements;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Requirement.Department;
import com.rreganjr.requel.project.Requirement.Result;
import com.rreganjr.requel.project.Requirement.Status;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.RequirementImpl;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.WorkflowRole;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequirementControllerTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final CurrentUserResolver users = mock(CurrentUserResolver.class);
    private final EntityManager em = mock(EntityManager.class);
    private final Project project = mock(Project.class);
    private final User user = mock(User.class);
    private RequirementController controller(boolean member) {
        var c = new RequirementController(projects, users);
        ReflectionTestUtils.setField(c, "em", em);
        when(projects.findProjectByName("alpha")).thenReturn(project);
        when(users.resolve()).thenReturn(user);
        when(project.getId()).thenReturn(1L);
        when(project.getUserStakeholder(user)).thenReturn(member ? mock(UserStakeholder.class) : null);
        return c;
    }
    @Test void nonStakeholdersCannotReadRequirements() {
        var c = controller(false);
        assertThrows(AuthorizationException.class, () -> c.get("alpha", 7));
        verifyNoInteractions(em);
    }
    @Test void idsFromAnotherProjectAreNotExposed() {
        var c = controller(true);
        var r = new RequirementImpl(2L, null, "The system shall export.", "author");
        when(em.find(RequirementImpl.class, 7L)).thenReturn(r);
        var error = assertThrows(ResponseStatusException.class, () -> c.get("alpha", 7));
        assertEquals(404, error.getStatusCode().value());
    }
    @ParameterizedTest
    @CsvSource(value = {"ANALYST, QUALITY", "ENGINEER, FEASIBILITY",
            "TEST_ENGINEER, TESTABILITY", "PRODUCT_MANAGER, NONE"}, nullValues = "NONE")
    void workflowRoleRestrictsDepartmentReviews(WorkflowRole role, Department allowedDepartment) {
        var c = controller(true);
        when(user.getWorkflowRole()).thenReturn(role);
        when(user.getUsername()).thenReturn("reviewer");
        var requirement = requirementInProject();

        var view = c.get("alpha", 7);
        assertEquals(allowedDepartment == null ? List.of() : List.of(allowedDepartment),
                view.get("allowedReviewDepartments"));
        assertEquals(role == WorkflowRole.PRODUCT_MANAGER, view.get("canApproveOrReopen"));

        for (Department department : Department.values()) {
            var request = new RequirementController.ReviewRequest(department, Result.PASSED, null, 0L);
            if (department == allowedDepartment) {
                c.review("alpha", 7, request);
                var review = requirement.getReviews().get(department);
                assertEquals("reviewer", review.getReviewer());
                assertEquals(role.getDisplayName(), review.getReviewerRole());
            } else {
                assertThrows(AuthorizationException.class, () -> c.review("alpha", 7, request));
                assertFalse(requirement.getReviews().containsKey(department));
            }
        }
    }

    @Test void reviewerOutsideTheProjectCannotSubmitEvenTheirOwnReview() {
        var c = controller(false);
        when(user.getWorkflowRole()).thenReturn(WorkflowRole.ANALYST);
        var request = new RequirementController.ReviewRequest(Department.QUALITY, Result.PASSED, null, 0L);
        assertThrows(AuthorizationException.class, () -> c.review("alpha", 7, request));
        verifyNoInteractions(em);
    }

    @Test void systemAdministratorCanSeeAndSubmitAllDepartmentReviews() {
        var c = controller(false);
        when(user.hasRole(SystemAdminUserRole.class)).thenReturn(true);
        when(user.getWorkflowRole()).thenReturn(WorkflowRole.ENGINEER);
        when(user.getUsername()).thenReturn("admin");
        var requirement = requirementInProject();

        assertEquals(List.of(Department.values()), c.get("alpha", 7).get("allowedReviewDepartments"));
        for (Department department : Department.values()) {
            var request = new RequirementController.ReviewRequest(department, Result.PASSED, null, 0L);
            c.review("alpha", 7, request);
            assertEquals("System Administrator", requirement.getReviews().get(department).getReviewerRole());
        }
        assertTrue(requirement.allReviewsPassed());
    }

    @Test void productManagerRoleIsRequiredForApprovalAndReopening() {
        var c = controller(true);
        when(user.getWorkflowRole()).thenReturn(WorkflowRole.ANALYST);
        assertThrows(AuthorizationException.class, () -> c.approve("alpha", 7, new RequirementController.Version(0L)));
        assertThrows(AuthorizationException.class, () -> c.reopen("alpha", 7, new RequirementController.Version(0L)));
        verifyNoInteractions(em);
    }

    @Test void productManagerCanApproveAfterAllReviewsPassAndReopenForFreshReview() {
        var c = controller(true);
        when(user.getWorkflowRole()).thenReturn(WorkflowRole.PRODUCT_MANAGER);
        var requirement = requirementInProject();
        var version = new RequirementController.Version(0L);
        assertThrows(IllegalStateException.class, () -> c.approve("alpha", 7, version));

        for (Department department : Department.values()) {
            requirement.review(department, Result.PASSED, null, "reviewer", "Reviewer");
        }
        c.approve("alpha", 7, version);
        assertEquals(Status.APPROVED, requirement.getStatus());
        c.reopen("alpha", 7, version);
        assertEquals(Status.DRAFT, requirement.getStatus());
        assertTrue(requirement.getReviews().isEmpty());
    }

    private RequirementImpl requirementInProject() {
        var requirement = new RequirementImpl(1L, null, "The system shall export.", "author");
        ReflectionTestUtils.setField(requirement, "id", 7L);
        when(em.find(RequirementImpl.class, 7L)).thenReturn(requirement);
        return requirement;
    }
}
