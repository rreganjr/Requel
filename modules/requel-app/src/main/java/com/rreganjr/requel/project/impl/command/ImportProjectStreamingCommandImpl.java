package com.rreganjr.requel.project.impl.command;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
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
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.utils.jaxb.imports.ActorStaxImporter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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
    private final AnnotatableTypeRegistry annotatableTypeRegistry;
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
                                             AnnotatableTypeRegistry annotatableTypeRegistry) {
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
        this.annotatableTypeRegistry = annotatableTypeRegistry;
    }

    @Override
    public void execute() {
        User createdBy = getUserRepository().get(getEditedBy());
        ProjectImpl targetProject = project instanceof ProjectImpl
                ? (ProjectImpl) project
                : new ProjectImpl(resolveProjectName(), createdBy, ((com.rreganjr.requel.user.User)createdBy).getOrganization());

        ImportUnitOfWork unitOfWork = new DefaultImportUnitOfWork();
        unitOfWork.register(User.class, createdByExternalId(createdBy), createdBy);

        ActorAssembler actorAssembler = new ActorAssembler(targetProject, getUserRepository(), createdBy);
        GoalAssembler goalAssembler = new GoalAssembler(targetProject, getUserRepository(), createdBy);
        StoryAssembler storyAssembler = new StoryAssembler(targetProject, getUserRepository(), createdBy);
        ScenarioAssembler scenarioAssembler = new ScenarioAssembler(targetProject, getUserRepository(), createdBy);
        UseCaseAssembler useCaseAssembler = new UseCaseAssembler(targetProject, getUserRepository(), createdBy);
        UserAssembler userAssembler = new UserAssembler(getUserRepository());
        StakeholderAssembler stakeholderAssembler = new StakeholderAssembler(targetProject, getUserRepository(), createdBy);
        PositionAssembler positionAssembler = new PositionAssembler(getUserRepository(), createdBy);
        AnnotationAssembler annotationAssembler = new AnnotationAssembler(getUserRepository(), createdBy, targetProject,
                new com.rreganjr.requel.annotation.imports.AnnotatableResolver(annotatableTypeRegistry));

        byte[] xmlBytes = toByteArray(getInputStream());

        // Import goals first so actors can resolve goal refs.
        List<com.rreganjr.requel.imports.project.GoalImportDraft> goalDrafts =
                goalStaxImporter.readGoals(new ByteArrayInputStream(xmlBytes));
        goalDrafts.forEach(draft -> goalAssembler.assemble(draft, unitOfWork));
        goalDrafts.forEach(draft -> goalAssembler.attachSupports(draft, unitOfWork));

        // Then import actors and link to already-registered goals.
        List<ActorImportDraft> drafts = actorStaxImporter.readActors(new ByteArrayInputStream(xmlBytes));
        drafts.forEach(draft -> {
            targetProject.getActors().add(actorAssembler.assemble(draft, unitOfWork));
        });

        // Import stories (needs goals + actors).
        List<com.rreganjr.requel.imports.project.StoryImportDraft> storyDrafts =
                storyStaxImporter.readStories(new ByteArrayInputStream(xmlBytes));
        storyDrafts.forEach(draft -> storyAssembler.assemble(draft, unitOfWork));

        // Import stakeholders/users early for createdBy resolution in remaining parts.
        com.rreganjr.requel.utils.jaxb.imports.StakeholderStaxImporter.StakeholderReadResult stakeholders =
                stakeholderStaxImporter.readStakeholders(new ByteArrayInputStream(xmlBytes));
        stakeholders.users().forEach(draft -> userAssembler.assemble(draft, unitOfWork));
        stakeholders.stakeholders().forEach(draft -> stakeholderAssembler.assemble(draft, unitOfWork));

        // Import scenarios (steps).
        List<com.rreganjr.requel.imports.project.ScenarioImportDraft> scenarioDrafts =
                scenarioStaxImporter.readScenarios(new ByteArrayInputStream(xmlBytes));
        scenarioDrafts.forEach(draft -> scenarioAssembler.assemble(draft, unitOfWork));

        // Import use cases (needs actors, goals, stories, scenarios).
        List<com.rreganjr.requel.imports.project.UseCaseImportDraft> useCaseDrafts =
                useCaseStaxImporter.readUseCases(new ByteArrayInputStream(xmlBytes));
        useCaseDrafts.forEach(draft -> useCaseAssembler.assemble(draft, unitOfWork));

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
