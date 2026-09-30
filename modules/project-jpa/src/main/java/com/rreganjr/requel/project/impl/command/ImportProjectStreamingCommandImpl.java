/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.command;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.ActorImportDraft;
import com.rreganjr.requel.imports.project.ScenarioImportDraft;
import com.rreganjr.requel.imports.project.GlossaryTermImportDraft;
import com.rreganjr.requel.imports.project.ReportGeneratorImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectAnalysisRequestSource;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.project.imports.ActorAssembler;
import com.rreganjr.requel.project.imports.DefaultImportUnitOfWork;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.project.imports.GoalAssembler;
import com.rreganjr.requel.project.imports.StoryAssembler;
import com.rreganjr.requel.project.imports.ScenarioAssembler;
import com.rreganjr.requel.project.imports.UseCaseAssembler;
import com.rreganjr.requel.project.imports.StakeholderAssembler;
import com.rreganjr.requel.project.imports.UserAssembler;
import com.rreganjr.requel.project.imports.GlossaryTermAssembler;
import com.rreganjr.requel.project.imports.ReportGeneratorAssembler;
import com.rreganjr.requel.project.imports.ReportGeneratorAssembler;
import com.rreganjr.requel.annotation.imports.PositionAssembler;
import com.rreganjr.requel.annotation.imports.AnnotationAssembler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.imports.AnnotationLinkRegistry;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.utils.jaxb.imports.ActorStaxImporter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

/**
 * New streaming-based import command that avoids JAXB afterUnmarshal hooks.
 * Currently, processes actors via StAX + assemblers; other aggregates will follow.
 */
@Controller("importProjectCommand")
@Scope("prototype")
public class ImportProjectStreamingCommandImpl extends AbstractEditProjectCommand
        implements ImportProjectCommand, ProjectScopedCommand, ProjectAnalysisRequestSource {

    private final ActorStaxImporter actorStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.GoalStaxImporter goalStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.StoryStaxImporter storyStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.ScenarioStaxImporter scenarioStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.UseCaseStaxImporter useCaseStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.StakeholderStaxImporter stakeholderStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.PositionStaxImporter positionStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.AnnotationStaxImporter annotationStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.GlossaryTermStaxImporter glossaryTermStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.DictionaryWordStaxImporter dictionaryWordStaxImporter;
    private final com.rreganjr.nlp.dictionary.DictionaryRepository dictionaryRepository;
    private final com.rreganjr.requel.utils.jaxb.imports.ReportGeneratorStaxImporter reportGeneratorStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.TagStaxImporter tagStaxImporter;
    private final com.rreganjr.requel.tagging.spi.TaggableTypeRegistry taggableTypeRegistry;
    private final com.rreganjr.requel.tagging.TagImportHandler tagImportHandler;
    private static final String PROJECT_NS = "http://www.rreganjr.com/requel";
    private InputStream inputStream;
    private String name;
    private Project project;
    private boolean analysisEnabled = false;

    @Autowired
    public ImportProjectStreamingCommandImpl(AssistantFacade assistantManager,
                                             UserRepository userRepository,
                                             ProjectRepository projectRepository,
                                             ProjectCommandFactory projectCommandFactory,
                                             AnnotationCommandFactory annotationCommandFactory,
                                             CommandHandler commandHandler,
                                             ActorStaxImporter actorStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.GoalStaxImporter goalStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.StoryStaxImporter storyStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.ScenarioStaxImporter scenarioStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.UseCaseStaxImporter useCaseStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.StakeholderStaxImporter stakeholderStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.PositionStaxImporter positionStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.AnnotationStaxImporter annotationStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.GlossaryTermStaxImporter glossaryTermStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.DictionaryWordStaxImporter dictionaryWordStaxImporter,
                                             com.rreganjr.nlp.dictionary.DictionaryRepository dictionaryRepository,
                                             com.rreganjr.requel.utils.jaxb.imports.ReportGeneratorStaxImporter reportGeneratorStaxImporter,
                                             com.rreganjr.requel.utils.jaxb.imports.TagStaxImporter tagStaxImporter,
                                             com.rreganjr.requel.tagging.spi.TaggableTypeRegistry taggableTypeRegistry,
                                             com.rreganjr.requel.tagging.TagImportHandler tagImportHandler) {
        super(assistantManager, userRepository, projectRepository, projectCommandFactory,
                annotationCommandFactory, commandHandler);
        this.actorStaxImporter = actorStaxImporter;
        this.goalStaxImporter = goalStaxImporter;
        this.storyStaxImporter = storyStaxImporter;
        this.scenarioStaxImporter = scenarioStaxImporter;
        this.useCaseStaxImporter = useCaseStaxImporter;
        this.stakeholderStaxImporter = stakeholderStaxImporter;
        this.positionStaxImporter = positionStaxImporter;
        this.annotationStaxImporter = annotationStaxImporter;
        this.glossaryTermStaxImporter = glossaryTermStaxImporter;
        this.dictionaryWordStaxImporter = dictionaryWordStaxImporter;
        this.dictionaryRepository = dictionaryRepository;
        this.reportGeneratorStaxImporter = reportGeneratorStaxImporter;
        this.tagStaxImporter = tagStaxImporter;
        this.taggableTypeRegistry = taggableTypeRegistry;
        this.tagImportHandler = tagImportHandler;
    }

    @Override
    public void execute() {
        User createdBy = getUserRepository().get(getEditedBy());

        ImportUnitOfWork unitOfWork = new DefaultImportUnitOfWork();
        unitOfWork.register(User.class, createdByExternalId(createdBy), createdBy);
        AnnotationLinkRegistry annotationLinks = new AnnotationLinkRegistry();

        byte[] xmlBytes = toByteArray(getInputStream());
        ProjectMetadata metadata = readProjectMetadata(xmlBytes);

        ProjectImpl targetProject;
        if (project instanceof ProjectImpl existingProject) {
            targetProject = existingProject;
            if (metadata.organizationName() != null) {
                Organization resolvedOrg = resolveProjectOrganization(createdBy, metadata.organizationName());
                targetProject.setOrganization(resolvedOrg);
            }
            if (metadata.description() != null) {
                targetProject.setText(metadata.description());
            }
        } else {
            Organization organization = resolveProjectOrganization(createdBy, metadata.organizationName());
            targetProject = new ProjectImpl(resolveProjectName(), createdBy, organization);
            if (metadata.description() != null) {
                targetProject.setText(metadata.description());
            }
        }

        GlossaryTermAssembler glossaryAssembler = new GlossaryTermAssembler(targetProject, getUserRepository(), createdBy);
        ActorAssembler actorAssembler = new ActorAssembler(targetProject, getUserRepository(), createdBy);
        GoalAssembler goalAssembler = new GoalAssembler(targetProject, getUserRepository(), createdBy);
        StoryAssembler storyAssembler = new StoryAssembler(targetProject, getUserRepository(), createdBy);
        ScenarioAssembler scenarioAssembler = new ScenarioAssembler(targetProject, getUserRepository(), createdBy);
        UseCaseAssembler useCaseAssembler = new UseCaseAssembler(targetProject, getUserRepository(), createdBy);
        ReportGeneratorAssembler reportAssembler = new ReportGeneratorAssembler(targetProject, createdBy);
        UserAssembler userAssembler = new UserAssembler(getUserRepository());
        StakeholderAssembler stakeholderAssembler = new StakeholderAssembler(targetProject, getUserRepository(), createdBy);
        com.rreganjr.requel.project.imports.ProjectPositionAssembler positionAssembler =
                new com.rreganjr.requel.project.imports.ProjectPositionAssembler(getUserRepository(), createdBy);
        AnnotationAssembler annotationAssembler = new AnnotationAssembler(getUserRepository(), createdBy, targetProject,
                annotationLinks);

        List<GlossaryTermImportDraft> glossaryDrafts =
                glossaryTermStaxImporter.readTerms(new ByteArrayInputStream(xmlBytes));
        glossaryDrafts.forEach(draft -> {
            var term = glossaryAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, term, draft.getAnnotationExternalIds());
        });
        Set<String> pendingCanonicalTerms = new LinkedHashSet<>();
        for (GlossaryTermImportDraft draft : glossaryDrafts) {
            if (!StringUtils.hasText(draft.getCanonicalTermExternalId())) {
                continue;
            }
            if (draft.getExternalId() == null) {
                log.warn("Glossary term " + draft.getName() + " is missing an external id; canonical reference "
                        + draft.getCanonicalTermExternalId() + " cannot be resolved.");
                continue;
            }
            pendingCanonicalTerms.add(draft.getExternalId());
        }
        while (!pendingCanonicalTerms.isEmpty()) {
            Set<String> unresolved = new LinkedHashSet<>();
            for (GlossaryTermImportDraft draft : glossaryDrafts) {
                String draftId = draft.getExternalId();
                if (draftId == null || !pendingCanonicalTerms.contains(draftId)) {
                    continue;
                }
                boolean attached = glossaryAssembler.attachCanonicalTerm(draft, unitOfWork);
                if (!attached) {
                    unresolved.add(draftId);
                }
            }
            if (unresolved.size() == pendingCanonicalTerms.size()) {
                log.warn("Unable to resolve canonical glossary term references for ids " + unresolved);
                break;
            }
            pendingCanonicalTerms = unresolved;
        }

        // Import goals first so actors can resolve goal refs.
        List<com.rreganjr.requel.imports.project.GoalImportDraft> goalDrafts =
                goalStaxImporter.readGoals(new ByteArrayInputStream(xmlBytes));
        goalDrafts.forEach(draft -> {
            var goal = goalAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, goal, draft.getAnnotationExternalIds());
        });
        goalDrafts.forEach(draft -> goalAssembler.attachRelations(draft, unitOfWork));

        // Then import actors and link to already-registered goals.
        List<ActorImportDraft> drafts = actorStaxImporter.readActors(new ByteArrayInputStream(xmlBytes));
        drafts.forEach(draft -> {
            var actor = actorAssembler.assemble(draft, unitOfWork);
            targetProject.getActors().add(actor);
            recordAnnotationLinks(annotationLinks, actor, draft.getAnnotationExternalIds());
        });

        // Import stories (needs goals + actors).
        List<com.rreganjr.requel.imports.project.StoryImportDraft> storyDrafts =
                storyStaxImporter.readStories(new ByteArrayInputStream(xmlBytes));
        storyDrafts.forEach(draft -> {
            var story = storyAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, story, draft.getAnnotationExternalIds());
        });

        // Import stakeholders/users early for createdBy resolution in remaining parts.
        com.rreganjr.requel.utils.jaxb.imports.StakeholderStaxImporter.StakeholderReadResult stakeholders =
                stakeholderStaxImporter.readStakeholders(new ByteArrayInputStream(xmlBytes));
        stakeholders.users().forEach(draft -> userAssembler.assemble(draft, unitOfWork));
        stakeholders.stakeholders().forEach(draft -> {
            var stakeholder = stakeholderAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, stakeholder, draft.getAnnotationExternalIds());
        });

        // Import scenarios (steps).
        List<com.rreganjr.requel.imports.project.ScenarioImportDraft> scenarioDrafts =
                scenarioStaxImporter.readScenarios(new ByteArrayInputStream(xmlBytes));
        scenarioDrafts.forEach(draft -> {
            var step = scenarioAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, step, draft.getAnnotationExternalIds());
        });
        scenarioDrafts.stream()
                .filter(ScenarioImportDraft::isScenarioElement)
                .forEach(draft -> scenarioAssembler.attachSteps(draft, unitOfWork));

        // Import report generators.
        List<ReportGeneratorImportDraft> reportDrafts =
                reportGeneratorStaxImporter.readReportGenerators(new ByteArrayInputStream(xmlBytes));
        reportDrafts.forEach(draft -> {
            var report = reportAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, report, draft.getAnnotationExternalIds());
        });

        // Import use cases (needs actors, goals, stories, scenarios).
        List<com.rreganjr.requel.imports.project.UseCaseImportDraft> useCaseDrafts =
                useCaseStaxImporter.readUseCases(new ByteArrayInputStream(xmlBytes));
        useCaseDrafts.forEach(draft -> {
            var useCase = useCaseAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, useCase, draft.getAnnotationExternalIds());
        });

        // Import positions.
        var positionDrafts = positionStaxImporter.readPositions(new ByteArrayInputStream(xmlBytes));
        positionDrafts.forEach(draft -> positionAssembler.assemble(draft, unitOfWork));

        // Import annotations (notes/issues) with positions already cached.
        var annotationDrafts = annotationStaxImporter.readAnnotations(new ByteArrayInputStream(xmlBytes));
        annotationDrafts.forEach(draft -> annotationAssembler.assemble(draft, unitOfWork));

        addUserAsStakeholder(targetProject, createdBy, createdBy);
        try {
            // The assistant holds its own, narrower set rather than the creator's full matrix
            // (issue #302), and holds exactly that set: a project imported over an existing one
            // can carry an assistant row this path over-granted before #302.
            addUserAsStakeholder(targetProject, getUserRepository().findUserByUsername(User.ASSISTANT_USERNAME),
                    createdBy, getProjectRepository().findAssistantStakeholderPermissions(), true);
        } catch (NoSuchUserException e) {
            log.warn("The assistant user doesn't exist and could not be added as a stakeholder to " + targetProject.getName());
        }
        targetProject.getStakeholders().forEach(stakeholder -> {
            try {
                stakeholder.ensureProjectMembership();
            } catch (com.rreganjr.requel.user.exception.NoSuchRoleForUserException e) {
                if (stakeholder instanceof UserStakeholder) {
                    log.warn("Stakeholder user missing ProjectUserRole; skipping membership enforcement for "
                            + ((UserStakeholder) stakeholder).getUser().getUsername(), e);
                } else {
                    log.warn("Stakeholder missing ProjectUserRole; skipping membership enforcement", e);
                }
            }
        });

        addMissingBuiltinReportGenerators(targetProject, createdBy);

        setProject(getProjectRepository().persist(targetProject));

        // Import the project's own dictionary words (issue #313) AFTER the persist, for the same
        // reason the tags below wait: project_dictionary_words is keyed by the project's id, and a
        // new project has none until it is persisted. They go through the repository rather than
        // through AbstractProjectOrDomain.getDictionaryWords(), which is a read-only export view
        // with no cascade. addToDictionary recomputes the phonetic code with this installation's
        // transformator instead of trusting the one in the file, and is idempotent, so re-importing
        // a project over itself does not duplicate its dictionary.
        Long importedProjectId = targetProject.getId();
        if (importedProjectId != null) {
            for (com.rreganjr.requel.imports.project.DictionaryWordImportDraft draft
                    : dictionaryWordStaxImporter.readWords(new ByteArrayInputStream(xmlBytes))) {
                if (StringUtils.hasText(draft.getLemma())) {
                    dictionaryRepository.addToDictionary(importedProjectId, draft.getLemma());
                }
            }
        } else {
            log.warn("The imported project has no id after persist; its dictionary words were not "
                    + "imported.");
        }

        // Issue #320: the project's ignored findings, after the persist for the same reason as the
        // dictionary words: they are keyed by the new ids of the project, the entity and the issue.
        importIgnoredFindings(xmlBytes, targetProject, unitOfWork, createdBy);

        // Issue #272: the project's external sources and their entity links, after the persist:
        // the links are keyed by the entities' new ids.
        importExternalSources(xmlBytes, targetProject, unitOfWork, createdBy);

        // Import tag assignments (issue #112, Phase 5) AFTER the project and its entities are
        // persisted, so the tag @ManyToAny never references a transient entity. Entities are resolved
        // here (this command owns the unit of work) and applied through the TagImportHandler SPI, so
        // the command never references a tag JPA type — tagging stays a strict leaf.
        var tagAssignments = tagStaxImporter.readTagAssignments(new ByteArrayInputStream(xmlBytes));
        for (com.rreganjr.requel.utils.jaxb.imports.TagAssignmentImportXml ta : tagAssignments) {
            com.rreganjr.requel.tagging.Taggable taggable = resolveTaggable(ta, targetProject, unitOfWork);
            if (taggable != null) {
                tagImportHandler.assignImportedTag(taggable, ta.getToken(), createdBy);
            }
        }
    }

    /**
     * Issue #320: record each exported ignore against the imported entity, with its key rebuilt
     * for the entity's new id. A row whose entity isn't in the file is skipped with a WARN; one
     * whose issue isn't in the file keeps no issue id.
     */
    private void importIgnoredFindings(byte[] xmlBytes, Project targetProject,
            ImportUnitOfWork unitOfWork, User createdBy) {
        if (getIgnoredFindingStore() == null || targetProject.getId() == null) {
            return;
        }
        var ignored = new com.rreganjr.requel.utils.jaxb.imports.IgnoredFindingStaxImporter()
                .readIgnoredFindings(new ByteArrayInputStream(xmlBytes));
        if (ignored.isEmpty()) {
            return;
        }
        // The annotations were persisted with the project; flush so they have ids.
        getProjectRepository().flush();
        for (com.rreganjr.requel.utils.jaxb.imports.IgnoredFindingImportXml xml : ignored) {
            Class<?> entityType = com.rreganjr.requel.project.impl.IgnorableEntityTypes.BY_NAME
                    .get(xml.getEntityType());
            Object entity = (entityType == null || !StringUtils.hasText(xml.getEntityRef())) ? null
                    : unitOfWork.resolve(entityType, xml.getEntityRef()).orElse(null);
            if (!(entity instanceof com.rreganjr.requel.project.ProjectOrDomainEntity target)
                    || target.getId() == null
                    || !StringUtils.hasText(xml.getKeySuffix())) {
                log.warn("import: ignored finding " + xml.getAssistant() + ":" + xml.getEntityType()
                        + ":" + xml.getEntityRef() + ":" + xml.getKeySuffix()
                        + " has no entity in the file; skipped");
                continue;
            }
            Long annotationId = null;
            if (StringUtils.hasText(xml.getAnnotationRef())) {
                annotationId = unitOfWork
                        .resolve(com.rreganjr.requel.annotation.Annotation.class, xml.getAnnotationRef())
                        .map(com.rreganjr.requel.annotation.Annotation::getId).orElse(null);
            }
            getIgnoredFindingStore().record(new com.rreganjr.requel.project.IgnoredFindingStore.Spec(
                    targetProject.getId(), xml.getEntityType(), target.getId(), xml.getAssistant(),
                    xml.getFindingType(), xml.getProperty(), xml.getKeySuffix(), xml.getSubject(),
                    annotationId), createdBy);
        }
    }

    /**
     * Issue #272: record each exported source for the imported project, and each of its links
     * against the imported entity, keeping the recorded hashes, fingerprint and ingest times (the
     * entity's content is the same). A link whose entity isn't in the file is skipped with a
     * WARN.
     */
    private void importExternalSources(byte[] xmlBytes, Project targetProject,
            ImportUnitOfWork unitOfWork, User createdBy) {
        com.rreganjr.requel.project.ProvenanceStore store = getProvenanceStore();
        if (store == null || targetProject.getId() == null) {
            return;
        }
        var sources = new com.rreganjr.requel.utils.jaxb.imports.ExternalSourceStaxImporter()
                .readExternalSources(new ByteArrayInputStream(xmlBytes));
        if (sources.isEmpty()) {
            return;
        }
        getProjectRepository().flush();
        java.util.Map<com.rreganjr.requel.utils.jaxb.imports.ExternalSourceImportXml,
                com.rreganjr.requel.project.ExternalSource> imported = new java.util.LinkedHashMap<>();
        for (com.rreganjr.requel.utils.jaxb.imports.ExternalSourceImportXml xml : sources) {
            com.rreganjr.requel.project.SourceLocatorType locatorType = null;
            String locator = xml.getLocator();
            try {
                locatorType = StringUtils.hasText(xml.getLocatorType())
                        ? com.rreganjr.requel.project.SourceLocatorType.parse(xml.getLocatorType())
                        : null;
                com.rreganjr.requel.project.ProvenanceStore.validateLocator(locatorType, locator);
            } catch (IllegalArgumentException e) {
                log.warn("import: source " + xml.getSystem() + " " + xml.getExternalId()
                        + " has an unusable locator (" + e.getMessage() + "); imported without it");
                locatorType = null;
                locator = null;
            }
            com.rreganjr.requel.project.ProvenanceStore.SourceSpec spec =
                    new com.rreganjr.requel.project.ProvenanceStore.SourceSpec(targetProject.getId(),
                            xml.getSystem(), xml.getExternalId(), locatorType, locator,
                            xml.getTitle(), xml.getContentHash(), xml.getKind(), xml.getNote());
            try {
                com.rreganjr.requel.project.ProvenanceStore.validateSourceSpec(spec);
            } catch (IllegalArgumentException e) {
                log.warn("import: source " + xml.getSystem() + " " + xml.getExternalId()
                        + " skipped: " + e.getMessage());
                continue;
            }
            com.rreganjr.requel.project.ExternalSource source = store.recordSource(spec, createdBy)
                    .source();
            store.restoreLastIngestedAt(source.getId(), parseExportDate(xml.getLastIngestedAt()));
            imported.put(xml, source);
            for (var link : xml.getLinks()) {
                // #273: before this, every link came back DERIVED_FROM. A blank relation is a
                // file written before relations were exported, which were all DERIVED_FROM.
                com.rreganjr.requel.project.SourceLinkRelation relation;
                try {
                    relation = com.rreganjr.requel.project.SourceLinkRelation.parse(
                            link.getRelation());
                } catch (IllegalArgumentException e) {
                    log.warn("import: source link " + xml.getSystem() + " " + xml.getExternalId()
                            + " " + link.getFragment() + " has an unknown relation '"
                            + link.getRelation() + "'; skipped");
                    continue;
                }
                com.rreganjr.requel.project.ProjectOrDomainEntity target =
                        resolveLinkTarget(link, targetProject, unitOfWork);
                if (target == null || target.getId() == null) {
                    log.warn("import: source link " + xml.getSystem() + " " + xml.getExternalId()
                            + " " + link.getFragment() + " to " + link.getEntityType() + " "
                            + link.getEntityRef() + " has no entity in the file; skipped");
                    continue;
                }
                com.rreganjr.requel.project.EntitySourceLink importedLink = store.link(
                        new com.rreganjr.requel.project.ProvenanceStore.LinkSpec(source,
                                relation,
                                com.rreganjr.requel.project.impl.ProvenanceEntityTypes
                                        .nameOf(target),
                                target.getId(), link.getFragment(), link.getFragmentHash(),
                                link.getSourceHashSeen(), link.getEntityFingerprint()),
                        createdBy);
                store.restoreIngestedAt(importedLink.getId(), parseExportDate(link.getIngestedAt()));
            }
        }
        // #273: authority edges once every source exists, since an edge may name a source that
        // comes later in the file. The store's checks still apply: a cycle is skipped, not kept.
        for (var entry : imported.entrySet()) {
            for (var defersTo : entry.getKey().getDefersTo()) {
                var superior = store.findSource(targetProject.getId(), defersTo.getSystem(),
                        defersTo.getExternalId());
                if (superior.isEmpty()) {
                    log.warn("import: " + entry.getKey().getSystem() + " "
                            + entry.getKey().getExternalId() + " defers to "
                            + defersTo.getSystem() + " " + defersTo.getExternalId()
                            + ", which is not in the file; skipped");
                    continue;
                }
                try {
                    // Check before the store: a refusal thrown through the store's transactional
                    // proxy would mark the whole import for rollback.
                    AddSourceAuthorityCommandImpl.checkEdge(store, entry.getValue(), superior.get());
                    com.rreganjr.requel.project.ProvenanceStore.validateNote("note",
                            defersTo.getNote());
                    store.addAuthority(entry.getValue(), superior.get(), defersTo.getNote(),
                            createdBy);
                } catch (IllegalArgumentException e) {
                    log.warn("import: " + entry.getKey().getSystem() + " "
                            + entry.getKey().getExternalId() + " defers to "
                            + defersTo.getSystem() + " " + defersTo.getExternalId()
                            + " skipped: " + e.getMessage());
                }
            }
        }
    }

    private com.rreganjr.requel.project.ProjectOrDomainEntity resolveLinkTarget(
            com.rreganjr.requel.utils.jaxb.imports.ExternalSourceImportXml.Link link,
            Project targetProject, ImportUnitOfWork unitOfWork) {
        Class<? extends com.rreganjr.requel.project.ProjectOrDomainEntity> type =
                com.rreganjr.requel.project.impl.ProvenanceEntityTypes.BY_NAME
                        .get(link.getEntityType());
        if (type == null) {
            return null;
        }
        // Stakeholders are registered with the unit of work as Stakeholder, not NonUserStakeholder.
        Class<?> registered = com.rreganjr.requel.project.NonUserStakeholder.class.equals(type)
                ? com.rreganjr.requel.project.Stakeholder.class : type;
        Object resolved = StringUtils.hasText(link.getEntityRef())
                ? unitOfWork.resolve(registered, link.getEntityRef()).orElse(null)
                : null;
        return type.isInstance(resolved) ? type.cast(resolved) : null;
    }

    /** An export timestamp, or null when absent or unreadable (the store's "now" then stands). */
    private static java.util.Date parseExportDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            synchronized (com.rreganjr.requel.utils.DateUtils.standardDateAndTime) {
                return com.rreganjr.requel.utils.DateUtils.standardDateAndTime.parse(value);
            }
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Issue #275: add each bundled generator the imported project does not already carry (by
     * key), so an import from before #275, or from an export that dropped them, still gets the
     * current set. A name already taken by one of the project's own generators gets a
     * " (bundled)" suffix.
     */
    private void addMissingBuiltinReportGenerators(Project project, User user) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (com.rreganjr.requel.project.ReportGenerator existing : project.getReportGenerators()) {
            if (existing.getBuiltinKey() != null) {
                keys.add(existing.getBuiltinKey());
            }
            names.add(existing.getName().toLowerCase(java.util.Locale.ROOT));
        }
        for (com.rreganjr.requel.project.impl.BuiltinReportGenerators.Builtin builtin
                : com.rreganjr.requel.project.impl.BuiltinReportGenerators.ALL) {
            if (keys.contains(builtin.key())) {
                continue;
            }
            String name = names.contains(builtin.name().toLowerCase(java.util.Locale.ROOT))
                    ? builtin.name() + " (bundled)" : builtin.name();
            try {
                EditReportGeneratorCommand command = getProjectCommandFactory()
                        .newEditReportGeneratorCommand();
                command.setEditedBy(user);
                command.setProjectOrDomain(project);
                command.setName(name);
                command.setText(builtin.text());
                command.setBuiltinKey(builtin.key());
                getCommandHandler().execute(command);
            } catch (Exception e) {
                log.error("The builtin report generator " + builtin.key()
                        + " could not be added to " + project, e);
            }
        }
    }


    /**
     * Not used: an import is analyzed through the assistant SPI as a whole project
     * ({@link #getAnalysisProject()}, #268). The old {@code AssistantFacade.analyzeProject}
     * path no longer runs.
     */
    @Override
    public void invokeAnalysis() {
    }

    /**
     * @return the imported project, or {@code null} when analysis is turned off for this import.
     */
    @Override
    public Project getAnalysisProject() {
        return isAnalysisEnabled() ? getProject() : null;
    }

    @Override
    public User getAnalysisTriggeredBy() {
        return getEditedBy();
    }

    private String resolveProjectName() {
        String baseName = name != null ? name : "Imported Project";
        String candidate = baseName;
        for (int i = 1; isProjectNameTaken(candidate); i++) {
            // Strip existing trailing number+parens: "Foo (2)" → "Foo"
            String stripped = baseName.replaceAll("\\s*\\(\\d+\\)$", "");
            candidate = stripped + " (" + i + ")";
        }
        return candidate;
    }

    private boolean isProjectNameTaken(String projectName) {
        try {
            getProjectRepository().findProjectByName(projectName);
            return true;
        } catch (NoSuchProjectException e) {
            return false;
        }
    }

    private com.rreganjr.requel.tagging.Taggable resolveTaggable(
            com.rreganjr.requel.utils.jaxb.imports.TagAssignmentImportXml assignment,
            Project targetProject, ImportUnitOfWork unitOfWork) {
        if ("Project".equals(assignment.getEntityType())) {
            return (targetProject instanceof com.rreganjr.requel.tagging.Taggable projectTaggable)
                    ? projectTaggable : null;
        }
        Class<? extends com.rreganjr.requel.tagging.Taggable> entityClass =
                taggableTypeRegistry.resolveEntityType(assignment.getEntityType()).orElse(null);
        if ((entityClass == null) || (assignment.getEntityRef() == null)) {
            return null;
        }
        return unitOfWork.resolve(entityClass, assignment.getEntityRef()).orElse(null);
    }

    private String createdByExternalId(User createdBy) {
        // Fallback external id marker for unit-of-work registration
        return createdBy.getId() != null ? "USR_" + createdBy.getId() : createdBy.getUsername();
    }

    private void recordAnnotationLinks(AnnotationLinkRegistry registry, Annotatable annotatable, java.util.Set<String> annotationIds) {
        if (registry == null || annotatable == null || annotationIds == null) {
            return;
        }
        annotationIds.forEach(id -> registry.recordLink(id, annotatable));
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    private byte[] toByteArray(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read import stream", e);
        }
    }

    private ProjectMetadata readProjectMetadata(byte[] xmlBytes) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
            String organizationName = null;
            String description = null;
            boolean insideProject = false;
            int depthWithinProject = 0;
            while (reader.hasNext()) {
                int eventType = reader.getEventType();
                if (eventType == XMLStreamConstants.START_ELEMENT
                        && PROJECT_NS.equals(reader.getNamespaceURI())) {
                    if (!insideProject && "project".equals(reader.getLocalName())) {
                        insideProject = true;
                        depthWithinProject = 0;
                    } else if (insideProject) {
                        depthWithinProject++;
                        if (depthWithinProject == 1 && "organization".equals(reader.getLocalName())) {
                            organizationName = reader.getAttributeValue(null, "name");
                        } else if (depthWithinProject == 1 && "description".equals(reader.getLocalName())) {
                            description = reader.getElementText();
                            depthWithinProject--;
                            continue;
                        }
                    }
                } else if (eventType == XMLStreamConstants.END_ELEMENT && insideProject) {
                    if (depthWithinProject == 0 && "project".equals(reader.getLocalName())) {
                        break;
                    }
                    if (depthWithinProject > 0) {
                        depthWithinProject--;
                    }
                }
                reader.next();
            }
            reader.close();
            return new ProjectMetadata(organizationName, description);
        } catch (XMLStreamException e) {
            throw new ImportException("Unable to parse project metadata", e);
        }
    }

    private Organization resolveProjectOrganization(User createdBy, String organizationName) {
        if (!StringUtils.hasText(organizationName)) {
            return ((com.rreganjr.requel.user.User) createdBy).getOrganization();
        }
        try {
            return getUserRepository().findOrganizationByName(organizationName);
        } catch (NoSuchOrganizationException ignored) {
            return new OrganizationImpl(organizationName);
        }
    }

    private record ProjectMetadata(String organizationName, String description) {}

    private void addUserAsStakeholder(Project project, User user, User editedBy) {
        addUserAsStakeholder(project, user, editedBy,
                getProjectRepository().findAvailableStakeholderPermissions(), false);
    }

    /**
     * Ensure {@code user} has a stakeholder row on {@code project} holding {@code permissions}.
     *
     * <p>
     * When {@code exact} is false the row is topped up and anything extra it already holds is
     * left alone - the right behaviour for a human stakeholder, whose permissions someone
     * assigned deliberately. When true the row is made to match {@code permissions} exactly,
     * extras included. Only the assistant is imported that way (issue #302): its permissions
     * were never assigned by anyone, this loop granted them, and the point of #302 is that the
     * assistant holds the same set however the project arrived.
     */
    private void addUserAsStakeholder(Project project, User user, User editedBy,
            Set<StakeholderPermission> permissions, boolean exact) {
        if (user == null) {
            return;
        }
        // Ensure the importing user has the project role so stakeholder permissions can be granted.
        if (!user.hasRole(ProjectUserRole.class) && user instanceof com.rreganjr.requel.user.impl.UserImpl ui) {
            ui.grantRole(ProjectUserRole.class);
            getUserRepository().persist((com.rreganjr.requel.user.User) ui);
        }
        if (!user.hasRole(ProjectUserRole.class)) {
            log.warn("Stakeholder user missing ProjectUserRole; skipping membership enforcement for " + user.getUsername());
            return;
        }
        UserStakeholder creatorStakeholder = project.getStakeholders().stream()
                .filter(stakeholder -> stakeholder.matchesUser(user))
                .map(stakeholder -> (UserStakeholder) stakeholder)
                .findFirst()
                .orElseGet(() -> {
                    UserStakeholder created = new UserStakeholderImpl(project, editedBy,
                            (com.rreganjr.requel.user.User) user);
                    getProjectRepository().persist(created);
                    project.getStakeholders().add(created);
                    return created;
                });

        // Grant any missing permissions.
        for (StakeholderPermission permission : permissions) {
            if (!creatorStakeholder.getStakeholderPermissions().contains(permission)) {
                creatorStakeholder.grantStakeholderPermission(permission);
            }
        }
        if (exact) {
            for (StakeholderPermission held : new LinkedHashSet<StakeholderPermission>(
                    creatorStakeholder.getStakeholderPermissions())) {
                if (!permissions.contains(held)) {
                    creatorStakeholder.revokeStakeholderPermission(held);
                }
            }
        }
        creatorStakeholder.ensureProjectMembership();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    protected boolean isAnalysisEnabled() {
        return analysisEnabled;
    }

    public void setAnalysisEnabled(boolean analysisEnabled) {
        this.analysisEnabled = analysisEnabled;
    }
}
