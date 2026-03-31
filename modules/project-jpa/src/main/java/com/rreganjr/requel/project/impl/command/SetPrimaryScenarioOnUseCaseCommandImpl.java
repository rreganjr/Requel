package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.SetPrimaryScenarioOnUseCaseCommand;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

@Controller("setPrimaryScenarioOnUseCaseCommand")
@Scope("prototype")
public class SetPrimaryScenarioOnUseCaseCommandImpl extends AbstractEditProjectCommand
		implements SetPrimaryScenarioOnUseCaseCommand {

	@Autowired
	public SetPrimaryScenarioOnUseCaseCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private UseCase useCase;
	private Scenario scenario;

	@Override public UseCase getUseCase() { return useCase; }
	@Override public void setUseCase(UseCase useCase) { this.useCase = useCase; }
	@Override public Scenario getScenario() { return scenario; }
	@Override public void setScenario(Scenario scenario) { this.scenario = scenario; }

	@Override
	public void execute() {
		UseCaseImpl useCaseImpl = (UseCaseImpl) getProjectRepository().get(getUseCase());
		ScenarioImpl scenarioImpl = (ScenarioImpl) getProjectRepository().get(getScenario());
		useCaseImpl.setScenario(scenarioImpl);
		getRepository().merge(useCaseImpl);
		setUseCase(useCaseImpl);
		setScenario(scenarioImpl);
	}
}
