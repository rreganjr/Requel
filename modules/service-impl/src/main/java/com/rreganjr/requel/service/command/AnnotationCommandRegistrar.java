/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.command;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteArgumentCommand;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.impl.ArgumentImpl;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.annotation.impl.PositionTypes;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.AnnotationSubjectDto;
import com.rreganjr.requel.service.api.dto.ArgumentDto;
import com.rreganjr.requel.service.api.dto.DeleteArgumentInput;
import com.rreganjr.requel.service.api.dto.DeleteIssueInput;
import com.rreganjr.requel.service.api.dto.DeleteNoteInput;
import com.rreganjr.requel.service.api.dto.DeletePositionInput;
import com.rreganjr.requel.service.api.dto.EditArgumentInput;
import com.rreganjr.requel.service.api.dto.EditIssueInput;
import com.rreganjr.requel.service.api.dto.EditNoteInput;
import com.rreganjr.requel.service.api.dto.EditPositionInput;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.NoteDto;
import com.rreganjr.requel.service.api.dto.PositionDto;
import com.rreganjr.requel.service.api.dto.ResolveIssueInput;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Registers annotation domain command types with the CQRS command registry at startup.
 * <p>
 * ResolveIssueCommand is polymorphic: the concrete resolver depends on the position's type, so
 * it is registered with a builder that loads the position first and asks the factory for the
 * matching command.
 */
@Component
public class AnnotationCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(AnnotationCommandRegistrar.class);

    private final AnnotationCommandFactory factory;
    private final CommandRegistry registry;
    private final AnnotatableTypeRegistry annotatableTypeRegistry;
    private final EntityManager entityManager;
    private final ProjectRepository projectRepository;

    public AnnotationCommandRegistrar(AnnotationCommandFactory factory,
                                      CommandRegistry registry,
                                      AnnotatableTypeRegistry annotatableTypeRegistry,
                                      EntityManager entityManager,
                                      ProjectRepository projectRepository) {
        this.factory = factory;
        this.registry = registry;
        this.annotatableTypeRegistry = annotatableTypeRegistry;
        this.entityManager = entityManager;
        this.projectRepository = projectRepository;
    }

    @PostConstruct
    void registerCommands() {
        // Notes
        registry.register("EditNote", EditNoteInput.class,
                factory::newEditNoteCommand,
                (cmd, input) -> {
                    EditNoteCommand c = (EditNoteCommand) cmd;
                    EditNoteInput i = (EditNoteInput) input;
                    Annotatable annotatable = loadAnnotatable(i.entityType(), i.entityId());
                    c.setAnnotatable(annotatable);
                    c.setGroupingObject(getProject(annotatable));
                    c.setText(i.text());
                    if (i.noteId() != null) {
                        NoteImpl note = require(NoteImpl.class, i.noteId(), "Note");
                        requireSameScope(note.getGroupingObject(), getProject(annotatable), "Note",
                                i.noteId());
                        c.setNote(note);
                    }
                },
                null,
                cmd -> toNoteDto(((EditNoteCommand) cmd).getNote()));

        registry.register("DeleteNote", DeleteNoteInput.class,
                factory::newDeleteNoteCommand,
                (cmd, input) -> {
                    DeleteNoteCommand c = (DeleteNoteCommand) cmd;
                    DeleteNoteInput i = (DeleteNoteInput) input;
                    c.setNote(require(NoteImpl.class, i.noteId(), "Note"));
                });

        // Issues
        registry.register("EditIssue", EditIssueInput.class,
                factory::newEditIssueCommand,
                (cmd, input) -> {
                    EditIssueCommand c = (EditIssueCommand) cmd;
                    EditIssueInput i = (EditIssueInput) input;
                    Annotatable annotatable = loadAnnotatable(i.entityType(), i.entityId());
                    c.setAnnotatable(annotatable);
                    c.setGroupingObject(getProject(annotatable));
                    c.setText(i.text());
                    // #271: null = unchanged on update (#316's contract); false/default on create.
                    // An unknown severity maps to null here, but ValidatingCommandHandler rejects
                    // the input on its @Pattern before the command executes.
                    c.setMustBeResolved(i.mustBeResolved());
                    c.setSeverity(IssueSeverity.parse(i.severity()).orElse(null));
                    if (i.issueId() != null) {
                        IssueImpl issue = require(IssueImpl.class, i.issueId(), "Issue");
                        requireSameScope(issue.getGroupingObject(), getProject(annotatable), "Issue",
                                i.issueId());
                        c.setIssue(issue);
                    }
                },
                null,
                cmd -> toIssueDto(((EditIssueCommand) cmd).getIssue()));

        registry.register("DeleteIssue", DeleteIssueInput.class,
                factory::newDeleteIssueCommand,
                (cmd, input) -> {
                    DeleteIssueCommand c = (DeleteIssueCommand) cmd;
                    DeleteIssueInput i = (DeleteIssueInput) input;
                    c.setIssue(require(IssueImpl.class, i.issueId(), "Issue"));
                });

        // LexicalIssue — no API input DTO, driven by NLP analysis
        registry.register("EditLexicalIssue", factory::newEditLexicalIssueCommand);

        // Positions
        registry.register("EditPosition", EditPositionInput.class,
                factory::newEditPositionCommand,
                (cmd, input) -> {
                    EditPositionCommand c = (EditPositionCommand) cmd;
                    EditPositionInput i = (EditPositionInput) input;
                    IssueImpl issue = require(IssueImpl.class, i.issueId(), "Issue");
                    c.setIssue(issue);
                    c.setText(i.text());
                    if (i.positionId() != null) {
                        PositionImpl position = require(PositionImpl.class, i.positionId(), "Position");
                        requireSameScope(scopeOfPosition(position.getId()), issue.getGroupingObject(),
                                "Position", i.positionId());
                        c.setPosition(position);
                    }
                },
                null,
                cmd -> toPositionDto(((EditPositionCommand) cmd).getPosition()));

        registry.register("DeletePosition", DeletePositionInput.class,
                factory::newDeletePositionCommand,
                (cmd, input) -> {
                    DeletePositionCommand c = (DeletePositionCommand) cmd;
                    DeletePositionInput i = (DeletePositionInput) input;
                    c.setPosition(require(PositionImpl.class, i.positionId(), "Position"));
                });

        // Spelling/dictionary positions — driven by NLP analysis
        registry.register("EditChangeSpellingPosition", factory::newEditChangeSpellingPositionCommand);
        registry.register("EditAddWordToDictionaryPosition", factory::newEditAddWordToDictionaryPositionCommand);

        // Arguments
        registry.register("EditArgument", EditArgumentInput.class,
                factory::newEditArgumentCommand,
                (cmd, input) -> {
                    EditArgumentCommand c = (EditArgumentCommand) cmd;
                    EditArgumentInput i = (EditArgumentInput) input;
                    PositionImpl position = require(PositionImpl.class, i.positionId(), "Position");
                    c.setPosition(position);
                    c.setText(i.text());
                    c.setSupportLevelName(i.supportLevel());
                    if (i.argumentId() != null) {
                        ArgumentImpl argument = require(ArgumentImpl.class, i.argumentId(), "Argument");
                        requireSameScope(scopeOfPosition(argument.getPosition().getId()),
                                scopeOfPosition(position.getId()), "Argument", i.argumentId());
                        c.setArgument(argument);
                    }
                },
                null,
                cmd -> toArgumentDto(((EditArgumentCommand) cmd).getArgument()));

        registry.register("DeleteArgument", DeleteArgumentInput.class,
                factory::newDeleteArgumentCommand,
                (cmd, input) -> {
                    DeleteArgumentCommand c = (DeleteArgumentCommand) cmd;
                    DeleteArgumentInput i = (DeleteArgumentInput) input;
                    c.setArgument(require(ArgumentImpl.class, i.argumentId(), "Argument"));
                });

        // Resolve issue — polymorphic: correct command subtype depends on position type
        registry.registerWithBuilder("ResolveIssue", ResolveIssueInput.class,
                (ResolveIssueInput input) -> {
                    PositionImpl position = entityManager.find(PositionImpl.class, input.positionId());
                    if (position == null) {
                        throw new NotFoundInputException("Position not found: " + input.positionId());
                    }
                    IssueImpl issue = entityManager.find(IssueImpl.class, input.issueId());
                    if (issue == null) {
                        throw new NotFoundInputException("Issue not found: " + input.issueId());
                    }
                    // #330: the issue must be in the project named, as for the other annotation
                    // commands (#342). That the position is one of the issue's is checked by the
                    // command itself, so the UI's path gets it too.
                    requireSameScope(issue.getGroupingObject(),
                            projectRepository.findProjectByName(input.projectName()), "Issue",
                            input.issueId());
                    var cmd = factory.newResolveIssueCommand(position);
                    cmd.setPosition(position);
                    cmd.setIssue(issue);
                    return cmd;
                },
                null);

        // Cleanup
        registry.register("RemoveAnnotationFromAnnotatable", factory::newRemoveAnnotationFromAnnotatableCommand);

        log.info("Registered {} annotation command types", 13);
    }

    private Annotatable loadAnnotatable(String entityType, Long entityId) {
        Class<? extends Annotatable> entityClass = annotatableTypeRegistry
                .resolveEntityType(entityType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));
        Annotatable entity = entityManager.find(entityClass, entityId);
        if (entity == null) {
            throw new NotFoundInputException("Entity not found: " + entityType + "#" + entityId);
        }
        return entity;
    }

    /**
     * #342: looks an annotation up by id and refuses a miss. {@code entityManager.find} returns
     * null for an unknown id, and the binders used to pass that on: an edit then created a new
     * annotation and reported success, and a delete failed with a NullPointerException. Same
     * message shape as ResolveIssue's builder.
     */
    private <T> T require(Class<T> type, Long id, String label) {
        T found = id == null ? null : entityManager.find(type, id);
        if (found == null) {
            throw new NotFoundInputException(label + " not found: " + id);
        }
        return found;
    }

    /**
     * #342: refuses an edit whose child (the note, issue, position or argument named by id)
     * belongs to another project than the entity or parent it is edited under. The edit commands
     * take their permission scope from that entity or parent, so without this a stakeholder of
     * one project could rewrite another project's annotation by id. A child that reaches no
     * project or domain (an orphan position) is refused too.
     */
    private static void requireSameScope(Object childScope, Object expectedScope, String label, Long id) {
        if (childScope == null || expectedScope == null || !sameScope(childScope, expectedScope)) {
            throw new IllegalArgumentException(label + " " + id + " belongs to another project.");
        }
    }

    /**
     * Projects and domains compare by kind and id: a grouping object may be a Hibernate proxy, and
     * project and domain ids come from different tables, so equal ids alone could collide.
     */
    private static boolean sameScope(Object a, Object b) {
        if (a instanceof com.rreganjr.requel.project.ProjectOrDomain pa
                && b instanceof com.rreganjr.requel.project.ProjectOrDomain pb) {
            boolean aProject = pa instanceof com.rreganjr.requel.project.Project;
            boolean bProject = pb instanceof com.rreganjr.requel.project.Project;
            return aProject == bProject && pa.getId() != null && pa.getId().equals(pb.getId());
        }
        return a.equals(b);
    }

    /**
     * The grouping object of the issues a position answers; null when it answers none. Queried
     * rather than read from {@code position.getIssues()}: that set is lazy, and the binder can run
     * with no open persistence context (the in-process gateway), where it cannot be loaded.
     */
    private Object scopeOfPosition(Long positionId) {
        List<IssueImpl> issues = entityManager.createQuery(
                        "select i from IssueImpl i join i.positions p where p.id = :positionId"
                                + " order by i.id", IssueImpl.class)
                .setParameter("positionId", positionId)
                .setMaxResults(1)
                .getResultList();
        return issues.isEmpty() ? null : issues.get(0).getGroupingObject();
    }

    private Object getProject(Annotatable annotatable) {
        if (annotatable instanceof ProjectOrDomainEntity entity) {
            return entity.getProjectOrDomain();
        }
        // Project itself — use directly as groupingObject
        return annotatable;
    }

    // --- DTO mappers ---
    // Note/Issue/Position/Argument now expose getId() / getVersion() on the
    // annotation-domain interfaces, so no downcasts to *Impl are needed.
    // TODO(#43): replace the hardcoded `0` version with the real getVersion()
    // value once the Angular edit flow is confirmed to round-trip versions.

    public static NoteDto toNoteDto(Note note) {
        return toNoteDto(note, false);
    }

    /** @param stale whether the note may no longer apply on the entity it is read for (#270) */
    public static NoteDto toNoteDto(Note note, boolean stale) {
        if (note == null) return null;
        return new NoteDto(
                note.getId(),
                0,
                note.getText(),
                note.getCreatedBy() != null ? note.getCreatedBy().getDisplayName() : null,
                note.getSource(),
                stale,
                AnnotationSources.name(note.getSource()),
                AnnotationSources.kind(note.getSource())
        );
    }

    /**
     * #271: ordering weight of a DTO's severity name, 0 for a missing or unknown one. Lists sort
     * by it descending, so HIGH lists first.
     */
    public static int severityRank(String severity) {
        return IssueSeverity.parse(severity).map(IssueSeverity::rank).orElse(0);
    }

    /**
     * An issue as a command returns it: just written, so not stale (#270). Reads use
     * {@link #toIssueDto(Issue, boolean)}.
     */
    public static IssueDto toIssueDto(Issue issue) {
        return toIssueDto(issue, false);
    }

    /** @param stale whether the issue may no longer apply on the entity it is read for (#270) */
    public static IssueDto toIssueDto(Issue issue, boolean stale) {
        if (issue == null) return null;
        List<PositionDto> positions = issue.getPositions().stream()
                .sorted(Comparator.naturalOrder())
                .map(AnnotationCommandRegistrar::toPositionDto)
                .toList();
        return new IssueDto(
                issue.getId(),
                0,
                issue.getText(),
                issue.isMustBeResolved(),
                issue.getSeverity() != null ? issue.getSeverity().name() : IssueSeverity.MEDIUM.name(),
                issue.isResolved(),
                issue.getResolvedByUser() != null ? issue.getResolvedByUser().getDisplayName() : null,
                issue.getResolvedByPosition() != null ? issue.getResolvedByPosition().getText() : null,
                issue.getCreatedBy() != null ? issue.getCreatedBy().getDisplayName() : null,
                positions,
                issue.getSource(),
                stale,
                AnnotationSources.name(issue.getSource(), projectId(issue)),
                AnnotationSources.kind(issue.getSource(), projectId(issue)),
                subjects(issue)
        );
    }

    /**
     * #264: the project an annotation belongs to, from the first entity it is on, so a project's
     * own definitions label its issues. Null when it is on no project entity.
     */
    static Long projectId(com.rreganjr.requel.annotation.Annotation annotation) {
        if (annotation.getAnnotatables() == null) {
            return null;
        }
        for (Annotatable annotatable : annotation.getAnnotatables()) {
            if (annotatable instanceof ProjectOrDomainEntity entity
                    && entity.getProjectOrDomain() instanceof com.rreganjr.requel.project.Project project) {
                return project.getId();
            }
        }
        return null;
    }

    /** #266: the entities an annotation is on, when there is more than one. */
    static List<AnnotationSubjectDto> subjects(com.rreganjr.requel.annotation.Annotation annotation) {
        if (annotation.getAnnotatables() == null || annotation.getAnnotatables().size() < 2) {
            return List.of();
        }
        List<AnnotationSubjectDto> subjects = new java.util.ArrayList<>();
        for (Annotatable annotatable : annotation.getAnnotatables()) {
            if (annotatable instanceof ProjectOrDomainEntity entity && entity.getId() != null) {
                subjects.add(new AnnotationSubjectDto(
                        entity.getProjectOrDomainEntityInterface().getSimpleName(), entity.getId(),
                        entity.getName()));
            }
        }
        subjects.sort(Comparator.comparing(AnnotationSubjectDto::type)
                .thenComparing(AnnotationSubjectDto::id));
        return subjects.size() < 2 ? List.of() : List.copyOf(subjects);
    }

    public static PositionDto toPositionDto(Position position) {
        if (position == null) return null;
        List<ArgumentDto> arguments = position.getArguments().stream()
                .sorted(Comparator.naturalOrder())
                .map(AnnotationCommandRegistrar::toArgumentDto)
                .toList();
        // Resolved from the persisted discriminator, never from getClass() — the position here is
        // always CGLIB-wrapped (Command.get* is inside DomainObjectWrappingAdvice's pointcut), and
        // a generated class name is neither stable nor meaningful to a client (issue #253).
        String positionType = PositionTypes.typeNameOf(position);
        return new PositionDto(
                position.getId(),
                0,
                position.getText(),
                position.getCreatedBy() != null ? position.getCreatedBy().getDisplayName() : null,
                positionType,
                arguments
        );
    }

    public static ArgumentDto toArgumentDto(Argument argument) {
        if (argument == null) return null;
        return new ArgumentDto(
                argument.getId(),
                0,
                argument.getText(),
                argument.getSupportLevel() != null ? argument.getSupportLevel().name() : null,
                argument.getCreatedBy() != null ? argument.getCreatedBy().getDisplayName() : null
        );
    }
}
