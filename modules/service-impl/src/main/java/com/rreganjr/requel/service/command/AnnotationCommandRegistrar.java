package com.rreganjr.requel.service.command;

import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.service.api.CommandRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public AnnotationCommandRegistrar(AnnotationCommandFactory factory, CommandRegistry registry) {
        this.factory = factory;
        this.registry = registry;
    }

    @PostConstruct
    void registerCommands() {
        // Notes
        registry.register("EditNote", factory::newEditNoteCommand);
        registry.register("DeleteNote", factory::newDeleteNoteCommand);

        // Issues
        registry.register("EditIssue", factory::newEditIssueCommand);
        registry.register("EditLexicalIssue", factory::newEditLexicalIssueCommand);
        registry.register("DeleteIssue", factory::newDeleteIssueCommand);
        // ResolveIssue is polymorphic — requires Position argument, handled separately

        // Positions
        registry.register("EditPosition", factory::newEditPositionCommand);
        registry.register("EditChangeSpellingPosition", factory::newEditChangeSpellingPositionCommand);
        registry.register("EditAddWordToDictionaryPosition", factory::newEditAddWordToDictionaryPositionCommand);
        registry.register("DeletePosition", factory::newDeletePositionCommand);

        // Arguments
        registry.register("EditArgument", factory::newEditArgumentCommand);
        registry.register("DeleteArgument", factory::newDeleteArgumentCommand);

        // Cleanup
        registry.register("RemoveAnnotationFromAnnotatable", factory::newRemoveAnnotationFromAnnotatableCommand);

        log.info("Registered {} annotation command types", 12);
    }
}
