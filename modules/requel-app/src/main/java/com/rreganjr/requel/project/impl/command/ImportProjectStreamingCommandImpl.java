package com.rreganjr.requel.project.impl.command;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.ActorImportDraft;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.imports.ActorAssembler;
import com.rreganjr.requel.project.imports.DefaultImportUnitOfWork;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.project.imports.GoalAssembler;
import com.rreganjr.requel.project.imports.StoryAssembler;
import com.rreganjr.requel.project.imports.ScenarioAssembler;
import com.rreganjr.requel.project.imports.UseCaseAssembler;
import com.rreganjr.requel.project.imports.StakeholderAssembler;
import com.rreganjr.requel.project.imports.UserAssembler;
import com.rreganjr.requel.annotation.imports.PositionAssembler;
import com.rreganjr.requel.annotation.imports.AnnotationAssembler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.imports.AnnotationLinkRegistry;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.utils.jaxb.imports.ActorStaxImporter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
@Controller("importProjectStreamingCommand")
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
                                             com.rreganjr.requel.utils.jaxb.imports.AnnotationStaxImporter annotationStaxImporter) {
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
        } else {
            Organization organization = resolveProjectOrganization(createdBy, metadata.organizationName());
            targetProject = new ProjectImpl(resolveProjectName(), createdBy, organization);
        }

        ActorAssembler actorAssembler = new ActorAssembler(targetProject, getUserRepository(), createdBy);
        GoalAssembler goalAssembler = new GoalAssembler(targetProject, getUserRepository(), createdBy);
        StoryAssembler storyAssembler = new StoryAssembler(targetProject, getUserRepository(), createdBy);
        ScenarioAssembler scenarioAssembler = new ScenarioAssembler(targetProject, getUserRepository(), createdBy);
        UseCaseAssembler useCaseAssembler = new UseCaseAssembler(targetProject, getUserRepository(), createdBy);
        UserAssembler userAssembler = new UserAssembler(getUserRepository());
        StakeholderAssembler stakeholderAssembler = new StakeholderAssembler(targetProject, getUserRepository(), createdBy);
        PositionAssembler positionAssembler = new PositionAssembler(getUserRepository(), createdBy);
        AnnotationAssembler annotationAssembler = new AnnotationAssembler(getUserRepository(), createdBy, targetProject,
                annotationLinks);

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
        stakeholders.stakeholders().forEach(draft -> stakeholderAssembler.assemble(draft, unitOfWork));

        // Import scenarios (steps).
        List<com.rreganjr.requel.imports.project.ScenarioImportDraft> scenarioDrafts =
                scenarioStaxImporter.readScenarios(new ByteArrayInputStream(xmlBytes));
        scenarioDrafts.forEach(draft -> {
            var scenario = scenarioAssembler.assemble(draft, unitOfWork);
            recordAnnotationLinks(annotationLinks, scenario, draft.getAnnotationExternalIds());
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

        setProject(getProjectRepository().persist(targetProject));
    }

    @Override
    public void invokeAnalysis() {
        if (isAnalysisEnabled()) {
            getAssistantManager().analyzeProject(getProject());
        }
    }

    private String resolveProjectName() {
        return name != null ? name : "Imported Project";
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
                            break;
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
            return new ProjectMetadata(organizationName);
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

    private record ProjectMetadata(String organizationName) {}

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
