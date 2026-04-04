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
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.service.api.CommandRegistry;
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
 * Note: ResolveIssueCommand is polymorphic (requires a Position argument to determine
 * the concrete resolver). It cannot be registered as a simple Supplier and will be
 * handled via a dedicated endpoint or a specialized applicator in Phase 2.
 */
@Component
public class AnnotationCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(AnnotationCommandRegistrar.class);

    private final AnnotationCommandFactory factory;
    private final CommandRegistry registry;
    private final AnnotatableTypeRegistry annotatableTypeRegistry;
    private final EntityManager entityManager;

    public AnnotationCommandRegistrar(AnnotationCommandFactory factory,
                                      CommandRegistry registry,
                                      AnnotatableTypeRegistry annotatableTypeRegistry,
                                      EntityManager entityManager) {
        this.factory = factory;
        this.registry = registry;
        this.annotatableTypeRegistry = annotatableTypeRegistry;
        this.entityManager = entityManager;
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
                        c.setNote(entityManager.find(NoteImpl.class, i.noteId()));
                    }
                },
                null,
                cmd -> toNoteDto(((EditNoteCommand) cmd).getNote()));

        registry.register("DeleteNote", DeleteNoteInput.class,
                factory::newDeleteNoteCommand,
                (cmd, input) -> {
                    DeleteNoteCommand c = (DeleteNoteCommand) cmd;
                    DeleteNoteInput i = (DeleteNoteInput) input;
                    c.setNote(entityManager.find(NoteImpl.class, i.noteId()));
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
                    c.setMustBeResolved(Boolean.TRUE.equals(i.mustBeResolved()));
                    if (i.issueId() != null) {
                        c.setIssue(entityManager.find(IssueImpl.class, i.issueId()));
                    }
                },
                null,
                cmd -> toIssueDto(((EditIssueCommand) cmd).getIssue()));

        registry.register("DeleteIssue", DeleteIssueInput.class,
                factory::newDeleteIssueCommand,
                (cmd, input) -> {
                    DeleteIssueCommand c = (DeleteIssueCommand) cmd;
                    DeleteIssueInput i = (DeleteIssueInput) input;
                    c.setIssue(entityManager.find(IssueImpl.class, i.issueId()));
                });

        // LexicalIssue — no API input DTO, driven by NLP analysis
        registry.register("EditLexicalIssue", factory::newEditLexicalIssueCommand);

        // Positions
        registry.register("EditPosition", EditPositionInput.class,
                factory::newEditPositionCommand,
                (cmd, input) -> {
                    EditPositionCommand c = (EditPositionCommand) cmd;
                    EditPositionInput i = (EditPositionInput) input;
                    c.setIssue(entityManager.find(IssueImpl.class, i.issueId()));
                    c.setText(i.text());
                    if (i.positionId() != null) {
                        c.setPosition(entityManager.find(PositionImpl.class, i.positionId()));
                    }
                },
                null,
                cmd -> toPositionDto(((EditPositionCommand) cmd).getPosition()));

        registry.register("DeletePosition", DeletePositionInput.class,
                factory::newDeletePositionCommand,
                (cmd, input) -> {
                    DeletePositionCommand c = (DeletePositionCommand) cmd;
                    DeletePositionInput i = (DeletePositionInput) input;
                    c.setPosition(entityManager.find(PositionImpl.class, i.positionId()));
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
                    c.setPosition(entityManager.find(PositionImpl.class, i.positionId()));
                    c.setText(i.text());
                    c.setSupportLevelName(i.supportLevel());
                    if (i.argumentId() != null) {
                        c.setArgument(entityManager.find(ArgumentImpl.class, i.argumentId()));
                    }
                },
                null,
                cmd -> toArgumentDto(((EditArgumentCommand) cmd).getArgument()));

        registry.register("DeleteArgument", DeleteArgumentInput.class,
                factory::newDeleteArgumentCommand,
                (cmd, input) -> {
                    DeleteArgumentCommand c = (DeleteArgumentCommand) cmd;
                    DeleteArgumentInput i = (DeleteArgumentInput) input;
                    c.setArgument(entityManager.find(ArgumentImpl.class, i.argumentId()));
                });

        // Resolve issue — polymorphic: correct command subtype depends on position type
        registry.registerWithBuilder("ResolveIssue", ResolveIssueInput.class,
                (ResolveIssueInput input) -> {
                    PositionImpl position = entityManager.find(PositionImpl.class, input.positionId());
                    if (position == null) {
                        throw new IllegalArgumentException("Position not found: " + input.positionId());
                    }
                    IssueImpl issue = entityManager.find(IssueImpl.class, input.issueId());
                    if (issue == null) {
                        throw new IllegalArgumentException("Issue not found: " + input.issueId());
                    }
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
            throw new IllegalArgumentException("Entity not found: " + entityType + "#" + entityId);
        }
        return entity;
    }

    private Object getProject(Annotatable annotatable) {
        if (annotatable instanceof ProjectOrDomainEntity entity) {
            return entity.getProjectOrDomain();
        }
        // Project itself — use directly as groupingObject
        return annotatable;
    }

    // --- DTO mappers ---
    // Note/Issue/Position/Argument interfaces don't expose getId(); cast to concrete impls.

    public static NoteDto toNoteDto(Note note) {
        if (note == null) return null;
        NoteImpl impl = (NoteImpl) note;
        return new NoteDto(
                impl.getId(),
                0,
                impl.getText(),
                impl.getCreatedBy() != null ? impl.getCreatedBy().getDisplayName() : null
        );
    }

    public static IssueDto toIssueDto(Issue issue) {
        if (issue == null) return null;
        IssueImpl impl = (IssueImpl) issue;
        List<PositionDto> positions = impl.getPositions().stream()
                .sorted(Comparator.naturalOrder())
                .map(AnnotationCommandRegistrar::toPositionDto)
                .toList();
        return new IssueDto(
                impl.getId(),
                0,
                impl.getText(),
                impl.isMustBeResolved(),
                impl.isResolved(),
                impl.getResolvedByUser() != null ? impl.getResolvedByUser().getDisplayName() : null,
                impl.getResolvedByPosition() != null ? impl.getResolvedByPosition().getText() : null,
                impl.getCreatedBy() != null ? impl.getCreatedBy().getDisplayName() : null,
                positions
        );
    }

    public static PositionDto toPositionDto(Position position) {
        if (position == null) return null;
        PositionImpl impl = (PositionImpl) position;
        List<ArgumentDto> arguments = impl.getArguments().stream()
                .sorted(Comparator.naturalOrder())
                .map(AnnotationCommandRegistrar::toArgumentDto)
                .toList();
        // Simple class name used by the UI to label and dispatch the correct resolve variant
        String positionType = impl.getClass().getSimpleName();
        return new PositionDto(
                impl.getId(),
                0,
                impl.getText(),
                impl.getCreatedBy() != null ? impl.getCreatedBy().getDisplayName() : null,
                positionType,
                arguments
        );
    }

    public static ArgumentDto toArgumentDto(Argument argument) {
        if (argument == null) return null;
        ArgumentImpl impl = (ArgumentImpl) argument;
        return new ArgumentDto(
                impl.getId(),
                0,
                impl.getText(),
                impl.getSupportLevel() != null ? impl.getSupportLevel().name() : null,
                impl.getCreatedBy() != null ? impl.getCreatedBy().getDisplayName() : null
        );
    }
}
