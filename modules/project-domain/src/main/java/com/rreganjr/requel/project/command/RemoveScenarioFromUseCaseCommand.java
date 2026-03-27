package com.rreganjr.requel.project.command;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.UseCase;

public interface RemoveScenarioFromUseCaseCommand extends EditCommand {

	public void setUseCase(UseCase useCase);
	public UseCase getUseCase();

	public void setScenario(Scenario scenario);
	public Scenario getScenario();
}
