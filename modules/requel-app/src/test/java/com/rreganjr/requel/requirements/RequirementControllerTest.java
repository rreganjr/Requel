package com.rreganjr.requel.requirements;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.RequirementImpl;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
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
    @Test void managerPermissionIsRequiredForApproval() {
        var c = controller(true);
        when(project.getUserStakeholder(user).getStakeholderPermissions()).thenReturn(java.util.Set.of());
        assertThrows(AuthorizationException.class, () -> c.approve("alpha", 7, new RequirementController.Version(0L)));
        verifyNoInteractions(em);
    }
}
