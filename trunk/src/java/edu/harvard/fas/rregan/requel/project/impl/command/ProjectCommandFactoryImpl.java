/*
 * $Id: ProjectCommandFactoryImpl.java,v 1.28 2009/01/19 09:32:22 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.AbstractCommandFactory;
import edu.harvard.fas.rregan.command.CommandFactoryStrategy;
import edu.harvard.fas.rregan.requel.project.command.AddActorToActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.AddGoalToGoalContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.AddStoryToStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ConvertStepToScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyActorCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteActorCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteGlossaryTermCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteReportGeneratorCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteStakeholderCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.EditActorCommand;
import edu.harvard.fas.rregan.requel.project.command.EditAddActorToProjectPositionCommand;
import edu.harvard.fas.rregan.requel.project.command.EditAddWordToGlossaryPositionCommand;
import edu.harvard.fas.rregan.requel.project.command.EditGlossaryTermCommand;
import edu.harvard.fas.rregan.requel.project.command.EditGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.EditProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.EditReportGeneratorCommand;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.EditStakeholderCommand;
import edu.harvard.fas.rregan.requel.project.command.EditStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.EditUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.ExportProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.GenerateReportCommand;
import edu.harvard.fas.rregan.requel.project.command.ImportProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveActorFromActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.RemoveGoalFromGoalContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.RemoveStoryFromStoryContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.RemoveUnneedLexicalIssuesCommand;
import edu.harvard.fas.rregan.requel.project.command.ReplaceGlossaryTermCommand;

/**
 * An implementation of ProjectCommandFactory.
 * 
 * @author ron
 */
@Controller("projectCommandFactory")
@Scope("singleton")
public class ProjectCommandFactoryImpl extends AbstractCommandFactory implements
		ProjectCommandFactory {

	/**
	 * @param creationStrategy -
	 *            the strategy to use for creating new Command instances
	 */
	@Autowired
	public ProjectCommandFactoryImpl(CommandFactoryStrategy creationStrategy) {
		super(creationStrategy);
	}

	@Override
	public EditProjectCommand newEditProjectCommand() {
		return (EditProjectCommand) getCreationStrategy().newInstance(EditProjectCommandImpl.class);
	}

	@Override
	public ExportProjectCommand newExportProjectCommand() {
		return (ExportProjectCommand) getCreationStrategy().newInstance(
				ExportProjectCommandImpl.class);
	}

	@Override
	public ImportProjectCommand newImportProjectCommand() {
		return (ImportProjectCommand) getCreationStrategy().newInstance(
				ImportProjectCommandImpl.class);
	}

	@Override
	public EditStakeholderCommand newEditStakeholderCommand() {
		return (EditStakeholderCommand) getCreationStrategy().newInstance(
				EditStakeholderCommandImpl.class);
	}

	@Override
	public EditGoalCommand newEditGoalCommand() {
		return (EditGoalCommand) getCreationStrategy().newInstance(EditGoalCommandImpl.class);
	}

	@Override
	public EditGoalRelationCommand newEditGoalRelationCommand() {
		return (EditGoalRelationCommand) getCreationStrategy().newInstance(
				EditGoalRelationCommandImpl.class);
	}

	@Override
	public AddGoalToGoalContainerCommand newAddGoalToGoalContainerCommand() {
		return (AddGoalToGoalContainerCommand) getCreationStrategy().newInstance(
				AddGoalToGoalContainerCommandImpl.class);
	}

	@Override
	public RemoveGoalFromGoalContainerCommand newRemoveGoalFromGoalContainerCommand() {
		return (RemoveGoalFromGoalContainerCommand) getCreationStrategy().newInstance(
				RemoveGoalFromGoalContainerCommandImpl.class);
	}

	@Override
	public EditStoryCommand newEditStoryCommand() {
		return (EditStoryCommand) getCreationStrategy().newInstance(EditStoryCommandImpl.class);
	}

	@Override
	public AddStoryToStoryContainerCommand newAddStoryToStoryContainerCommand() {
		return (AddStoryToStoryContainerCommand) getCreationStrategy().newInstance(
				AddStoryToStoryContainerCommandImpl.class);
	}

	@Override
	public RemoveStoryFromStoryContainerCommand newRemoveStoryFromStoryContainerCommand() {
		return (RemoveStoryFromStoryContainerCommand) getCreationStrategy().newInstance(
				RemoveStoryFromStoryContainerCommandImpl.class);
	}

	@Override
	public EditActorCommand newEditActorCommand() {
		return (EditActorCommand) getCreationStrategy().newInstance(EditActorCommandImpl.class);
	}

	@Override
	public AddActorToActorContainerCommand newAddActorToActorContainerCommand() {
		return (AddActorToActorContainerCommand) getCreationStrategy().newInstance(
				AddActorToActorContainerCommandImpl.class);
	}

	@Override
	public RemoveActorFromActorContainerCommand newRemoveActorFromActorContainerCommand() {
		return (RemoveActorFromActorContainerCommand) getCreationStrategy().newInstance(
				RemoveActorFromActorContainerCommandImpl.class);
	}

	public EditGlossaryTermCommand newEditGlossaryTermCommand() {
		return (EditGlossaryTermCommand) getCreationStrategy().newInstance(
				EditGlossaryTermCommandImpl.class);
	}

	public EditAddWordToGlossaryPositionCommand newEditAddWordToGlossaryPositionCommand() {
		return (EditAddWordToGlossaryPositionCommand) getCreationStrategy().newInstance(
				EditAddWordToGlossaryPositionCommandImpl.class);
	}

	@Override
	public EditAddActorToProjectPositionCommand newEditAddActorToProjectPositionCommand() {
		return (EditAddActorToProjectPositionCommand) getCreationStrategy().newInstance(
				EditAddActorToProjectPositionCommandImpl.class);
	}

	@Override
	public ReplaceGlossaryTermCommand newReplaceGlossaryTermCommand() {
		return (ReplaceGlossaryTermCommand) getCreationStrategy().newInstance(
				ReplaceGlossaryTermCommandImpl.class);
	}

	@Override
	public EditUseCaseCommand newEditUseCaseCommand() {
		return (EditUseCaseCommand) getCreationStrategy().newInstance(EditUseCaseCommandImpl.class);
	}

	@Override
	public CopyUseCaseCommand newCopyUseCaseCommand() {
		return (CopyUseCaseCommand) getCreationStrategy().newInstance(CopyUseCaseCommandImpl.class);
	}

	@Override
	public CopyStoryCommand newCopyStoryCommand() {
		return (CopyStoryCommand) getCreationStrategy().newInstance(CopyStoryCommandImpl.class);
	}

	@Override
	public CopyActorCommand newCopyActorCommand() {
		return (CopyActorCommand) getCreationStrategy().newInstance(CopyActorCommandImpl.class);
	}

	@Override
	public CopyGoalCommand newCopyGoalCommand() {
		return (CopyGoalCommand) getCreationStrategy().newInstance(CopyGoalCommandImpl.class);
	}

	@Override
	public EditScenarioCommand newEditScenarioCommand() {
		return (EditScenarioCommand) getCreationStrategy().newInstance(
				EditScenarioCommandImpl.class);
	}

	@Override
	public CopyScenarioCommand newCopyScenarioCommand() {
		return (CopyScenarioCommand) getCreationStrategy().newInstance(
				CopyScenarioCommandImpl.class);
	}

	@Override
	public CopyScenarioStepCommand newCopyScenarioStepCommand() {
		return (CopyScenarioStepCommand) getCreationStrategy().newInstance(
				CopyScenarioStepCommandImpl.class);
	}

	@Override
	public EditScenarioStepCommand newEditScenarioStepCommand() {
		return (EditScenarioStepCommand) getCreationStrategy().newInstance(
				EditScenarioStepCommandImpl.class);
	}

	@Override
	public ConvertStepToScenarioCommand newConvertStepToScenarioCommand() {
		return (ConvertStepToScenarioCommand) getCreationStrategy().newInstance(
				ConvertStepToScenarioCommandImpl.class);
	}

	@Override
	public EditReportGeneratorCommand newEditReportGeneratorCommand() {
		return (EditReportGeneratorCommand) getCreationStrategy().newInstance(
				EditReportGeneratorCommandImpl.class);
	}

	@Override
	public GenerateReportCommand newGenerateReportCommand() {
		return (GenerateReportCommand) getCreationStrategy().newInstance(
				GenerateReportCommandImpl.class);
	}

	@Override
	public DeleteActorCommand newDeleteActorCommand() {
		return (DeleteActorCommand) getCreationStrategy().newInstance(DeleteActorCommandImpl.class);
	}

	@Override
	public DeleteGlossaryTermCommand newDeleteGlossaryTermCommand() {
		return (DeleteGlossaryTermCommand) getCreationStrategy().newInstance(
				DeleteGlossaryTermCommandImpl.class);
	}

	@Override
	public DeleteReportGeneratorCommand newDeleteReportGeneratorCommand() {
		return (DeleteReportGeneratorCommand) getCreationStrategy().newInstance(
				DeleteReportGeneratorCommandImpl.class);
	}

	@Override
	public DeleteScenarioCommand newDeleteScenarioCommand() {
		return (DeleteScenarioCommand) getCreationStrategy().newInstance(
				DeleteScenarioCommandImpl.class);
	}

	@Override
	public DeleteScenarioStepCommand newDeleteScenarioStepCommand() {
		return (DeleteScenarioStepCommand) getCreationStrategy().newInstance(
				DeleteScenarioStepCommandImpl.class);
	}

	@Override
	public DeleteStakeholderCommand newDeleteStakeholderCommand() {
		return (DeleteStakeholderCommand) getCreationStrategy().newInstance(
				DeleteStakeholderCommandImpl.class);
	}

	@Override
	public DeleteStoryCommand newDeleteStoryCommand() {
		return (DeleteStoryCommand) getCreationStrategy().newInstance(DeleteStoryCommandImpl.class);
	}

	@Override
	public DeleteUseCaseCommand newDeleteUseCaseCommand() {
		return (DeleteUseCaseCommand) getCreationStrategy().newInstance(
				DeleteUseCaseCommandImpl.class);
	}

	@Override
	public DeleteGoalCommand newDeleteGoalCommand() {
		return (DeleteGoalCommand) getCreationStrategy().newInstance(DeleteGoalCommandImpl.class);
	}

	@Override
	public DeleteGoalRelationCommand newDeleteGoalRelationCommand() {
		return (DeleteGoalRelationCommand) getCreationStrategy().newInstance(
				DeleteGoalRelationCommandImpl.class);
	}

	@Override
	public RemoveUnneedLexicalIssuesCommand newRemoveUnneedLexicalIssuesCommand() {
		return (RemoveUnneedLexicalIssuesCommand) getCreationStrategy().newInstance(
				RemoveUnneedLexicalIssuesCommandImpl.class);
	}
}
