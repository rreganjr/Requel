/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.query;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link AnnotationQueryController}.
 *
 * The single endpoint, GET /api/annotations, accepts three query parameters
 * (projectName, entityType, entityId), resolves the annotatable entity via
 * the registry and EntityManager, then returns all attached notes and issues.
 *
 * Note on mocking: {@code AnnotationCommandRegistrar.toNoteDto} and
 * {@code toIssueDto} cast their argument to {@code NoteImpl}/{@code IssueImpl}.
 * {@code mock(NoteImpl.class)} produces a CGLIB subclass that passes the cast,
 * allowing per-field stubbing without touching the database.
 *
 * Collaborators mocked:
 * - {@code AnnotatableTypeRegistry} — discriminator → entity class resolution
 * - {@code EntityManager}           — entity lookup by class and id
 *
 * Scenarios covered:
 * - Unknown entity type → 400 BAD_REQUEST
 * - Entity not found (null from EntityManager) → 404 Not Found
 * - Entity found with no annotations → 200 with empty notes and issues arrays
 * - Entity found with one note and one issue → 200 with populated arrays, sorted by id
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class AnnotationQueryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AnnotatableTypeRegistry annotatableTypeRegistry;
    @MockBean EntityManager entityManager;

    // -------------------------------------------------------------------------
    // Error cases
    // -------------------------------------------------------------------------

    @Test
    void unknownEntityTypeReturnsBadRequest() throws Exception {
        when(annotatableTypeRegistry.resolveEntityType("UnknownType"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/annotations")
                        .param("projectName", "TestProject")
                        .param("entityType", "UnknownType")
                        .param("entityId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void entityNotFoundReturnsNotFound() throws Exception {
        when(annotatableTypeRegistry.resolveEntityType("Goal"))
                .thenReturn(Optional.of(stubAnnotatableClass()));
        when(entityManager.find(any(), eq(42L))).thenReturn(null);

        mockMvc.perform(get("/api/annotations")
                        .param("projectName", "TestProject")
                        .param("entityType", "Goal")
                        .param("entityId", "42"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Success cases
    // -------------------------------------------------------------------------

    @Test
    void entityWithNoAnnotationsReturnsEmptyLists() throws Exception {
        Annotatable annotatable = stubAnnotatable(Set.of());

        mockMvc.perform(get("/api/annotations")
                        .param("projectName", "TestProject")
                        .param("entityType", "Goal")
                        .param("entityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes.length()").value(0))
                .andExpect(jsonPath("$.issues.length()").value(0));
    }

    @Test
    void entityWithNoteAndIssueReturnsPopulatedArraysSortedById() throws Exception {
        NoteImpl note = mock(NoteImpl.class);
        when(note.getId()).thenReturn(10L);
        when(note.getText()).thenReturn("A note");
        when(note.getCreatedBy()).thenReturn(null);

        IssueImpl issue = mock(IssueImpl.class);
        when(issue.getId()).thenReturn(20L);
        when(issue.getText()).thenReturn("An issue");
        when(issue.isMustBeResolved()).thenReturn(true);
        when(issue.isResolved()).thenReturn(false);
        when(issue.getResolvedByUser()).thenReturn(null);
        when(issue.getResolvedByPosition()).thenReturn(null);
        when(issue.getCreatedBy()).thenReturn(null);
        when(issue.getPositions()).thenReturn(Set.of());

        stubAnnotatable(Set.of(note, issue));

        mockMvc.perform(get("/api/annotations")
                        .param("projectName", "TestProject")
                        .param("entityType", "Goal")
                        .param("entityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes.length()").value(1))
                .andExpect(jsonPath("$.notes[0].id").value(10))
                .andExpect(jsonPath("$.notes[0].text").value("A note"))
                .andExpect(jsonPath("$.issues.length()").value(1))
                .andExpect(jsonPath("$.issues[0].id").value(20))
                .andExpect(jsonPath("$.issues[0].text").value("An issue"))
                .andExpect(jsonPath("$.issues[0].mustBeResolved").value(true));
    }

    @Test
    void multipleNotesAreSortedById() throws Exception {
        NoteImpl n1 = stubNote(30L, "Third");
        NoteImpl n2 = stubNote(10L, "First");
        NoteImpl n3 = stubNote(20L, "Second");

        stubAnnotatable(Set.of(n1, n2, n3));

        mockMvc.perform(get("/api/annotations")
                        .param("projectName", "TestProject")
                        .param("entityType", "Goal")
                        .param("entityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes.length()").value(3))
                .andExpect(jsonPath("$.notes[0].id").value(10))
                .andExpect(jsonPath("$.notes[1].id").value(20))
                .andExpect(jsonPath("$.notes[2].id").value(30));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Class<? extends Annotatable> stubAnnotatableClass() {
        return (Class<? extends Annotatable>) (Class<?>) Annotatable.class;
    }

    /** Wire registry + EntityManager to return the given annotatable for entityId=1. */
    private Annotatable stubAnnotatable(Set<Annotation> annotations) {
        Annotatable annotatable = mock(Annotatable.class);
        when(annotatable.getAnnotations()).thenReturn(annotations);

        Class<? extends Annotatable> clazz = stubAnnotatableClass();
        when(annotatableTypeRegistry.resolveEntityType("Goal")).thenReturn(Optional.of(clazz));
        when(entityManager.find(any(), eq(1L))).thenReturn(annotatable);

        return annotatable;
    }

    private NoteImpl stubNote(Long id, String text) {
        NoteImpl note = mock(NoteImpl.class);
        when(note.getId()).thenReturn(id);
        when(note.getText()).thenReturn(text);
        when(note.getCreatedBy()).thenReturn(null);
        return note;
    }
}
