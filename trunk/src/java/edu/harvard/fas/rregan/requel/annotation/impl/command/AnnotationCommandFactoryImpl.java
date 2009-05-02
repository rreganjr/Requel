/*
 * $Id: AnnotationCommandFactoryImpl.java,v 1.17 2009/02/13 12:07:56 rregan Exp $
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
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.AbstractCommandFactory;
import edu.harvard.fas.rregan.command.CommandFactoryStrategy;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteArgumentCommand;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteNoteCommand;
import edu.harvard.fas.rregan.requel.annotation.command.DeletePositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditChangeSpellingPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditLexicalIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditNoteCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.annotation.command.ResolveIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.AddWordToDictionaryPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.ChangeSpellingPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;

/**
 * @author ron
 */
@Controller("annotationCommandFactory")
@Scope("singleton")
public class AnnotationCommandFactoryImpl extends AbstractCommandFactory implements
		AnnotationCommandFactory {

	// TODO: move the configuration to spring
	// Map of position types to resolver command types for those positions.
	private static final Map<Class<? extends Position>, Class<? extends ResolveIssueCommand>> positionToResolverCommand = new HashMap<Class<? extends Position>, Class<? extends ResolveIssueCommand>>();
	static {
		positionToResolverCommand.put(PositionImpl.class, ResolveIssueCommandImpl.class);
		positionToResolverCommand.put(ChangeSpellingPosition.class,
				ResolveIssueWithChangeSpellingPositionCommandImpl.class);
		positionToResolverCommand.put(AddWordToDictionaryPosition.class,
				ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class);
	}

	/**
	 * Register a command to use to resolve a specific type of position.
	 * 
	 * @param positionType
	 * @param commandType
	 */
	public static void addPositionResolverCommand(Class<? extends Position> positionType,
			Class<? extends ResolveIssueCommand> commandType) {
		positionToResolverCommand.put(positionType, commandType);
	}

	/**
	 * @param creationStrategy -
	 *            the strategy to use for creating new Command instances
	 */
	@Autowired
	public AnnotationCommandFactoryImpl(CommandFactoryStrategy creationStrategy) {
		super(creationStrategy);
	}

	@Override
	public EditNoteCommand newEditNoteCommand() {
		return (EditNoteCommand) getCreationStrategy().newInstance(EditNoteCommandImpl.class);
	}

	@Override
	public EditIssueCommand newEditIssueCommand() {
		return (EditIssueCommand) getCreationStrategy().newInstance(EditIssueCommandImpl.class);
	}

	@Override
	public EditLexicalIssueCommand newEditLexicalIssueCommand() {
		return (EditLexicalIssueCommand) getCreationStrategy().newInstance(
				EditLexicalIssueCommandImpl.class);
	}

	@Override
	public ResolveIssueCommand newResolveIssueCommand(Position position) {
		Class<?> positionType = position.getClass();
		while (positionType != null) {
			Class<? extends ResolveIssueCommand> resolverClass = positionToResolverCommand
					.get(positionType);
			if (resolverClass == null) {
				for (Class<?> intf : positionType.getInterfaces()) {
					resolverClass = positionToResolverCommand.get(intf);
					if (resolverClass != null) {
						break;
					}
				}
			}
			if (resolverClass != null) {
				return (ResolveIssueCommand) getCreationStrategy().newInstance(resolverClass);
			}
			positionType = positionType.getSuperclass();
		}
		throw new RuntimeException("unexpected position type: " + position.getClass().getName()
				+ " no resolver command");
	}

	@Override
	public EditPositionCommand newEditPositionCommand() {
		return (EditPositionCommand) getCreationStrategy().newInstance(
				EditPositionCommandImpl.class);
	}

	@Override
	public EditChangeSpellingPositionCommand newEditChangeSpellingPositionCommand() {
		return (EditChangeSpellingPositionCommand) getCreationStrategy().newInstance(
				EditChangeSpellingPositionCommandImpl.class);
	}

	@Override
	public EditAddWordToDictionaryPositionCommand newEditAddWordToDictionaryPositionCommand() {
		return (EditAddWordToDictionaryPositionCommand) getCreationStrategy().newInstance(
				EditAddWordToDictionaryPositionCommandImpl.class);
	}

	@Override
	public EditArgumentCommand newEditArgumentCommand() {
		return (EditArgumentCommand) getCreationStrategy().newInstance(
				EditArgumentCommandImpl.class);
	}

	@Override
	public RemoveAnnotationFromAnnotatableCommand newRemoveAnnotationFromAnnotatableCommand() {
		return (RemoveAnnotationFromAnnotatableCommand) getCreationStrategy().newInstance(
				RemoveAnnotationFromAnnotatableCommandImpl.class);
	}

	@Override
	public DeleteArgumentCommand newDeleteArgumentCommand() {
		return (DeleteArgumentCommand) getCreationStrategy().newInstance(
				DeleteArgumentCommandImpl.class);
	}

	@Override
	public DeleteIssueCommand newDeleteIssueCommand() {
		return (DeleteIssueCommand) getCreationStrategy().newInstance(DeleteIssueCommandImpl.class);
	}

	@Override
	public DeleteNoteCommand newDeleteNoteCommand() {
		return (DeleteNoteCommand) getCreationStrategy().newInstance(DeleteNoteCommandImpl.class);
	}

	@Override
	public DeletePositionCommand newDeletePositionCommand() {
		return (DeletePositionCommand) getCreationStrategy().newInstance(
				DeletePositionCommandImpl.class);
	}
}
