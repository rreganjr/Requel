/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.command;

import com.rreganjr.command.CommandFactory;

/**
 * @author ron
 */
public interface ProjectCommandFactory extends CommandFactory {

	/**
	 * @return a new EditProjectCommand for creating or editing a project.
	 */
	public EditProjectCommand newEditProjectCommand();

	/**
	 * @return a new ExportProjectCommand for exporting a project to xml.
	 */
	public ExportProjectCommand newExportProjectCommand();

	/**
	 * @return a new ImportProjectCommand for importing a new or updating an
	 *         existing project from xml.
	 */
	public ImportProjectCommand newImportProjectCommand();

	/**
	 * @return a new EditStakeholderCommand for creating or editing a
	 *         stakeholder.
	 */
	public EditUserStakeholderCommand newEditUserStakeholderCommand();

	/**
	 * @return a new EditNonUserStakeholderCommand for creating or editing a
	 *         non-user stakeholder.
	 */
	public EditNonUserStakeholderCommand newEditNonUserStakeholderCommand();

	/**
	 * @return a new EditGoalCommand for creating or editing a goal.
	 */
	public EditGoalCommand newEditGoalCommand();

	/**
	 * @return a new EditGoalRelationCommand for creating or editing a
	 *         relationship between goals.
	 */
	public EditGoalRelationCommand newEditGoalRelationCommand();

	/**
	 * @return a new AddGoalToGoalContainerCommand for adding a goal to a goal
	 *         container.
	 */
	public AddGoalToGoalContainerCommand newAddGoalToGoalContainerCommand();

	/**
	 * @return a new RemoveGoalFromGoalContainerCommand for removing a goal from
	 *         a goal container.
	 */
	public RemoveGoalFromGoalContainerCommand newRemoveGoalFromGoalContainerCommand();

	/**
	 * @return a new EditStoryCommand for creating or editing a Story.
	 */
	public EditStoryCommand newEditStoryCommand();

	/**
	 * @return a new AddStoryToStoryContainerCommand for adding a Story to a
	 *         Story container.
	 */
	public AddStoryToStoryContainerCommand newAddStoryToStoryContainerCommand();

	/**
	 * @return a new RemoveStoryFromStoryContainerCommand for removing a Story
	 *         from a Story container.
	 */
	public RemoveStoryFromStoryContainerCommand newRemoveStoryFromStoryContainerCommand();

	/**
	 * @return a new EditActorCommand for creating or editing an Actor.
	 */
	public EditActorCommand newEditActorCommand();

	/**
	 * @return a new AddActorToActorContainerCommand for adding an Actor to an
	 *         Actor container.
	 */
	public AddActorToActorContainerCommand newAddActorToActorContainerCommand();

	/**
	 * @return a new RemoveActorFromActorContainerCommand for removing an Actor
	 *         from an Actor container.
	 */
	public RemoveActorFromActorContainerCommand newRemoveActorFromActorContainerCommand();

	/**
	 * @return a new EditGlossaryTermCommand for creating or editing a glossary
	 *         term.
	 */
	public EditGlossaryTermCommand newEditGlossaryTermCommand();

	/**
	 * @return a new EditAddWordToGlossaryPositionCommand for creating or
	 *         editing a position for adding a term to the project glossary.
	 */
	public EditAddWordToGlossaryPositionCommand newEditAddWordToGlossaryPositionCommand();

	/**
	 * @return a new EditAddActorToProjectPositionCommand for creating or
	 *         editing a position for adding an actor to the project.
	 */
	public EditAddActorToProjectPositionCommand newEditAddActorToProjectPositionCommand();

	/**
	 * @return a new ReplaceGlossaryTermCommand for replacing the term name in
	 *         the referring entities with the name of the canonical term.
	 */
	public ReplaceGlossaryTermCommand newReplaceGlossaryTermCommand();

	/**
	 * @return a new EditUseCaseCommand for creating or editing a usecase.
	 */
	public EditUseCaseCommand newEditUseCaseCommand();

	/**
	 * @return a new EditScenarioCommand for creating or editing a scenario.
	 */
	public EditScenarioCommand newEditScenarioCommand();

	/**
	 * @return a new EditScenarioStepCommand for creating or editing a scenario
	 *         step.
	 */
	public EditScenarioStepCommand newEditScenarioStepCommand();

	/**
	 * @return a new CopyUseCaseCommand for creating a copy of an existing
	 *         usecase.
	 */
	public CopyUseCaseCommand newCopyUseCaseCommand();

	/**
	 * @return a new CopyStoryCommand for creating a copy of an existing story.
	 */
	public CopyStoryCommand newCopyStoryCommand();

	/**
	 * @return a new CopyActorCommand for creating a copy of an existing actor.
	 */
	public CopyActorCommand newCopyActorCommand();

	/**
	 * @return a new CopyGoalCommand for creating a copy of an existing goal.
	 */
	public CopyGoalCommand newCopyGoalCommand();

	/**
	 * @return a new CopyScenarioCommand for creating a copy of an existing
	 *         scenario.
	 */
	public CopyScenarioCommand newCopyScenarioCommand();

	/**
	 * @return a new CopyScenarioCommand for creating a copy of an existing
	 *         scenario.
	 */
	public CopyScenarioStepCommand newCopyScenarioStepCommand();

	/**
	 * @return a new ConvertStepToScenarioCommand for converting a step into a
	 *         scenario.
	 */
	public ConvertStepToScenarioCommand newConvertStepToScenarioCommand();

	/**
	 * @return a new EditReportGeneratorCommand for creating or editing a report
	 *         generator.
	 */
	public EditReportGeneratorCommand newEditReportGeneratorCommand();

	/**
	 * @return a new GenerateReportCommand for generating a report for a
	 *         project.
	 */
	public GenerateReportCommand newGenerateReportCommand();

	public DeleteActorCommand newDeleteActorCommand();

	public DeleteGlossaryTermCommand newDeleteGlossaryTermCommand();

	public DeleteReportGeneratorCommand newDeleteReportGeneratorCommand();

	public DeleteScenarioCommand newDeleteScenarioCommand();

	public DeleteScenarioStepCommand newDeleteScenarioStepCommand();

	public DeleteStakeholderCommand newDeleteStakeholderCommand();

	public DeleteStoryCommand newDeleteStoryCommand();

	public DeleteUseCaseCommand newDeleteUseCaseCommand();

	public DeleteGoalCommand newDeleteGoalCommand();

	public DeleteGoalRelationCommand newDeleteGoalRelationCommand();

	public RemoveUnneedLexicalIssuesCommand newRemoveUnneedLexicalIssuesCommand();
}
