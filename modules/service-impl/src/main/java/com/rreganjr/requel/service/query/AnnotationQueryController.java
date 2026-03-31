package com.rreganjr.requel.service.query;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.NoteDto;
import com.rreganjr.requel.service.command.AnnotationCommandRegistrar;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read endpoints for annotations on any annotatable entity.
 */
@RestController
@RequestMapping("/api/annotations")
public class AnnotationQueryController {

    private final AnnotatableTypeRegistry annotatableTypeRegistry;
    private final EntityManager entityManager;

    public AnnotationQueryController(AnnotatableTypeRegistry annotatableTypeRegistry,
                                     EntityManager entityManager) {
        this.annotatableTypeRegistry = annotatableTypeRegistry;
        this.entityManager = entityManager;
    }

    /**
     * GET /api/annotations?projectName={name}&entityType={type}&entityId={id}
     * Returns all notes and issues attached to the specified entity.
     */
    @GetMapping
    public ResponseEntity<AnnotationsDto> getAnnotations(
            @RequestParam String projectName,
            @RequestParam String entityType,
            @RequestParam Long entityId) {

        Class<? extends Annotatable> entityClass = annotatableTypeRegistry
                .resolveEntityType(entityType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));

        Annotatable annotatable = entityManager.find(entityClass, entityId);
        if (annotatable == null) {
            return ResponseEntity.notFound().build();
        }

        List<NoteDto> notes = new ArrayList<>();
        List<IssueDto> issues = new ArrayList<>();

        for (Annotation annotation : annotatable.getAnnotations()) {
            if (annotation instanceof Note note) {
                notes.add(AnnotationCommandRegistrar.toNoteDto(note));
            } else if (annotation instanceof Issue issue) {
                issues.add(AnnotationCommandRegistrar.toIssueDto(issue));
            }
        }

        notes.sort(Comparator.comparing(NoteDto::id));
        issues.sort(Comparator.comparing(IssueDto::id));

        return ResponseEntity.ok(new AnnotationsDto(notes, issues));
    }
}
