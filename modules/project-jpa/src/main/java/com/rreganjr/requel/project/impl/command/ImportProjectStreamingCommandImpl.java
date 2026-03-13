/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
import com.rreganjr.requel.project.command.ImportProjectCommand;
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
import org.apache.commons.io.IOUtils;
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
public class ImportProjectStreamingCommandImpl extends AbstractEditProjectCommand implements ImportProjectCommand {

    private final ActorStaxImporter actorStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.GoalStaxImporter goalStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.StoryStaxImporter storyStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.ScenarioStaxImporter scenarioStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.UseCaseStaxImporter useCaseStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.StakeholderStaxImporter stakeholderStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.PositionStaxImporter positionStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.AnnotationStaxImporter annotationStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.GlossaryTermStaxImporter glossaryTermStaxImporter;
    private final com.rreganjr.requel.utils.jaxb.imports.ReportGeneratorStaxImporter reportGeneratorStaxImporter;
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
                                             com.rreganjr.requel.utils.jaxb.imports.ReportGeneratorStaxImporter reportGeneratorStaxImporter) {
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
        this.reportGeneratorStaxImporter = reportGeneratorStaxImporter;
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
        goalDrafts.forEach(draft -> goalAssembler.attachSupports(draft, unitOfWork));

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
            addUserAsStakeholder(targetProject, getUserRepository().findUserByUsername("assistant"), createdBy);
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

        if (targetProject.getReportGenerators().isEmpty()) {
            addBuiltinReportGenerator(targetProject, createdBy);
        }

        setProject(getProjectRepository().persist(targetProject));
    }

    private void addBuiltinReportGenerator(Project project, User user) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                    EditProjectCommandImpl.BUILTIN_REPORT_GENERATOR_PATH);
            EditReportGeneratorCommand command = getProjectCommandFactory()
                    .newEditReportGeneratorCommand();
            command.setEditedBy(user);
            command.setProjectOrDomain(project);
            command.setName("HTML Specification");
            command.setText(IOUtils.toString(inputStream));
            getCommandHandler().execute(command);
        } catch (Exception e) {
            log.error("The builtin report generator could not be added to " + project, e);
        }
    }

    @Override
    public void invokeAnalysis() {
        if (isAnalysisEnabled()) {
            getAssistantManager().analyzeProject(getProject());
        }
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
        for (StakeholderPermission permission : getProjectRepository().findAvailableStakeholderPermissions()) {
            if (!creatorStakeholder.getStakeholderPermissions().contains(permission)) {
                creatorStakeholder.grantStakeholderPermission(permission);
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
